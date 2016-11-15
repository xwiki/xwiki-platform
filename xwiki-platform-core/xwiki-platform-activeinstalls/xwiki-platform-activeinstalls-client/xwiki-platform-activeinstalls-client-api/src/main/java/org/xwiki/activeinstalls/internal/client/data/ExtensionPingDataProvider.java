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
package org.xwiki.activeinstalls.internal.client.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.activeinstalls.internal.client.PingDataProvider;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;

import net.sf.json.JSONObject;

/**
 * Provide the list of installed extensions and their versions.
 *
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named("extensions")
@Singleton
public class ExtensionPingDataProvider implements PingDataProvider
{
    private static final String PROPERTY_ID = "id";

    private static final String PROPERTY_VERSION = "version";

    private static final String PROPERTY_FEATURES = "features";

    private static final String PROPERTY_EXTENSIONS = "extensions";

    @Inject
    private InstalledExtensionRepository extensionRepository;

    @Override
    public Map<String, Object> provideMapping()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "string");
        map.put("index", "not_analyzed");

        Map<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_ID, map);
        propertiesMap.put(PROPERTY_VERSION, map);
        propertiesMap.put(PROPERTY_FEATURES, map);

        return Collections.singletonMap(PROPERTY_EXTENSIONS,
            (Object) Collections.singletonMap("properties", propertiesMap));
    }

    @Override
    public Map<String, Object> provideData()
    {
        Collection<InstalledExtension> installedExtensions = this.extensionRepository.getInstalledExtensions();
        JSONObject[] extensions = new JSONObject[installedExtensions.size()];
        Iterator<InstalledExtension> it = installedExtensions.iterator();
        int i = 0;
        while (it.hasNext()) {
            InstalledExtension extension = it.next();
            Map<String, Object> extensionMap = new HashMap<>();
            extensionMap.put(PROPERTY_ID, extension.getId().getId());
            extensionMap.put(PROPERTY_VERSION, extension.getId().getVersion().toString());
            extensionMap.put(PROPERTY_FEATURES, extension.getFeatures().toArray());
            extensions[i] = JSONObject.fromObject(extensionMap);
            i++;
        }
        return Collections.singletonMap(PROPERTY_EXTENSIONS, (Object) extensions);
    }
}
