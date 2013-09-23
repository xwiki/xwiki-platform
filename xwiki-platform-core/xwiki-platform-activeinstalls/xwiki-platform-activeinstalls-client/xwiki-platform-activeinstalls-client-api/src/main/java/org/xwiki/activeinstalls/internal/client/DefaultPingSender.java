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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.xwiki.activeinstalls.internal.JestClientManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.version.Version;
import org.xwiki.instance.InstanceIdManager;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.mapping.PutMapping;
import net.sf.json.JSONObject;

/**
 * Default implementation using the Jest API to connect to a remote Elastic Search instance.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
public class DefaultPingSender implements PingSender
{
    /**
     * The version of the JSON format used to send the ping data. This will allow us to modify the data we send and
     * still have the server part be able to parse the results depending on the format used by the client side.
     */
    private static final String LATEST_FORMAT_VERSION = "1.0";

    /**
     * The elastic search index we use to index pings.
     */
    private static final String INDEX = "installs";

    /**
     * The elastic search index type we use to index pings.
     */
    private static final String TYPE = "install";

    /**
     * Formatter to format dates in a standard format.
     */
    private static final DateTimeFormatter DATE_FORMATTER = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    @Inject
    private InstalledExtensionRepository extensionRepository;

    @Inject
    private JestClientManager jestClientManager;

    @Inject
    private InstanceIdManager instanceIdManager;

    @Inject
    private DistributionManager distributionManager;

    @Override
    public void sendPing() throws Exception
    {
        JestClient client = this.jestClientManager.getClient();

        // Step 1: Create index (if already exists then it'll just be ignored)
        client.execute(new CreateIndex.Builder(INDEX).build());

        // Step 2: Create a mapping so that we can search distribution versions containing hyphens (otherwise they
        // are removed by the default tokenizer/analyzer). If mapping already exists then it'll just be ignored.
        PutMapping putMapping = new PutMapping.Builder(INDEX, TYPE, constructJSONMapping()).build();
        client.execute(putMapping);

        // Step 3: Index the data
        Index index = new Index.Builder(constructJSON())
            .index(INDEX)
            .type(TYPE)
            .id(this.instanceIdManager.getInstanceId().toString())
            .build();
        JestResult result = client.execute(index);

        if (!result.isSucceeded()) {
            throw new Exception(result.getErrorMessage());
        }
    }

    private String constructJSONMapping()
    {
        return "{ \"" + TYPE + "\" : { \"properties\" : { "
            + "\"formatVersion\" : {\"type\" : \"string\", \"index\" : \"not_analyzed\"},"
            + "\"date\" : {\"type\" : \"date\"},"
            + "\"distributionId\" : {\"type\" : \"string\", \"index\" : \"not_analyzed\"},"
            + "\"distributionVersion\" : {\"type\" : \"string\", \"index\" : \"not_analyzed\"},"
            + "\"extensions\" : { \"properties\" : {"
            + "\"id\" : {\"type\" : \"string\", \"index\" : \"not_analyzed\"},"
            + "\"version\" : {\"type\" : \"string\", \"index\" : \"not_analyzed\"}"
            + "} }"
            + "} } }";
    }

    private String constructJSON()
    {
        Map jsonMap = new HashMap();
        jsonMap.put("formatVersion", LATEST_FORMAT_VERSION);
        jsonMap.put("date", DATE_FORMATTER.print(new Date().getTime()));

        // Send distribution version if it exists (in case some distribution doesn't expose any information).
        CoreExtension distributionExtension = this.distributionManager.getDistributionExtension();
        if (distributionExtension != null) {
            String distributionId = distributionExtension.getId().getId();
            if (distributionId != null) {
                jsonMap.put("distributionId", distributionId);
            }
            Version distributionVersion = distributionExtension.getId().getVersion();
            if (distributionVersion != null) {
                jsonMap.put("distributionVersion", distributionVersion.toString());
            }
        }

        Collection<InstalledExtension> installedExtensions = this.extensionRepository.getInstalledExtensions();
        JSONObject[] extensions = new JSONObject[installedExtensions.size()];
        Iterator<InstalledExtension> it = installedExtensions.iterator();
        int i = 0;
        while (it.hasNext()) {
            InstalledExtension extension = it.next();
            Map extensionMap = new HashMap();
            extensionMap.put("id", extension.getId().getId());
            extensionMap.put("version", extension.getId().getVersion().toString());
            extensions[i] = JSONObject.fromObject(extensionMap);
            i++;
        }
        jsonMap.put("extensions", extensions);

        return JSONObject.fromObject(jsonMap).toString();
    }
}
