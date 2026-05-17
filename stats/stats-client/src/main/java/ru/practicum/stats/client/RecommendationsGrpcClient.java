package ru.practicum.stats.client;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@Slf4j
public class RecommendationsGrpcClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public List<RecommendedEvent> getRecommendationsForUser(Long userId, int maxResults) {
        log.info("Getting recommendations for user {} via gRPC, maxResults={}", userId, maxResults);

        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getRecommendationsForUser(request);
            List<RecommendedEventProto> protos = streamFromIterator(iterator).collect(Collectors.toList());

            List<RecommendedEvent> result = protos.stream()
                    .map(p -> new RecommendedEvent(p.getEventId(), p.getScore()))
                    .collect(Collectors.toList());

            log.info("Received {} recommendations for user {}", result.size(), userId);
            return result;
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for getRecommendationsForUser: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<RecommendedEvent> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        log.info("Getting similar events for eventId={}, userId={}, maxResults={}", eventId, userId, maxResults);

        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId != null ? userId : 0)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getSimilarEvents(request);
            List<RecommendedEventProto> protos = streamFromIterator(iterator).collect(Collectors.toList());

            List<RecommendedEvent> result = protos.stream()
                    .map(p -> new RecommendedEvent(p.getEventId(), p.getScore()))
                    .collect(Collectors.toList());

            log.info("Found {} similar events for eventId={}", result.size(), eventId);
            return result;
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for getSimilarEvents: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<RecommendedEvent> getInteractionsCount(List<Long> eventIds) {
        log.info("Getting interactions count for {} events via gRPC", eventIds.size());

        try {
            InteractionsCountRequestProto.Builder builder = InteractionsCountRequestProto.newBuilder();
            for (Long eventId : eventIds) {
                builder.addEventId(eventId);
            }

            Iterator<RecommendedEventProto> iterator = analyzerStub.getInteractionsCount(builder.build());
            List<RecommendedEventProto> protos = streamFromIterator(iterator).collect(Collectors.toList());

            List<RecommendedEvent> result = protos.stream()
                    .map(p -> new RecommendedEvent(p.getEventId(), p.getScore()))
                    .collect(Collectors.toList());

            log.info("Received counts for {} events", result.size());
            return result;
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for getInteractionsCount: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private <T> java.util.stream.Stream<T> streamFromIterator(Iterator<T> iterator) {
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    public record RecommendedEvent(Long eventId, Double score) {}
}
