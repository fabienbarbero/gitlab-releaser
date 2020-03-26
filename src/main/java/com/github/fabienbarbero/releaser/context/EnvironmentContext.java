package com.github.fabienbarbero.releaser.context;

public class EnvironmentContext
        implements JobContext {

    @Override
    public String getGitlabUrl() {
        return System.getenv("CI_SERVER_URL");
    }

    @Override
    public String getProjectId() {
        return System.getenv("CI_PROJECT_ID");
    }

    @Override
    public String getProjectName() {
        return System.getenv("CI_PROJECT_NAME");
    }

    @Override
    public String getBuildBranch() {
        return System.getenv("CI_COMMIT_BRANCH");
    }

    @Override
    public String getMergeRequestSourceBranch() {
        return System.getenv("CI_MERGE_REQUEST_SOURCE_BRANCH_NAME");
    }

}
