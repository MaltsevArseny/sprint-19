package ru.yandex.practicum.collector.util;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;

import java.io.ByteArrayOutputStream;

public class AvroSerializer {

    public static byte[] serialize(SpecificRecordBase event) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // ✅ типобезопасно
            SpecificDatumWriter<SpecificRecordBase> writer =
                    new SpecificDatumWriter<>(event.getSchema());

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
