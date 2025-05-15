package dev.snowdrop;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ContainerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerUtils.class);
    private static DockerClient dockerClient;

    protected static DockerClient ConfigureDockerClient() {
        if (dockerClient == null) {
            try {
                var config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .build();
                var httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .build();
                dockerClient = DockerClientImpl.getInstance(config, httpClient);
                return dockerClient;
            } catch (Exception e) {
                LOGGER.error("Failed to create Docker client: {}", e.getMessage(), e);
                return null;
            }
        } else {
            return dockerClient;
        }
    }

    public static String fetchContainerId(String containerName) throws IOException {

        List<Container> containers = dockerClient.listContainersCmd()
            .withShowAll(true)
            .exec();

        for (Container container : containers) {
            for (String name : container.getNames()) {
                // container names are prefixed with '/'
                if (name.equals("/" + containerName)) {
                    LOGGER.info("Container ID: " + container.getId());
                    return container.getId();
                }
            }
        }
        return containerName;
    }

    public static CreateContainerCmd convertInspectJsonToCreateCommand(DockerClient dockerClient, String jsonFilePath) throws IOException {
        String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
        Gson gson = new Gson();
        JsonObject jsonObject;
        try {
            jsonObject = gson.fromJson(jsonContent, JsonObject.class);
        } catch (JsonParseException e) {
            throw new IOException("Error parsing JSON: " + e.getMessage(), e);
        }

        // 1. Basic Image and Name
        String imageName = jsonObject.get("Config").getAsJsonObject().get("Image").getAsString();
        CreateContainerCmd execContainerCmd = dockerClient.createContainerCmd(imageName);

        if (jsonObject.has("Name")) {
            execContainerCmd.withName(jsonObject.get("Name").getAsString().substring(1)); //remove the first "/"
        }

        // 2. Command
        if (jsonObject.get("Config").getAsJsonObject().has("Cmd")) {
            List<String> cmdList = new ArrayList<>();
            for (JsonElement cmdElement : jsonObject.get("Config").getAsJsonObject().get("Cmd").getAsJsonArray()) {
                cmdList.add(cmdElement.getAsString());
            }
            execContainerCmd.withCmd(cmdList.toArray(new String[0]));
        }

        // 3. Environment Variables
        if (jsonObject.get("Config").getAsJsonObject().has("Env")) {
            List<String> envList = new ArrayList<>();
            for (JsonElement envElement : jsonObject.get("Config").getAsJsonObject().get("Env").getAsJsonArray()) {
                envList.add(envElement.getAsString());
            }
            execContainerCmd.withEnv(envList.toArray(new String[0]));
        }

        // 4. Ports
        if (jsonObject.get("HostConfig").getAsJsonObject().has("PortBindings")) {
            JsonObject portBindingsJson = jsonObject.get("HostConfig").getAsJsonObject().getAsJsonObject("PortBindings");
            List<ExposedPort> exposedPorts = new ArrayList<>();
            List<PortBinding> portBindings = new ArrayList<>();

/*            for (Map.Entry<String, JsonElement> entry : portBindingsJson.entrySet()) {
                String portString = entry.getKey(); // e.g., "80/tcp", "443/udp"
                String[] portParts = portString.split("/");
                int portNumber = Integer.parseInt(portParts[0]);
                ApplicationProtocolConfig.Protocol protocol = ApplicationProtocolConfig.Protocol.valueOf(portParts[1].toUpperCase());
                ExposedPort exposedPort = new ExposedPort(portNumber, protocol);
                exposedPorts.add(exposedPort);

                JsonArray bindings = entry.getValue().getAsJsonArray();
                if (bindings != null && bindings.size() > 0) {
                    JsonObject binding = bindings.get(0).getAsJsonObject();  // Assuming only one binding per port for simplicity
                    String hostPort = binding.get("HostPort").getAsString();
                    PortBinding portBinding = new PortBinding(, exposedPort);
                    portBindings.add(portBinding);
                } else {
                    PortBinding portBinding = new PortBinding(HostConfig.newPortBindings(), exposedPort);
                    portBindings.add(portBinding);
                }
            }*/
            execContainerCmd.withExposedPorts(exposedPorts.toArray(new ExposedPort[0]));
            execContainerCmd.withPortBindings(portBindings.toArray(new PortBinding[0]));
        }

        // 5. Volumes
        if (jsonObject.get("HostConfig").getAsJsonObject().has("Binds")) {
            List<Bind> binds = new ArrayList<>();
            for (JsonElement bindElement : jsonObject.get("HostConfig").getAsJsonObject().getAsJsonArray("Binds")) {
                String bindString = bindElement.getAsString(); // e.g., "/host:/container:ro"
                String[] parts = bindString.split(":");
                if (parts.length > 1) {
                    String hostPath = parts[0];
                    String containerPath = parts[1];
                    String mode = "rw";
                    if (parts.length > 2) {
                        mode = parts[2];
                    }
                    binds.add(new Bind(hostPath, new Volume(containerPath), AccessMode.valueOf(mode.toUpperCase())));
                }
            }
            execContainerCmd.withBinds(binds.toArray(new Bind[0]));
        }
        // 6. Network (Basic)
        if (jsonObject.get("HostConfig").getAsJsonObject().has("NetworkMode")) {
            String networkMode = jsonObject.get("HostConfig").getAsJsonObject().get("NetworkMode").getAsString();
            execContainerCmd.withNetworkMode(networkMode);
            // More complex network configuration might require additional API calls
            // and handling of ConnectToNetworkCmd.
        }
        return execContainerCmd;
    }
}
