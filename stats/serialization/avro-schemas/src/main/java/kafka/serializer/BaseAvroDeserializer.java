package kafka.serializer;

import lombok.RequiredArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseAvroDeserializer.class);
    private static final DecoderFactory DECODER_FACTORY = DecoderFactory.get();
    private final DatumReader<T> datumReader;

    public BaseAvroDeserializer(Schema schema) {
        this.datumReader = new SpecificDatumReader<>(schema);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            log.warn("Null data get for deserializer");
            return null;
        }
        try {
            BinaryDecoder decoder = DECODER_FACTORY.binaryDecoder(data, null);
            return datumReader.read(null,decoder);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize Avro message for topic " + topic, e);
        }
    }
}
