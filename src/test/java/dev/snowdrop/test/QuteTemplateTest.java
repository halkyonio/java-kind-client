package dev.snowdrop.test;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuteTemplateTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
        .withApplicationRoot((jar) -> jar
            .addClasses(Movie.class)
            .addAsResource(new StringAsset("{@dev.snowdrop.test.Movie movie}"
                + "{movie.mainCharacters.size}: {#for character in movie.mainCharacters}"
                + "{character}"
                + "{#if character_hasNext}, {/}"
                + "{/}"), "templates/movie.html"));

    @Inject
    Template movie;

    @Test
    public void testMovie() {
        Assertions.assertEquals("2: Michael Caine, John Cleese",
            movie.data("movie", new Movie("Michael Caine", "John Cleese")).render());
    }

    @Test
    public void testKubeAdmConfig() throws IOException {
        InputStream is = Test.class.getResourceAsStream("/kubeadm-from-1.24.yaml");
        String kubeAdmin = new String(is.readAllBytes());

        Engine engine = Engine.builder().addDefaults().build();
        Template kubeAdminTmpl = engine.parse(kubeAdmin);

        KubeAdmConfig kubeAdmConfig = new KubeAdmConfig();
        kubeAdmConfig.setNodeName("my-kind");

        var result = kubeAdminTmpl.data("config",kubeAdmConfig).render();
        assertTrue(result.contains("clusterName: \"my-kind\""));
    }

}
