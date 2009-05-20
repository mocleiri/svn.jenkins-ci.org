package hudson.plugins.mantis;

import hudson.MarkupText;
import hudson.Util;
import hudson.MarkupText.SubText;
import hudson.model.AbstractBuild;
import hudson.plugins.mantis.model.MantisIssue;
import hudson.scm.ChangeLogAnnotator;
import hudson.scm.ChangeLogSet.Entry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Creates HTML link for Mantis issues.
 *
 * @author Seiji Sogabe
 */
public final class MantisLinkAnnotator extends ChangeLogAnnotator {

    @Override
    public void annotate(final AbstractBuild<?, ?> build, final Entry change, final MarkupText text) {
        final MantisProjectProperty mpp = build.getParent().getProperty(MantisProjectProperty.class);
        if (mpp == null || mpp.getSite() == null) {
            return;
        }
        if (!mpp.isLinkEnabled()) {
            return;
        }

        final MantisBuildAction action = build.getAction(MantisBuildAction.class);
        final String url = mpp.getSite().getUrl().toExternalForm();
        final Pattern pattern = mpp.getRegExp();
        final List<MantisIssue> notSavedIssues = new ArrayList<MantisIssue>();

        for (final SubText st : text.findTokens(pattern)) {
            // retrieve id from changelog
            int id;
            try {
                id = Integer.valueOf(st.group(1));
            } catch (final NumberFormatException e) {
                LOGGER.log(Level.WARNING, Messages.MantisLinkAnnotator_IllegalMantisId(st.group(1)));
                continue;
            }

            // get the issue from saved one or Mantis
            MantisIssue issue;
            if (action != null) {
                issue = action.getIssue(id);
            } else {
                issue = getIssueFromMantis(build, id);
                if (issue != null) {
                    notSavedIssues.add(issue);
                }
            }

            // decorate changelog woth hyperlink
            final String newUrl = Util.encode(url + "view.php?id=$1");
            if (issue == null) {
                LOGGER.log(Level.WARNING, Messages.MantisLinkAnnotator_FailedToGetMantisIssue(id));
                st.surroundWith("<a href='" + newUrl + "'>", "</a>");
            } else {
                final String summary = Utility.escape(issue.getSummary());
                st.surroundWith(String.format("<a href='%s' tooltip='%s'>", newUrl, summary), "</a>");
            }
        }

        if (!notSavedIssues.isEmpty()) {
            saveIssues(build, notSavedIssues);
        }
    }

    private void saveIssues(AbstractBuild<?, ?> build, final List<MantisIssue> issues) {
        final MantisBuildAction action = new MantisBuildAction(issues.toArray(new MantisIssue[0]));
        build.getActions().add(action);
        try {
            build.save();
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, Messages.MantisLinkAnnotator_FailedToSave(), e);
        }
    }

    private MantisIssue getIssueFromMantis(final AbstractBuild<?, ?> build, final int id) {
        MantisSite site = MantisSite.get(build.getProject());
        MantisIssue issue = null;
        try {
            issue = site.getIssue(id);
        } catch (final MantisHandlingException e) {
            //
        }
        return issue;
    }

    private static final Logger LOGGER = Logger.getLogger(MantisLinkAnnotator.class.getName());
}
