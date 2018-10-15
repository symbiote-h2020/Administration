package eu.h2020.symbiote.administration;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper class for waiting other services to startup.
 *
 *  It is checking if service is available on specified host and port.
 *
 * @author Mario Kušek
 *
 */
public class WaitForPort {

    private int waitTimeInMilis;
    private int maxIteration;

    /**
     * Waits for services in format "host1:port1;host2:port2;..."
     *
     * It waits with default values of 1000 ms between checks and max
     * number of checks per host is 1000.
     *
     * @param hostWithPorts string that represents hosts and ports
     */
    public static void waitForServices(String hostWithPorts) {
        if(hostWithPorts == null) {
            System.out.println("No service to wait for. Continuing immediately.");
            return;
        }
        new WaitForPort(1000, 1000).start(hostWithPorts);
    }

    /**
     * Creates object that implements waiting for other services.
     *
     * @param waitTimeInMilis time beetween checks
     * @param maxIteration maximal number of iteretion per pair of host and port
     */
    public WaitForPort(int waitTimeInMilis, int maxIteration) {
        this.waitTimeInMilis = waitTimeInMilis;
        this.maxIteration = maxIteration;
    }

    /**
     * Starts waiting for services.
     *
     * @param hostsWithPorts this format is following "host1:port1;host2:port2;..."
     *
     * @throws RuntimeException when max wait time for some host is reached.
     */
    public void start(String hostsWithPorts) {
        List<HostPort> hostPorts = parse(hostsWithPorts);

        for(HostPort hp: hostPorts) {
            System.out.println("Checking availability of service: " + hp);
            for(int i = 1; i <= maxIteration; i++) {
                try (Socket socket = new Socket(hp.host, hp.port)) {
                    break;
                } catch (IOException e) {
                    System.out.println("Waiting for " + hp);
                    try {
                        Thread.sleep(waitTimeInMilis);
                    } catch (InterruptedException waitException) {
                    }
                }
                if(i == maxIteration) {
                    throw new RuntimeException("Max wait time reached for service " + hp.host + ":" + hp.port);
                }
            }
        }

    }

    /**
     * Parses string to list of HostPort objects.
     *
     * @param hostsWithPorts string in format: "host1:port1;host2:port2;..."
     * @return list of parsed HostPort objects
     */
    private List<HostPort> parse(String hostsWithPorts) {
        List<HostPort> list = new LinkedList<>();
        String[] hostPort = hostsWithPorts.split(";");
        for(String hp: hostPort) {
            String[] hpArray = hp.split(":");
            int port = Integer.parseInt(hpArray[1]);
            list.add(new HostPort(hpArray[0], port));
        }
        return list;
    }

    /**
     * Try to find property in environment variables. If there is no property it tries
     * in properties (-D option on startup).
     * @param propertyName the name of property
     * @return value of property
     */
    public static String findProperty(String propertyName) {
        String value = System.getenv("SPRING_BOOT_WAIT_FOR_SERVICES");
        if(value != null)
            return value;

        value = System.getProperty(propertyName);
        return value;
    }

    /**
     * Helper class that contains host and port data.
     *
     * @author Mario Kušek <mario.kusek@fer.hr>
     *
     */
    public static class HostPort {
        private String host;

        private int port;

        public HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }
    }
}
