package br.com.toluja.printagent;

public final class Version {
    private static final String FALLBACK_VERSION = "dev";

    private Version() {
    }

    public static String value() {
        Package pkg = Version.class.getPackage();
        if (pkg == null || pkg.getImplementationVersion() == null
                || pkg.getImplementationVersion().isBlank()) {
            return FALLBACK_VERSION;
        }
        return pkg.getImplementationVersion();
    }

    public static String display() {
        return "Toluja Print Agent v2 " + value();
    }
}
