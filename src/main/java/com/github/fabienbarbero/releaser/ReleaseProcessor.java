package com.github.fabienbarbero.releaser;

import com.github.fabienbarbero.releaser.context.JobContext;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.MergeRequestParams;
import org.gitlab4j.api.models.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReleaseProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseProcessor.class);
    private static final String BRANCH_RELEASE = "release-";
    private static final String BRANCH_HOTFIX = "hotfix-";
    private static final String BRANCH_MASTER = "master";
    private static final String BRANCH_DEVELOP = "develop";

    private final JobContext context;
    private final GitLabApi gitLabApi;

    public ReleaseProcessor(JobContext context, String accessToken) {
        this.context = context;

        gitLabApi = new GitLabApi(context.getGitlabUrl(), accessToken);
        gitLabApi.setRequestTimeout(1000, 5000);
    }

    // For tests
    ReleaseProcessor(JobContext context, GitLabApi gitLabApi) {
        this.context = context;
        this.gitLabApi = gitLabApi;
    }

    public void run()
            throws ExecutionException {
        String buildBranch = context.getBuildBranch();
        LOGGER.info("Starting release of branch {}", buildBranch);
        String tag;

        try {
            if (buildBranch.startsWith(BRANCH_RELEASE)) {
                tag = createReleaseCandidate();

            } else if (buildBranch.equals(BRANCH_MASTER)) {
                String mergeRequestBranch = context.getMergeRequestSourceBranch();
                if (mergeRequestBranch == null) {
                    // Commit on "master" without merge request
                    throw new ExecutionException("Only merge request must be used on master branch");

                } else if (mergeRequestBranch.startsWith(BRANCH_RELEASE)) {
                    tag = createFinalRelease();

                } else if (mergeRequestBranch.startsWith(BRANCH_HOTFIX)) {
                    tag = createHotfix();

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

    private String createFinalRelease()
            throws GitLabApiException {
        LOGGER.info("Will release final sources");
        String sourceBranch = context.getMergeRequestSourceBranch();
        String tagName = context.getProjectName() + "-" + getReleaseVersion(sourceBranch);

        // Create tag
        Tag tag = gitLabApi.getTagsApi().createTag(context.getProjectId(), tagName, context.getBuildBranch());
        mergeOnDevelop(BRANCH_MASTER);
        return tag.getName();
    }

    private String createHotfix()
            throws GitLabApiException {
        LOGGER.info("Will create hotfix release");

        // Get last release
        Version lastVersion = gitLabApi.getTagsApi().getTagsStream(context.getProjectId())
                .filter(tag -> !tag.getName().contains("-RC")) // FIXME: use regexp
                .map(tag -> getReleaseVersion(tag.getName()))
                .max(Comparator.naturalOrder()).orElseThrow(() -> new ExecutionException("No release found for hotfix"));

        String tagName;
        String tagPrefix = context.getProjectName() + "-";
        if (lastVersion.getCountParts() > 2) {
            // Hotfix already set. We increment it
            int minorVersion = lastVersion.getParts()[2] + 1;
            Version newVersion = new Version(lastVersion.getParts()[0], lastVersion.getParts()[1], minorVersion);
            tagName = tagPrefix + newVersion;

        } else {
            // Very first hotfix
            tagName = tagPrefix + lastVersion + ".1";
        }

        // Create tag
        Tag tag = gitLabApi.getTagsApi().createTag(context.getProjectId(), tagName, context.getBuildBranch());
        mergeOnDevelop(BRANCH_MASTER);

        return tag.getName();
    }

    private String createReleaseCandidate()
            throws GitLabApiException {
        LOGGER.info("Will create release candidate");
        String releaseBranch = context.getBuildBranch();
        String tagPrefix = context.getProjectName() + "-" + getReleaseVersion(releaseBranch) + "-RC";

        // Get last RC
        Integer lastVersion = gitLabApi.getTagsApi().getTagsStream(context.getProjectId(),
                                                                   Constants.TagOrderBy.UPDATED,
                                                                   Constants.SortOrder.DESC,
                                                                   "^" + tagPrefix)
                .map(tag -> getReleaseCandidateVersion(tag.getName()))
                .max(Comparator.naturalOrder()).orElse(0);

        int nextVersion = lastVersion + 1;
        String tagName = tagPrefix + nextVersion;

        // Create tag
        Tag tag = gitLabApi.getTagsApi().createTag(context.getProjectId(), tagName, releaseBranch);
        mergeOnDevelop(releaseBranch);
        return tag.getName();
    }

    private Version getReleaseVersion(String releaseBranchName) {
        Pattern pattern = Pattern.compile("-(.*)$");
        Matcher matcher = pattern.matcher(releaseBranchName);
        if (!matcher.find()) {
            throw new ExecutionException("Malformed release branch " + releaseBranchName);
        }
        return new Version(matcher.group(1));
    }

    private int getReleaseCandidateVersion(String tagName) {
        Pattern pattern = Pattern.compile("-RC([0-9]+)$");
        Matcher matcher = pattern.matcher(tagName);
        if (!matcher.find()) {
            throw new ExecutionException("Invalid release candidate tag: " + tagName);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private void mergeOnDevelop(String sourceBranch)
            throws GitLabApiException {
        MergeRequestParams mrp = new MergeRequestParams()
                .withSourceBranch(sourceBranch)
                .withTargetBranch(BRANCH_DEVELOP);
        MergeRequest mr = gitLabApi.getMergeRequestApi().createMergeRequest(context.getProjectId(), mrp);
        gitLabApi.getMergeRequestApi().acceptMergeRequest(context.getProjectId(), mr.getIid());
        LOGGER.info("Branch {} merged on develop", sourceBranch);
    }
}
