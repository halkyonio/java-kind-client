package dev.snowdrop;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import dev.snowdrop.container.ContainerUtils;
import dev.snowdrop.container.ExecConfig;
import dev.snowdrop.container.ExecResult;
import dev.snowdrop.container.output.FrameConsumerResultCallback;
import dev.snowdrop.container.output.OutputFrame;
import dev.snowdrop.container.output.ToStringConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Container {

    private static final Logger LOGGER = LoggerFactory.getLogger(Container.class);
    protected static DockerClient dockerClient;
    protected static InspectContainerResponse containerInfo;

    protected static void initDockerClient() {
        try {
            dockerClient = ContainerUtils.ConfigureDockerClient();
        } catch (Exception e) {
            LOGGER.error("Error creating Docker client: {}", e.getMessage(), e);
            System.exit(-1);
        }
    }

    protected void setContainerInfo(InspectContainerResponse icr) {
        containerInfo = icr;
    }

    protected static void closeDockerClient() {
        try {
            if (dockerClient != null) {
                dockerClient.close();
            }
        } catch (Exception e) {
            LOGGER.error("Error closing Docker client: {}", e.getMessage(), e);
        }
    }


    public static ExecResult execInContainer(String... command) throws UnsupportedOperationException, IOException, InterruptedException {
        return execInContainer(StandardCharsets.UTF_8, command);
    }

    public static ExecResult execInContainer(Charset outputCharset, String... command) throws UnsupportedOperationException, IOException, InterruptedException {
        ExecConfig ec = new ExecConfig();
        ec.setCommand(command);
        return execInContainerInternal(dockerClient, containerInfo, outputCharset, ec);
    }

    /**
     * Run a command inside a running container as a given user, as using "podman exec".
     * <p>
     * @param dockerClient the {@link DockerClient}
     * @param containerInfo the container info
     * @param outputCharset the character set used to interpret the output.
     * @param execConfig the exec configuration
     * @return the result of execution
     * @throws IOException if there's an issue communicating with Docker
     * @throws InterruptedException if the thread waiting for the response is interrupted
     * @throws UnsupportedOperationException if the docker daemon you're connecting to doesn't support "exec".
     */
    public static ExecResult execInContainerInternal(
        DockerClient dockerClient,
        InspectContainerResponse containerInfo,
        Charset outputCharset,
        ExecConfig execConfig
    ) throws UnsupportedOperationException, IOException, InterruptedException {
        if (!isRunning(containerInfo)) {
            throw new IllegalStateException("execInContainer can only be used while the Container is running");
        }

        String containerId = containerInfo.getId();
        String containerName = containerInfo.getName();

        String[] command = execConfig.getCommand();
        LOGGER.debug("{}: Running \"exec\" command: {}", containerName, String.join(" ", command));
        final ExecCreateCmd execCreateCmd = dockerClient
            .execCreateCmd(containerId)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withCmd(command);

        String user = execConfig.getUser();
        if (user != null && !user.isEmpty()) {
            LOGGER.debug("{}: Running \"exec\" command with user: {}", containerName, user);
            execCreateCmd.withUser(user);
        }

        String workDir = execConfig.getWorkDir();
        if (workDir != null && !workDir.isEmpty()) {
            LOGGER.debug("{}: Running \"exec\" command inside workingDir: {}", containerName, workDir);
            execCreateCmd.withWorkingDir(workDir);
        }

        Map<String, String> envVars = execConfig.getEnvVars();
        if (envVars != null && !envVars.isEmpty()) {
            List<String> envVarList = envVars
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.toList());
            execCreateCmd.withEnv(envVarList);
        }

        final ExecCreateCmdResponse execCreateCmdResponse = execCreateCmd.exec();

        final ToStringConsumer stdoutConsumer = new ToStringConsumer();
        final ToStringConsumer stderrConsumer = new ToStringConsumer();

        try (FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
            callback.addConsumer(OutputFrame.OutputType.STDOUT, stdoutConsumer);
            callback.addConsumer(OutputFrame.OutputType.STDERR, stderrConsumer);

            dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();
        }
        int exitCode = dockerClient.inspectExecCmd(execCreateCmdResponse.getId()).exec().getExitCodeLong().intValue();

        final ExecResult result = new ExecResult(
            exitCode,
            stdoutConsumer.toString(outputCharset),
            stderrConsumer.toString(outputCharset)
        );

        LOGGER.trace("{}: stdout: {}", containerName, result.getStdout());
        LOGGER.trace("{}: stderr: {}", containerName, result.getStderr());
        return result;
    }

    private static boolean isRunning(InspectContainerResponse containerInfo) {
        try {
            return containerInfo != null && containerInfo.getState().getStatus().equals("created");
        } catch (DockerException e) {
            return false;
        }
    }
}
