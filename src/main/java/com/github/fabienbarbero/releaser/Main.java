package com.github.fabienbarbero.releaser;

import com.github.fabienbarbero.releaser.context.EnvironmentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            LOGGER.error("The access token must be set as parameter");
            System.exit(1);
            return;
        }

        try {
            ReleaseProcessor processor = new ReleaseProcessor(new EnvironmentContext(), args[0]);
            processor.run();

        } catch (ExecutionException ex) {
            LOGGER.error("Error releasing sources", ex);
            System.exit(1);
        }
    }
}
