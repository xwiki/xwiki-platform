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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.activeinstalls.ActiveInstallsConfiguration;
import org.xwiki.activeinstalls.internal.JestClientManager;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.instance.InstanceId;

import io.searchbox.client.JestResult;
import io.searchbox.core.Index;

/**
 * Thread that regularly sends information about the current instance (its unique id + the id and versions of all
 * installed extensions) to a central active installs Elastic Search server in order to count the number of active
 * installs of XWiki (and to know what extensions and in which versions they use).
 *
 * @version $Id$
 * @since 5.2M2
 */
public class ActiveInstallsPingThread extends Thread
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveInstallsPingThread.class);

    /**
     * Once every 24 hours.
     */
    private static final long WAIT_TIME = 1000L*60L*60L*24L;

    private static final String LATEST_FORMAT_VERSION = "1.0";

    private static final DateTimeFormatter DATE_FORMATTER = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    private InstanceId instanceId;

    private InstalledExtensionRepository extensionRepository;

    private ActiveInstallsConfiguration configuration;

    private JestClientManager jestClientManager;

    public ActiveInstallsPingThread(InstanceId instanceId, ActiveInstallsConfiguration configuration,
        InstalledExtensionRepository extensionRepository, JestClientManager jestClientManager)
    {
        this.instanceId = instanceId;
        this.extensionRepository = extensionRepository;
        this.configuration = configuration;
        this.jestClientManager = jestClientManager;
    }

    @Override
    public void run()
    {
        while (true) {
            try {
                sendPing();
            } catch (Exception e) {
                // Failed to connect or send the ping to the remote Elastic Search instance, will try again after the
                // sleep.
                LOGGER.warn(
                    "Failed to send Active Installation ping to [{}]. Error = [{}]. Will retry in [{}] seconds...",
                    this.configuration.getPingInstanceURL(), ExceptionUtils.getRootCause(e), WAIT_TIME/1000);
            }
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    protected void sendPing() throws Exception
    {
        Index index = new Index.Builder(constructJSON())
            .index("installs")
            .type("install")
            .id(this.instanceId.toString())
            .build();
        JestResult result = this.jestClientManager.getClient().execute(index);
        if (!result.isSucceeded()) {
            throw new Exception(result.getErrorMessage());
        }
    }

    private String constructJSON()
    {
        StringBuffer source = new StringBuffer();
        source.append('{');
        source.append("\"formatVersion\" : \"" + LATEST_FORMAT_VERSION + "\",");
        source.append("\"date\" : \"" + DATE_FORMATTER.print(new Date().getTime()) + "\",");
        source.append("\"extensions\" : [");

        Collection<InstalledExtension> installedExtensions = this.extensionRepository.getInstalledExtensions();
        Iterator<InstalledExtension> it = installedExtensions.iterator();
        while (it.hasNext()) {
            InstalledExtension extension = it.next();
            source.append('{');
            source.append("\"id\" : \"" + extension.getId().getId() + "\",");
            source.append("\"version\" : \"" + extension.getId().getVersion().toString() + "\"");
            source.append('}');
            if (it.hasNext()) {
                source.append(',');
            }
        }

        source.append(']');
        source.append('}');

        return source.toString();
    }
}
