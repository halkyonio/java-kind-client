package io.halkyon.kind;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.halkyon.config.model.qute.KubeAdmConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.halkyon.Container.execInContainer;
import static io.halkyon.container.ContainerUtils.getInternalIpAddress;
import static io.halkyon.kind.KubernetesConfig.*;

public class KindKubernetesConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(KindKubernetesConfiguration.class);

    private String kubernetesVersion;

    public String getKubernetesVersion() {
        return kubernetesVersion;
    }

    public void setKubernetesVersion(String kubernetesVersion) {
        this.kubernetesVersion = kubernetesVersion;
    }

    public KubeAdmConfig prepareTemplateParams(InspectContainerResponse containerInfo) throws IOException, InterruptedException {
        KubeAdmConfig cfg = new KubeAdmConfig();
        cfg.setNodeIp(getInternalIpAddress(containerInfo));
        // TODO: Check why tDocker Java client adds a "/" before the nodeName !
        cfg.setNodeName(containerInfo.getName().replaceAll("/", ""));
        cfg.setBindPort("6443");
        LOGGER.info("Container internal IP address: {}", cfg.getNodeIp());
        cfg.setPodSubnet(POD_SUBNET);
        cfg.setServiceSubnet(SERVICE_SUBNET);
        cfg.setKubernetesVersion(kubernetesVersion);
        cfg.setMinNodePort(String.valueOf(MIN_NODE_PORT));
        cfg.setMaxNodePort(String.valueOf(MAX_NODE_PORT));
        cfg.setNodeLabels(NODE_LABELS);
        LOGGER.info("Creating within the kind container the CONTAINER_WORKDIR: {}",CONTAINER_WORKDIR);
        execInContainer("mkdir", "-p", CONTAINER_WORKDIR);
        return cfg;
    }

}
