package com.affirm.jenkins.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.model.Job;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.kohsuke.github.GHEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.immutableEnumSet;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.withTrigger;
import static org.kohsuke.github.GHEvent.PUSH;

/**
 * Listens to push events from GitHub when the job uses {@link TagTrigger}.
 * <p>
 * This is an extension for github-plugin, and piggyback's on github-plugin's webhooks. If this project grows, we
 * should start handling our own webhooks and remove the dependency on github-plugin.
 * </p>
 *
 * @author jimjh
 * @see <a href="https://developer.github.com/webhooks/">GitHub Webhooks</a>
 * @since 1.0
 */
@Extension
@SuppressWarnings("unused")
public class TagEventSubscriber extends GHEventsSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagEventSubscriber.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * This subscriber is only applicable for job with {@link TagTrigger}.
     *
     * @param project to check for trigger
     * @return true if project has {@link TagTrigger}
     */
    @Override
    protected boolean isApplicable(Job<?, ?> project) {
        return withTrigger(TagTrigger.class).apply(project);
    }

    /**
     * @return immutable set with the push event
     */
    @Override
    protected Set<GHEvent> events() {
        return immutableEnumSet(PUSH);
    }

    /**
     * If event is related to a tag change, calls {@link TagTrigger} in all projects to handle this hook.
     *
     * @param event   github push event
     * @param payload payload of gh-event
     */
    @Override
    protected void onEvent(GHEvent event, final String payload) {
        checkNotNull(payload);

        JsonNode json;
        try {
            json = MAPPER.readTree(payload);
        } catch (IOException e) {
            LOGGER.error("Unable to parse payload from GitHub as JSON.");
            return;
        }

        TagBuilds.onPush(json);
    }
}
