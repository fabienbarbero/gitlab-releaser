package com.github.fabienbarbero.releaser;

import com.github.fabienbarbero.releaser.context.JobContext;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReleaseProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseProcessor.class);

    private final JobContext context;
    private final GitLabApi gitLabApi;

    public ReleaseProcessor(JobContext context, String accessToken) {
        this.context = context;

        gitLabApi = new GitLabApi(context.getGitlabUrl(), accessToken);
        gitLabApi.setRequestTimeout(1000, 5000);
    }

    // For tests
    public ReleaseProcessor(JobContext context, GitLabApi gitLabApi) {
        this.context = context;
        this.gitLabApi = gitLabApi;
    }

    public void run() throws ExecutionException {
        String buildBranch = context.getBuildBranch();
        LOGGER.info("Starting release of branch {}", buildBranch);
        String tag;

        try {
            if (buildBranch.startsWith("release-")) {
                tag = createReleaseCandidate();

            } else if (buildBranch.equals("master")) {
                String mergeRequestBranch = context.getMergeRequestSourceBranch();
                if (mergeRequestBranch == null) {
                    // Commit on "master" without merge request
                    throw new ExecutionException("Only merge request must be used on master branch");

                } else if (mergeRequestBranch.startsWith("release-")) {
                    tag = createRelease();

                } else if (mergeRequestBranch.startsWith("hotfix-")) {
                    tag = createHotfixRelease();

                } else {
                    throw new ExecutionException("Only release ans hotfix branches must be used in merge request. Found " + mergeRequestBranch);
                }
            } else {
                throw new ExecutionException("Release can only be done on release and master branches. Found " + buildBranch);
            }

            LOGGER.info("New tag {} created", tag);

        } catch (GitLabApiException ex) {
            throw new ExecutionException("Error calling gitlab", ex);
        }
    }

    private String createRelease() throws GitLabApiException {
        LOGGER.info("Will release final sources");
        String sourceBranch = context.getMergeRequestSourceBranch();
        String tagName = context.getProjectName() + "-" + getReleaseVersion(sourceBranch);

        // Create tag
        Tag tag = gitLabApi.getTagsApi().createTag(context.getProjectId(), tagName, context.getBuildBranch());
        return tag.getName();
    }

    private String createReleaseCandidate() throws GitLabApiException {
        LOGGER.info("Will create release candidate");
        String releaseBranch = context.getBuildBranch();
        String tagPrefix = context.getProjectName() + "-" + getReleaseVersion(releaseBranch) + "-RC";

        // Get last RC
        Integer lastVersion = gitLabApi.getTagsApi().getTagsStream(
                context.getProjectId(), Constants.TagOrderBy.UPDATED, Constants.SortOrder.DESC, "^" + tagPrefix)
                .map(tag -> getReleaseCandidateVersion(tag.getName()))
                .max(Comparator.naturalOrder()).orElse(0);

        int nextVersion = lastVersion + 1;
        String tagName = tagPrefix + nextVersion;

        // Create tag
        Tag tag = gitLabApi.getTagsApi().createTag(context.getProjectId(), tagName, releaseBranch);
        return tag.getName();
    }

    private String createHotfixRelease() {
        // TODO
    }

    private String getReleaseVersion(String releaseBranchName) {
        Pattern pattern = Pattern.compile("^release-(.*)$");
        Matcher matcher = pattern.matcher(releaseBranchName);
        if (!matcher.find()) {
            throw new ExecutionException("Malformed release branch " + releaseBranchName);
        }
        return matcher.group(1);
    }

    private int getReleaseCandidateVersion(String tagName) {
        Pattern pattern = Pattern.compile("-RC([0-9]+)$");
        Matcher matcher = pattern.matcher(tagName);
        if (!matcher.find()) {
            throw new ExecutionException("Invalid release candidate tag: " + tagName);
        }
        return Integer.parseInt(matcher.group(1));
    }
}
