/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.test.docker.internal.junit5.servletengine;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.test.docker.junit5.TestConfiguration;

/**
 * Compute the Java options telling an XWiki instance where to find the other members of the cluster, so that the
 * instances can exchange remote events.
 * <p>
 * The {@code tcp} channel embedded in the JGroups JAR is used, since the JGroups {@code udp} channel relies on IP
 * multicast which is usually not available (in a Docker network in particular), and its members are passed with the
 * {@code jgroups.tcpping.initial_hosts} system property. The channel itself is enabled in {@code xwiki.properties}
 * (see {@code ConfigurationFilesGenerator}).
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
final class RemoteObservationJavaOptions
{
    /**
     * The port on which the JGroups channel of the first XWiki instance listens (i.e. the default port of the JGroups
     * TCP transport, which cannot be configured with a system property since the embedded configuration sets it).
     */
    private static final int PORT = 7800;

    private static final String LOCALHOST = "localhost";

    private RemoteObservationJavaOptions()
    {
        // Utility class
    }

    /**
     * @param index the index of the XWiki instance for which to compute the options
     * @param testConfiguration the configuration of the test (number of XWiki instances, servlet engine, etc)
     * @return the Java options to pass to the XWiki instance, or an empty list when the test uses a single instance
     *     (i.e. when no remote event needs to be exchanged)
     */
    static List<String> get(int index, TestConfiguration testConfiguration)
    {
        if (!testConfiguration.isCluster()) {
            return List.of();
        }

        List<String> options = new ArrayList<>();

        // Give each instance the index as identifier among the members of the cluster (it defaults to a generated
        // UUID), so that tests can predict it and so that the instances can be told apart in the logs. It's passed as
        // a system property because the instances share the same generated xwiki.properties when they run in Docker.
        options.add(String.format("-Dxconf.xwikiproperties.observation.remote.id=%s", index));

        // Bind the channel to the address on which the other members contact this instance. This cannot be left to
        // JGroups: its default is the first network interface of the host, which is not necessarily the one the other
        // members use (a container is attached to the shared network but also to the default Docker network, and the
        // host generally has its own network in addition to the loopback interface).
        options.add(String.format("-Djgroups.bind_addr=%s", getHost(index, testConfiguration)));

        List<String> members = new ArrayList<>();
        for (int memberIndex = 0; memberIndex < testConfiguration.getXWikiInstances().value(); ++memberIndex) {
            members.add(String.format("%s[%s]", getHost(memberIndex, testConfiguration),
                getPort(memberIndex, testConfiguration)));
        }
        options.add(String.format("-Djgroups.tcpping.initial_hosts=%s", String.join(",", members)));

        return options;
    }

    private static String getHost(int index, TestConfiguration testConfiguration)
    {
        // Inside Docker each instance is reachable through its own network alias, while outside of Docker all the
        // instances run on the host.
        return testConfiguration.getServletEngine().isOutsideDocker() ? LOCALHOST
            : ServletContainerExecutor.getNetworkAlias(index);
    }

    private static int getPort(int index, TestConfiguration testConfiguration)
    {
        // Instances running in their own container can all use the same port. Instances sharing the host cannot: since
        // the port cannot be configured, each of them binds the first free port in [PORT, PORT + port_range] (the
        // JGroups transport "port_range" defaults to 10, which is more than enough for a test cluster) and thus the
        // ports are allocated in the order in which the instances are started.
        return testConfiguration.getServletEngine().isOutsideDocker() ? PORT + index : PORT;
    }
}
