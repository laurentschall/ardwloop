package org.llschall.ardwloop.serial;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.llschall.ardwloop.jni.NativeEntry;

public class SetupTest {

    @Test
    public void testSetup() {
        System.out.println("#################");
        System.out.println("## Setup Test ###");
        System.out.println("#################");

        Assertions.assertEquals(1, 1);
    }

    @Test
    public void testProperty() {
        String property = System.getProperty("test.setup");
        Assertions.assertEquals("Allschwil", property);
    }

    @Test
    public void testJni() {

        NativeEntry entry = new NativeEntry();
        int ping = entry.ping();
        Assertions.assertEquals(2023, ping);
    }
}
