package com.github.fabienbarbero.releaser;

import com.github.fabienbarbero.releaser.config.Configuration;
import com.github.fabienbarbero.releaser.connector.GitlabConnector;

public class ReleaseProcessor {

    private final Configuration configuration;
    private final GitlabConnector connector;

    public ReleaseProcessor(Configuration configuration, GitlabConnector connector) {
        this.configuration = configuration;
        this.connector = connector;
    }
}
