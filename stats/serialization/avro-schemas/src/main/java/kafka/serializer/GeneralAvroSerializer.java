package kafka.serializer;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class GeneralAvroSerializer implements Serializer<SpecificRecordBase> {
    private static final EncoderFactory ENCODER_FACTORY = EncoderFactory.get();

    @Override
    public byte[] serialize(String topic, SpecificRecordBase data) {
        if (data == null) {
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            SpecificDatumWriter<SpecificRecordBase> writer =
                    new SpecificDatumWriter<>(data.getSchema());
            BinaryEncoder encoder = ENCODER_FACTORY.binaryEncoder(baos, null);

            writer.write(data, encoder);
            encoder.flush();

            return baos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize Avro message for topic " + topic, e);
        }
    }
}
