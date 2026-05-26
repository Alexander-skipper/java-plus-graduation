package ru.practicum.stats.client;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Component
@Slf4j
public class CollectorGrpcClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void sendView(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_VIEW, Instant.now());
    }

    public void sendRegister(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_REGISTER, Instant.now());
    }

    public void sendLike(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_LIKE, Instant.now());
    }

    private void sendAction(Long userId, Long eventId, ActionTypeProto actionType, Instant timestamp) {
        log.info("Sending action via gRPC to Collector: userId={}, eventId={}, actionType={}, timestamp={}\",",
                userId, eventId, actionType, timestamp);

        try {
            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(timestamp.getEpochSecond())
                            .setNanos(timestamp.getNano()))
                    .build();

            Empty response = collectorStub.collectUserAction(request);
            log.debug("Successfully sent action to Collector: {}", response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for sendAction: {}", e.getMessage(), e);
        }
    }
}
