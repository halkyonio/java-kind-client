package dev.snowdrop.test;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class QuteTemplateTest {
    @Test
    public void test() throws IOException {
        InputStream is = Test.class.getResourceAsStream("/kubeadm-from-1.24.yaml");
        String kubeAdmin = new String(is.readAllBytes());
        Engine engine = Engine.builder().addDefaults().build();
        Template kubeAdminTmpl = engine.parse(kubeAdmin);

        var result = kubeAdminTmpl.data("nodeName","my-kind").render();
        assertEquals("my-kind",result);
    }

}
