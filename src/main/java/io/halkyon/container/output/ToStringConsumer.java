package io.halkyon.container.output;

import com.google.common.base.Charsets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class ToStringConsumer extends BaseConsumer<ToStringConsumer> {

    private final ByteArrayOutputStream stringBuffer = new ByteArrayOutputStream();

    @Override
    public void accept(OutputFrame outputFrame) {
        try {
            final byte[] bytes = outputFrame.getBytes();
            if (bytes != null) {
                stringBuffer.write(bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toUtf8String() {
        byte[] bytes = stringBuffer.toByteArray();
        return new String(bytes, Charsets.UTF_8);
    }

    public String toString(Charset charset) {
        byte[] bytes = stringBuffer.toByteArray();
        return new String(bytes, charset);
    }
}
