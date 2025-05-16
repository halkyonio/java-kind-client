package dev.snowdrop.kind;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static dev.snowdrop.Container.execInContainer;
import static dev.snowdrop.KindContainer.getInternalIpAddress;
import static dev.snowdrop.kind.KubernetesConfig.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

public class KindKubernetesConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(KindKubernetesConfiguration.class);

    private String kubernetesVersion;

    public String getKubernetesVersion() {
        return kubernetesVersion;
    }

    public void setKubernetesVersion(String kubernetesVersion) {
        this.kubernetesVersion = kubernetesVersion;
    }

    public Map<String, String> prepareTemplateParams(InspectContainerResponse containerInfo) throws IOException, InterruptedException {
        final String containerInternalIpAddress = getInternalIpAddress(containerInfo);
        LOGGER.info("Container internal IP address: {}", containerInternalIpAddress);
        final Set<String> subjectAlternativeNames = new HashSet<>(asList(
            containerInternalIpAddress,
            "127.0.0.1",
            "localhost"
            //getContainerIpAddress()
        ));
        LOGGER.debug("SANs for Kube-API server certificate: {}", subjectAlternativeNames);
        final Map<String, String> params = new HashMap<String, String>() {{
            put(".NodeIp", containerInternalIpAddress);
            put(".PodSubnet", POD_SUBNET);
            put(".ServiceSubnet", SERVICE_SUBNET);
            put(".CertSANs", subjectAlternativeNames.stream().map(san -> "\"" + san + "\"").collect(joining(",")));
            put(".KubernetesVersion", kubernetesVersion);
            put(".MinNodePort", String.valueOf(MIN_NODE_PORT));
            put(".MaxNodePort", String.valueOf(MAX_NODE_PORT));
        }};
        execInContainer(new String[]{"mkdir", "-p", CONTAINER_WORKDIR});
        return params;
    }

}
