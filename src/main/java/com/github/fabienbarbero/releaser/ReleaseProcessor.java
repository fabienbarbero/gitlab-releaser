package com.github.fabienbarbero.releaser;

import com.github.fabienbarbero.releaser.context.JobContext;
import org.gitlab4j.api.GitLabApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReleaseProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseProcessor.class);

    private final JobContext context;
    private final GitLabApi gitLabApi;

    public ReleaseProcessor(JobContext context, String accessToken) {
        this.context = context;

        gitLabApi = new GitLabApi(context.getGitlabUrl(), accessToken);
        gitLabApi.setRequestTimeout(1000, 5000);
    }

    public void run() throws ExecutionException {
        String buildBranch = context.getBuildBranch();
        LOGGER.info("Starting release of branch {}", buildBranch);
        String tag;

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
    }

    private String createRelease() {
        String sourceBranch = context.getMergeRequestSourceBranch();
        // TODO
    }

    private String createReleaseCandidate() {
        String releaseBranch = context.getBuildBranch();
        // TODO
    }

    private String createHotfixRelease() {
        // TODO
    }
}
