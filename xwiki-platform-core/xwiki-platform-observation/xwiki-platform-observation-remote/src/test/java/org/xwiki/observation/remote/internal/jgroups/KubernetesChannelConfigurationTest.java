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
package org.xwiki.observation.remote.internal.jgroups;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jgroups.JChannel;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.protocols.FD_ALL3;
import org.jgroups.protocols.FD_SOCK2;
import org.jgroups.protocols.FRAG4;
import org.jgroups.protocols.MERGE3;
import org.jgroups.protocols.MFC;
import org.jgroups.protocols.TCP;
import org.jgroups.protocols.UFC;
import org.jgroups.protocols.UNICAST3;
import org.jgroups.protocols.VERIFY_SUSPECT2;
import org.jgroups.protocols.dns.DNS_PING;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.stack.Protocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Validate the Kubernetes JGroups stack configuration shipped with XWiki: JGroups checks the protocol and attribute
 * names when creating the channel, so a typo in the configuration file fails this test.
 *
 * @version $Id$
 */
class KubernetesChannelConfigurationTest
{
    private static final String DNS_QUERY_PROPERTY = "jgroups.dns.dns_query";

    private static final String BIND_ADDR_PROPERTY = "jgroups.bind_addr";

    private final Map<String, String> previousProperties = new HashMap<>();

    @BeforeEach
    void beforeEach()
    {
        // DNS_PING refuses to initialize without a DNS query, which is exactly what we want in production
        setProperty(DNS_QUERY_PROPERTY, "localhost");
        // Don't depend on the network interfaces of the machine running the test
        setProperty(BIND_ADDR_PROPERTY, "127.0.0.1");
    }

    @AfterEach
    void afterEach()
    {
        for (Map.Entry<String, String> entry : this.previousProperties.entrySet()) {
            if (entry.getValue() == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    private void setProperty(String key, String value)
    {
        this.previousProperties.put(key, System.setProperty(key, value));
    }

    @Test
    void kubernetes() throws Exception
    {
        // Create (but don't connect) a channel to make sure the configuration is valid
        try (JChannel channel = new JChannel(loadConfiguration("kubernetes"))) {
            List<Protocol> protocols = channel.getProtocolStack().getProtocols();

            // The stack is stored top to bottom
            assertEquals(
                List.of(FRAG4.class, UFC.class, MFC.class, GMS.class, STABLE.class, UNICAST3.class, NAKACK2.class,
                    VERIFY_SUSPECT2.class, FD_ALL3.class, FD_SOCK2.class, MERGE3.class, DNS_PING.class, TCP.class),
                protocols.stream().map(Protocol::getClass).collect(Collectors.toList()));

            TCP tcp = channel.getProtocolStack().findProtocol(TCP.class);
            assertEquals(7800, tcp.getValue("bind_port"));
            assertEquals(0, tcp.getValue("port_range"));
            DNS_PING dnsPing = channel.getProtocolStack().findProtocol(DNS_PING.class);
            assertEquals("localhost", dnsPing.getDNSQuery());
            assertEquals("A", dnsPing.dnsRecordType());
        }
    }

    private XmlConfigurator loadConfiguration(String channelId) throws Exception
    {
        String path = JGroupsNetworkChannel.CONFIGURATION_PATH + channelId + ".xml";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing configuration file [" + path + "]");

            return XmlConfigurator.getInstance(stream);
        }
    }
}
