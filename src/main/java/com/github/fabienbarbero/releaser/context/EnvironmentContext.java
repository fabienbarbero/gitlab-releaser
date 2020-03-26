package com.github.fabienbarbero.releaser.context;

public class EnvironmentContext implements JobContext {

    @Override
    public String getGitlabUrl() {
        return System.getenv("CI_SERVER_URL");
    }

    @Override
    public String getProjectId() {
        return System.getenv("CI_PROJECT_ID");
    }

    public String getBuildBranch() {
        return System.getenv("CI_COMMIT_BRANCH");
    }

    @Override
    public String getMergeRequestSourceBranch() {
        return System.getenv("CI_MERGE_REQUEST_SOURCE_BRANCH_NAME");
    }

}
