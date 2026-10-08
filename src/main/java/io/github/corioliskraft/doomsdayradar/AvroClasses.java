package io.github.corioliskraft.doomsdayradar;

import org.apache.avro.util.ClassSecurityValidator;

final class AvroClasses {

    static synchronized void trustOwnClasses() {
        var global = ClassSecurityValidator.getGlobal();
        if (!global.isTrusted(SquareCount.class)) {
            ClassSecurityValidator.setGlobal(
                    ClassSecurityValidator.composite(
                            global,
                            ClassSecurityValidator.builder().add(SquareCount.class).build()));
        }
    }

    private AvroClasses() {}
}
