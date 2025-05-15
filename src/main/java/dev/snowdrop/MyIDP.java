package dev.snowdrop;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;

import java.util.*;
import java.util.concurrent.Callable;

import dev.snowdrop.command.DeleteContainer;
import dev.snowdrop.command.StartContainer;
import dev.snowdrop.command.StopContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@CommandLine.Command(name = "idp",
    subcommands = {
        MyIDP.CreateCommand.class,
        StopContainer.class,
        StartContainer.class,
        DeleteContainer.class
    },
    description = "A simple Docker CLI using Java and Picocli")
public class MyIDP extends Container implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyIDP.class);

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
    boolean usageHelpRequested;

    public static void main(String[] args) {
        initDockerClient();
        int exitCode = new CommandLine(new MyIDP()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // If no subcommand is specified, show usage
        CommandLine.usage(this, System.out);
        return 0;
    }

    // Example Subcommand: Create
    @Command(name = "create", description = "Create a new container")
    static class CreateCommand implements Callable<Integer> {

        @ParentCommand
        MyIDP parent;

        @Parameters(index = "0", description = "The image name to use for the container")
        String containerName;

        @Option(names = {"-i", "--image"}, description = "The name of the image")
        String imageName;

        @Option(names = {"-d", "--detach"}, description = "Run the container in detached mode")
        boolean detach = false;

        /*
        @Option(names = {"--cmd"}, description = "Command to execute in the container", arity = "1..*")
        List<String> command;
        */

        @Option(names = {"-p", "--publish"}, description = "Publish a container's port(s) to the host", arity = "1..*")
        List<String> ports;  // e.g., "8080:80", "443:443/tcp"

        @Option(names = {"-v", "--volume"}, description = "Bind a volume to the container", arity = "1..*")
        List<String> volumes; // e.g., "/host:/container:ro", "named-volume:/container:rw"

        @Option(names = {"-e", "--env"}, description = "Set environment variables", arity = "1..*")
        List<String> environment; // e.g., "VAR1=value1", "VAR2=value2"

        @Override
        public Integer call() {
            try {
                // Check if image name has been defined
                if (imageName == null) {
                    imageName = "mongo:3.6";
                }

                // Pull image
                ImageUtils.pullImage(dockerClient, imageName);

                // Build the command
                CreateContainerCmd ccc = Container.dockerClient.createContainerCmd(imageName);

                if (containerName != null) {
                    ccc.withName(containerName);
                }

                ccc.withCmd("--bind_ip_all");
                ccc.withEnv("MONGO_LATEST_VERSION=3.6");
                ccc.getHostConfig().withPortBindings(PortBinding.parse("9999:27017"));
                ccc.getHostConfig().withBinds(Bind.parse("/Users/cmoullia/code/ch007m/my-java-idp/my-java-idp/db:/data/db"));

                /*
                if (ports != null && !ports.isEmpty()) {
                    List<ExposedPort> exposedPorts = new ArrayList<>();
                    List<PortBinding> portBindings = new ArrayList<>();
                    for (String portMapping : ports) {
                        String[] parts = portMapping.split(":");
                        if (parts.length > 0) {
                            String hostPort = parts[0];
                            String containerPort = parts[1];
                            String protocol = "tcp"; // Default protocol

                            if (containerPort.contains("/")) {
                                String[] portParts = containerPort.split("/");
                                containerPort = portParts[0];
                                protocol = portParts[1];
                            }

                            ExposedPort exposedPort = new ExposedPort(Integer.parseInt(containerPort), Protocol.valueOf(protocol.toUpperCase()));
                            exposedPorts.add(exposedPort);

                            if(parts.length > 1) {
                                PortBinding portBinding = new PortBinding(HostConfig.newPortBindings(hostPort),exposedPort);
                                portBindings.add(portBinding);
                            } else {
                                PortBinding portBinding = new PortBinding(HostConfig.newPortBindings(hostPort),exposedPort);
                                portBindings.add(portBinding);
                            }
                        }
                    }
                    ccc.withExposedPorts(exposedPorts.toArray(new ExposedPort[0]));
                    ccc.withPortBindings(portBindings.toArray(new PortBinding[0]));
                }
                */


                if (volumes != null && !volumes.isEmpty()) {
                    List<Bind> binds = new ArrayList<>();
                    for (String volumeMapping : volumes) {
                        String[] parts = volumeMapping.split(":");
                        if (parts.length > 1) {
                            String hostPath = parts[0];
                            String containerPath = parts[1];
                            String mode = "rw"; // Default mode
                            if (parts.length > 2) {
                                mode = parts[2];
                            }
                            binds.add(new Bind(hostPath, new Volume(containerPath), AccessMode.valueOf(mode.toUpperCase())));
                        }
                    }
                    ccc.withBinds(binds.toArray(new Bind[0]));
                }

                if (environment != null && !environment.isEmpty()) {
                    ccc.withEnv(environment.toArray(new String[0]));
                }

                CreateContainerResponse containerResponse = ccc.exec();
                String containerId = containerResponse.getId();
                LOGGER.info("Container created with ID: {}", containerId);

                closeDockerClient();
                return 0;

            } catch (Exception e) {
                LOGGER.error("Error creating container: {}", e.getMessage(), e);
                return 1;
            }
        }
    }
}