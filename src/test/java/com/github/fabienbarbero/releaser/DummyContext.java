package com.github.fabienbarbero.releaser;

import com.github.fabienbarbero.releaser.context.JobContext;

/**
 * @author Fabien Barbero
 */
public class DummyContext implements JobContext {

    private String buildBranch;
    private String mergeRequestSourceBranch;

    @Override
    public String getGitlabUrl() {
        return "http://www.gitlab.com";
    }

    @Override
    public String getProjectId() {
        return "projectId";
    }

    @Override
    public String getProjectName() {
        return "test";
    }

    @Override
    public String getBuildBranch() {
        return buildBranch;
    }

    @Override
    public String getMergeRequestSourceBranch() {
        return mergeRequestSourceBranch;
    }

    public void setBuildBranch(String buildBranch) {
        this.buildBranch = buildBranch;
    }

    public void setMergeRequestSourceBranch(String mergeRequestSourceBranch) {
        this.mergeRequestSourceBranch = mergeRequestSourceBranch;
    }
}
