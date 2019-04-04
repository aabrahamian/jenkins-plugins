package com.affirm.jenkins.github;

import hudson.Extension;
import hudson.model.*;
import hudson.model.queue.QueueTaskFuture;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.jenkinsci.plugins.github.util.JobInfoHelpers.asParameterizedJobMixIn;

/**
 * Triggers a build when we learn that a tag has changed on GitHub.
 * <p>
 * This creates a UI element on the job configuration page.
 * </p>
 *
 * @author jimjh
 * @see <a href="https://developer.github.com/webhooks/">GitHub Webhooks</a>
 * @since 1.0
 */
public class TagTrigger extends Trigger<Job<?, ?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagTrigger.class);

    public final String regex;

    @DataBoundConstructor
    public TagTrigger(@Nonnull String regex) {
        this.regex = regex;
    }

    /**
     * Invoked when a tag-related event is received.
     *
     * @param cause build cause
     * @return task future, or {@code null} if it could not be scheduled.
     */
    public QueueTaskFuture startJob(@Nonnull TagCause cause) {
        LOGGER.info(cause.getShortDescription());

        ArrayList<ParameterValue> values = getDefaultParameters();
        values.add(new StringParameterValue("GH_PUSHER", cause.getPusher()));
        values.add(new StringParameterValue("TAG_TO_USE", cause.getTag()));
        values.add(new StringParameterValue("GIT_REF", cause.getRef()));

        ParameterizedJobMixIn pjob = asParameterizedJobMixIn(job);
        return pjob.scheduleBuild2(3, new CauseAction(cause), new ParametersAction(values));
    }

    /**
     * @param cause tag cause
     * @return true iff the regular expression matches the given cause
     */
    public boolean matches(@Nonnull TagCause cause) {
        return Pattern.compile(regex).asPredicate().test(cause.getTag());
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        @Override
        public String getDisplayName() {
            return "Build when a tag is edited on GitHub";
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof Job &&
                    SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(item) != null &&
                    item instanceof ParameterizedJobMixIn.ParameterizedJob;
        }
    }

    /**
     * Retrieve parameters to allow manual triggering.
     *
     * @return list of parameters set in the project's configuration
     */
    private ArrayList<ParameterValue> getDefaultParameters() {
        ArrayList<ParameterValue> values = new ArrayList<>();
        ParametersDefinitionProperty pdp = job.getProperty(ParametersDefinitionProperty.class);
        if (null != pdp) {
            for (ParameterDefinition pd : pdp.getParameterDefinitions()) {
                // we don't need to add params that the trigger will be setting anyway
                if (pd.getName().equals("GIT_REF") || pd.getName().equals("TAG_TO_USE") || pd.getName().equals("GH_PUSHER"))
                    continue;
                values.add(pd.getDefaultParameterValue());
            }
        }
        return values;
    }
}
