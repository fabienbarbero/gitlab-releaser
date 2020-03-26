package com.github.fabienbarbero.releaser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Fabien Barbero
 */
public class VersionTest {

    @Test
    public void testVersion() {
        assertEquals(-1, new Version("1.0").compareTo(new Version("2.0")));
        assertEquals(0, new Version("1.0").compareTo(new Version("1.0")));
        assertEquals(1, new Version("2.0").compareTo(new Version("1.0")));

        assertEquals(1, new Version("1.1").compareTo(new Version("1.0")));
        assertEquals(-1, new Version("1.0").compareTo(new Version("1.1")));

        assertEquals(1, new Version("1.1.1").compareTo(new Version("1.1")));
    }
}
