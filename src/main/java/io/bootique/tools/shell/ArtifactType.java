package io.bootique.tools.shell;

public enum ArtifactType {
    APP,
    MODULE;

    public static ArtifactType byName(String name) {
        String upperCaseName = name.toUpperCase();
        for (ArtifactType next : values()) {
            if (next.name().equals(upperCaseName)
                    || next.name().startsWith(upperCaseName)) {
                return next;
            }
        }
        return null;
    }
}
