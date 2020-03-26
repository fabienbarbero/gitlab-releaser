package com.github.fabienbarbero.releaser.context;

public interface JobContext {

    String getGitlabUrl();

    String getProjectId();

    String getBuildBranch();

    String getMergeRequestSourceBranch();

}
