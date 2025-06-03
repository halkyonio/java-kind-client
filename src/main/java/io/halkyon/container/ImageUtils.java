package io.halkyon.container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ImageUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtils.class);

    /**
     * Util method to retrieve info for a given container image.
     */
    public static ImageInfo inspectImage(DockerClient dc, ImageReference image) {
        String imageName = image.getReferenceWithLatest();
        InspectImageResponse iir = dc.inspectImageCmd(imageName).exec();

        // keep only some of the info.
        ImageInfo ii = new ImageInfo();
        ii.id = iir.getId();

        if (iir.getRepoDigests() != null) {
            ii.digest = iir.getRepoDigests().toString();
        }
        if (iir.getRepoTags() != null) {
            ii.tags = iir.getRepoTags().toString();
        }
        ii.labels = iir.getConfig().getLabels();
        ii.env = iir.getConfig().getEnv();
        if (iir.getArch() != null && !iir.getArch().isEmpty() && iir.getOs() != null && !iir.getOs().isEmpty()) {
            ii.platform = iir.getOs() + "/" + iir.getArch();
        } else {
            ii.platform = null;
        }
        return ii;
    }

    public static void pullImage(DockerClient dockerClient, String imageName) throws InterruptedException {

        boolean imageExists = false;

        List<Image> images = dockerClient.listImagesCmd().exec();
        for (Image image : images) {
            if (image.getRepoTags() != null) {
                for (String repoTag : image.getRepoTags()) {
                    var fullImageReference = "docker.io/" + imageName;
                    if (repoTag.equals(fullImageReference)) {
                        imageExists = true;
                        break;
                    }
                }
            }
        }

        if (!imageExists) {
            LOGGER.info("Pulling kindest/node image: {}", imageName);
            dockerClient.pullImageCmd(imageName)
                .exec(new PullImageResultCallback())
                .awaitCompletion();
            LOGGER.info("Image pulled: {}", imageName);
        } else {
            LOGGER.info("Image already pulled: {}", imageName);
        }
    }
}
