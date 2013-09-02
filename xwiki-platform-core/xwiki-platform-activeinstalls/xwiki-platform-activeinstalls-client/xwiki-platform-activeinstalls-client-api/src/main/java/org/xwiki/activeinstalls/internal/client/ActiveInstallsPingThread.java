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
package org.xwiki.activeinstalls.internal.client;

import java.io.IOException;
import java.util.Date;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.xwiki.activeinstalls.client.InstanceId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;

import static org.elasticsearch.common.xcontent.XContentFactory.*;

public class ActiveInstallsPingThread extends Thread
{
    /**
     * Once every 24 hours.
     */
    private static final long WAIT_TIME = 1000L*60L*60L*24L;

    private static final String LATEST_FORMAT_VERSION = "1.0";

    private InstanceId instanceId;

    private InstalledExtensionRepository extensionRepository;

    public ActiveInstallsPingThread(InstanceId instanceId, InstalledExtensionRepository extensionRepository)
    {
        this.instanceId = instanceId;
        this.extensionRepository = extensionRepository;
    }

    @Override
    public void run()
    {
        //Settings settings = ImmutableSettings.settingsBuilder().build();
        TransportClient client = new TransportClient();
        client.addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

        while (true) {
            try {
                sendPing(client);
            } catch (IOException e) {
                // Failed to connect or send the ping to the remote Elastic Search instance, will try again after the
                // sleep.
            }
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
                break;
            }
        }

        client.close();
    }

    private void sendPing(TransportClient client) throws IOException
    {
        XContentBuilder builder = jsonBuilder();

        builder.startObject();

        builder.field("formatVersion", LATEST_FORMAT_VERSION);
        builder.field("date", new Date());

        builder.startArray("extensions");

        for (InstalledExtension extension : this.extensionRepository.getInstalledExtensions()) {
            builder.startObject();
            builder.field("id", extension.getId().getId());
            builder.field("version", extension.getId().getVersion().toString());
            builder.endObject();
        }

        builder.endArray();

        builder.endObject();

        client.prepareIndex("installs", "install", this.instanceId.toString())
            .setSource(builder)
            .execute()
            .actionGet();
    }
}
