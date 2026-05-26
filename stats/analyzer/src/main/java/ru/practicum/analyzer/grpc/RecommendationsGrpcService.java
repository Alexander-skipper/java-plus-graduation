package ru.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

import java.util.List;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class RecommendationsGrpcService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("gRPC getRecommendationsForUser: userId={}, maxResults={}",
                request.getUserId(), request.getMaxResults());

        List<RecommendationService.RecommendedEvent> recommendations =
                recommendationService.getRecommendationsForUser(request.getUserId(), request.getMaxResults());

        for (RecommendationService.RecommendedEvent rec : recommendations) {
            RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                    .setEventId(rec.eventId())
                    .setScore(rec.score())
                    .build();
            responseObserver.onNext(proto);
        }

        responseObserver.onCompleted();
        log.info("Returned {} recommendations for user {}", recommendations.size(), request.getUserId());
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("gRPC getSimilarEvents: eventId={}, userId={}, maxResults={}",
                request.getEventId(), request.getUserId(), request.getMaxResults());

        List<RecommendationService.RecommendedEvent> similarEvents =
                recommendationService.getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults());

        for (RecommendationService.RecommendedEvent rec : similarEvents) {
            RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                    .setEventId(rec.eventId())
                    .setScore(rec.score())
                    .build();
            responseObserver.onNext(proto);
        }

        responseObserver.onCompleted();
        log.info("Returned {} similar events for eventId={}", similarEvents.size(), request.getEventId());
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("gRPC getInteractionsCount: {} events requested", request.getEventIdCount());

        List<RecommendationService.RecommendedEvent> counts =
                recommendationService.getInteractionsCount(request.getEventIdList());

        for (RecommendationService.RecommendedEvent rec : counts) {
            RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                    .setEventId(rec.eventId())
                    .setScore(rec.score())
                    .build();
            responseObserver.onNext(proto);
        }

        responseObserver.onCompleted();
        log.info("Returned counts for {} events", counts.size());
    }
}
