package com.gluonapplication;

import java.net.URI;
import java.net.URISyntaxException;

final class ServerEndpoints {

    private static final String DEFAULT_HOST = "olserver135.virgo.infn.it";
    private static final String BACKEND_HOST_PROPERTY = "vacuum.supervisor.backend.host";
    private static final String BACKEND_HOST_ENV = "VACUUM_SUPERVISOR_BACKEND_HOST";

    private ServerEndpoints() {
    }

    static String jchvUrl() {
        return buildUrl(8081, "jchv/jchv");
    }

    static String jmasterUrl() {
        return buildUrl(8081, "jmaster/jmaster");
    }

    static String jcmdUrl() {
        return buildUrl(9081, "jcmd/jcmd");
    }

    private static String buildUrl(int port, String path) {
        return "http://" + resolveHost() + ":" + port + "/" + path;
    }

    private static String resolveHost() {
        String host = System.getProperty(BACKEND_HOST_PROPERTY);
        if (host == null || host.trim().isEmpty()) {
            host = System.getenv(BACKEND_HOST_ENV);
        }
        if (host == null || host.trim().isEmpty()) {
            host = DEFAULT_HOST;
        }
        return normalizeHost(host.trim());
    }

    private static String normalizeHost(String host) {
        if (host.indexOf("://") >= 0) {
            try {
                URI uri = new URI(host);
                if (uri.getHost() != null && !uri.getHost().trim().isEmpty()) {
                    return uri.getHost().trim();
                }
            } catch (URISyntaxException ignored) {
                // Fall back to the legacy string cleanup below.
            }
        }

        int slashIndex = host.indexOf('/');
        if (slashIndex >= 0) {
            host = host.substring(0, slashIndex);
        }

        int colonIndex = host.indexOf(':');
        if (colonIndex >= 0) {
            host = host.substring(0, colonIndex);
        }

        return host;
    }
}
