package com.affirm.jenkins.github;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import com.fasterxml.jackson.databind.JsonNode;
import hudson.model.Job;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import static org.jenkinsci.plugins.github.util.JobInfoHelpers.triggerFrom;

/**
 * Instantiates causes, triggers, and schedules projects.
 *
 * @author jimjh
 */
public class TagBuilds {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagBuilds.class);

    static final String REF_PREFIX = "refs/tags/";

    /**
     * Receives events and instantiates builds.
     *
     * @param json payload from webhook
     */
    public static void onPush(@Nonnull JsonNode json) {

        // ignore pushes not related to tags
        String ref = json.get("ref").asText();
        if (!isTag(ref)) {
            LOGGER.debug("Skipped {} because it's not related to tags.", ref);
            return;
        }
        final String tag = ref.substring(REF_PREFIX.length());

        // ignore deletes
        boolean deleted = json.get("deleted").asBoolean();
        if (deleted) {
            LOGGER.debug("{} has been deleted, but no projects poked.", ref);
            return;
        }

        String pusher = json.get("pusher").get("name").asText();
        if (null == pusher) {
            LOGGER.warn("Missing GitHub username from webhook.");
            return;
        }

        String repoUrl = json.get("repository").get("url").asText();
        LOGGER.info("Received POST for {}, {}", repoUrl, ref);

        final GitHubRepositoryName repo = GitHubRepositoryName.create(repoUrl);
        if (null == repo) {
            LOGGER.warn("Malformed repo url {}", repoUrl);
            return;
        }

        final TagCause cause = new TagCause(ref, tag, pusher, json);

        // run in high privilege to see all the projects anonymous users don't see.
        // this is safe because when we actually schedule a build, it's a build that can
        // happen at some random time anyway.
        ACL.impersonate(ACL.SYSTEM, () -> {
            for (Job<?, ?> job : Jenkins.getInstance().getAllItems(Job.class)) {
                TagTrigger trigger = triggerFrom(job, TagTrigger.class);
                if (null == trigger) {
                    LOGGER.trace("Skipped {} because it doesn't have TagTrigger.", job.getFullDisplayName());
                } else {
                    poke(job, repo, trigger, cause);
                }
            }
        });
    }

    /**
     * Consider poking {@code job}.
     *
     * @param job     Jenkins job
     * @param repo    GitHub repository associated with the push event
     * @param trigger tag trigger
     * @param cause   tag cause
     */
    private static void poke(Job<?, ?> job, GitHubRepositoryName repo, TagTrigger trigger, TagCause cause) {
        String jobName = job.getFullDisplayName();
        LOGGER.debug("Considering to poke {}", jobName);
        if (GitHubRepositoryNameContributor.parseAssociatedNames(job).contains(repo) &&
                trigger.matches(cause)) {
            LOGGER.info("Poked {}", jobName);
            if (null == trigger.startJob(cause)) {
                LOGGER.error("Build for {} did not start.", jobName);
            }
        } else {
            LOGGER.debug("Skipped {} because it doesn't have a matching repository.", jobName);
        }
    }

    private static boolean isTag(String ref) {
        return null != ref && ref.startsWith(REF_PREFIX);
    }
}