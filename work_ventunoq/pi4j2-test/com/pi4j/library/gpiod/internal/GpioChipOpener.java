package com.pi4j.library.gpiod.internal;

import java.lang.reflect.Field;

/** Does what GpioDContext.initialize() does, minus its Raspberry-only check: opens the GPIO chip with the given label. */
public class GpioChipOpener {
    private static void loadNative(String name) throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("pi4j-", "-" + name);
        tmp.toFile().deleteOnExit();
        try (java.io.InputStream in = GpioChipOpener.class.getResourceAsStream("/lib/aarch64/pi4j-gpiod/" + name)) {
            java.nio.file.Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        System.load(tmp.toString());
    }

    public static String open(String wantedLabel) throws Exception {
        loadNative("libgpiod.so");
        loadNative("libpi4j-gpiod.so");
        long it = GpioD.chipIterNew();
        GpioChip found = null;
        Long p;
        while ((p = GpioD.chipIterNextNoClose(it)) != null) {
            GpioChip c = new GpioChip(p);
            if (found == null && c.getLabel().equals(wantedLabel)) found = c; else GpioD.chipClose(c.getCPointer());
        }
        GpioD.chipIterFreeNoClose(it);
        if (found == null) throw new IllegalStateException("gpiochip " + wantedLabel + " not found");
        Field f = GpioDContext.class.getDeclaredField("gpioChip");
        f.setAccessible(true);
        f.set(GpioDContext.getInstance(), found);
        return found.getName() + " " + found.getLabel();
    }
}
