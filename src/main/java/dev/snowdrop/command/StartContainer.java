package dev.snowdrop.command;

import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import dev.snowdrop.Container;
import dev.snowdrop.MyIDP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static dev.snowdrop.ContainerUtils.fetchContainerId;

@CommandLine.Command(name = "start", description = "Start a running container")
public class StartContainer extends Container implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartContainer.class);

    @CommandLine.Parameters(index = "0", description = "The ID or name of the container to start")
    String containerIdOrName;

    @Override
    public Integer call() {
        //System.setProperty("org.slf4j.simpleLogger.log.com.github.dockerjava", "debug");
        try {
            var containerId = fetchContainerId(containerIdOrName);

            var containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            LOGGER.info("Container state: {}", containerInfo.getState().getStatus());
            String status = containerInfo.getState().getStatus(); // "exited", "running", etc.
            Integer exitCode = containerInfo.getState().getExitCode();
            String error = containerInfo.getState().getError(); // May contain useful error text
            String startedAt = containerInfo.getState().getStartedAt();
            String finishedAt = containerInfo.getState().getFinishedAt();

            LOGGER.info("Container status: {}", status);
            LOGGER.info("Exit code: {}", exitCode);
            LOGGER.info("Error message: {}", error);
            LOGGER.info("Started at: {}, Finished at: {}", startedAt, finishedAt);

            diagnoseContainerExit(containerId);

            dockerClient.startContainerCmd(containerId)
                .exec();
            LOGGER.info("Container started: {}", containerId);
            closeDockerClient();
            return 0;
        } catch (NotModifiedException e) {
            LOGGER.info("Container is already running {}.", containerIdOrName);
            return 0;
        } catch (NotFoundException e) {
            LOGGER.error("Container not found: " + containerIdOrName);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error starting the container {}: {}", containerIdOrName, e.getMessage(), e);
            return 1;
        }
    }

    public void diagnoseContainerExit(String containerId) {
        try {
            var info = dockerClient.inspectContainerCmd(containerId).exec();
            var exitCode = info.getState().getExitCode();
            var error = info.getState().getError();
            var status = info.getState().getStatus();

            LOGGER.info("Container state: {}", status);
            LOGGER.info("Exit code: {}", exitCode);
            LOGGER.info("Error: {}", error);

            String logs = getContainerLogs(containerId);
            LOGGER.info("Container logs:\n{}", logs);

        } catch (Exception e) {
            LOGGER.error("Error while diagnosing container {}: {}", containerId, e.getMessage(), e);
        }
    }

    public String getContainerLogs(String containerId) throws InterruptedException {
        StringBuilder logBuilder = new StringBuilder();

        dockerClient.logContainerCmd(containerId)
            .withStdOut(true)
            .withStdErr(true)
            .withTailAll() // Get all logs
            .exec(new LogContainerResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    logBuilder.append(new String(frame.getPayload()));
                }
            }).awaitCompletion(10, TimeUnit.SECONDS); // Wait for log stream to complete

        return logBuilder.toString();
    }


}
