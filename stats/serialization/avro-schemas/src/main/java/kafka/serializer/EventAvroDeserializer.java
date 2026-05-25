package kafka.serializer;

import ru.practicum.ewm.stats.avro.EventAvro;

public class EventAvroDeserializer extends BaseAvroDeserializer<EventAvro> {

    public EventAvroDeserializer() {
        super(EventAvro.getClassSchema());
    }
}
