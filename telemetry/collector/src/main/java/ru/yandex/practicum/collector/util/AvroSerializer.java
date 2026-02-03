package ru.yandex.practicum.collector.util;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.io.ByteArrayOutputStream;

public class AvroSerializer {

    public static byte[] serialize(SensorEventAvro event) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            SpecificDatumWriter<SensorEventAvro> writer =
                    new SpecificDatumWriter<>(SensorEventAvro.class);

            BinaryEncoder encoder =
                    EncoderFactory.get().binaryEncoder(out, null);

            writer.write(event, encoder);
            encoder.flush();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Avro serialization failed", e);
        }
    }
}
