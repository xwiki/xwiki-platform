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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.xwiki.activeinstalls.internal.JestClientManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.instance.InstanceIdManager;

import io.searchbox.client.JestResult;
import io.searchbox.core.Index;

@Component
@Singleton
public class DefaultPingSender implements PingSender
{
    private static final String LATEST_FORMAT_VERSION = "1.0";

    private static final DateTimeFormatter DATE_FORMATTER = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    @Inject
    private InstalledExtensionRepository extensionRepository;

    @Inject
    private JestClientManager jestClientManager;

    @Inject
    private InstanceIdManager instanceIdManager;

    @Override
    public void sendPing() throws Exception
    {
        Index index = new Index.Builder(constructJSON())
            .index("installs")
            .type("install")
            .id(this.instanceIdManager.getInstanceId().toString())
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
