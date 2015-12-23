package com.affirm.jenkins.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.util.FlushProofOutputStream;
import hudson.util.IOUtils;
import jenkins.model.RunAction2;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * UI object that says a build is started by a tag-related change on GitHub.
 * <p>
 * Adds a {@code /payload} link to the build page that shows the payload received from GitHub.
 * </p>
 *
 * @author jimjh
 */
public class TagCause extends Cause {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagCause.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private final String ref;
    private final String tag;
    private final String pusher;
    private transient final JsonNode payload;

    /**
     * @param ref    git ref for tag
     * @param tag    git tag
     * @param pusher GitHub user who did the push
     */
    public TagCause(String ref,
                    String tag,
                    String pusher,
                    JsonNode payload) {
        this.ref = ref;
        this.tag = tag;
        this.pusher = pusher;
        this.payload = payload;
    }

    public String getRef() {
        return ref;
    }

    public String getTag() {
        return tag;
    }

    public String getPusher() {
        return pusher;
    }

    @Override
    public String getShortDescription() {
        return format("Started by GitHub push by %s for %s", trimToEmpty(pusher), trimToEmpty(ref));
    }

    @Override
    public void onAddedTo(@Nonnull Run build) {
        if (null == payload) {
            LOGGER.warn("No GitHub payload found.");
            return;
        }
        try {
            BuildAction a = new BuildAction(build);
            MAPPER.writeValue(a.getPayloadFile(), payload);
            build.replaceAction(a);
        } catch (IOException e) {
            LOGGER.warn("Failed to persist the GitHub payload.", e);
        }
    }

    /**
     * Associated with {@link Run} to show the GitHub event
     * that triggered that build.
     *
     * @since 1.0
     */
    public static class BuildAction implements RunAction2 {

        private transient Run<?, ?> run;

        public BuildAction(Run<?, ?> r) {
            this.run = r;
        }

        @Override
        public void onAttached(Run<?, ?> r) {
            this.run = r;
        }

        @Override
        public void onLoad(Run<?, ?> r) {
            this.run = r;
        }

        @Override
        public String getIconFileName() {
            return "/plugin/github-tag-trigger/github.png";
        }

        @Override
        public String getDisplayName() {
            return "GitHub Payload";
        }

        @Override
        public String getUrlName() {
            return "payload";
        }

        /**
         * Displays the JSON payload from GitHub. Stapler API.
         *
         * @param req request
         * @param rsp response
         * @throws IOException
         */
        public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
            rsp.setContentType("application/json;charset=UTF-8");
            // Prevent jelly from flushing stream so Content-Length header can be added afterwards
            try (FlushProofOutputStream out = new FlushProofOutputStream(rsp.getCompressedOutputStream(req))) {
                IOUtils.copy(getPayloadFile(), out);
            }
        }

        private File getPayloadFile() {
            return new File(run.getRootDir(), "payload.json");
        }
    }
}
