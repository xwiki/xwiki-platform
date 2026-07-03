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
package org.xwiki.activeinstalls2.internal.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;

import co.elastic.clients.elasticsearch._types.mapping.Property;

/**
 * Provide the list of installed extensions and their versions.
 *
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named("extensions")
@Singleton
public class ExtensionPingDataProvider extends AbstractPingDataProvider
{
    private static final String PROPERTY_ID = "id";

    private static final String PROPERTY_VERSION = "version";

    private static final String PROPERTY_FEATURES = "features";

    private static final String PROPERTY_EXTENSIONS = "extensions";

    @Inject
    private InstalledExtensionRepository extensionRepository;

    @Override
    public Map<String, Property> provideMapping()
    {
        Map<String, Property> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_ID, Property.of(b1 -> b1.keyword(b2 -> b2)));
        propertiesMap.put(PROPERTY_VERSION, Property.of(b1 -> b1.keyword(b2 -> b2)));
        propertiesMap.put(PROPERTY_FEATURES, Property.of(b1 -> b1.keyword(b2 -> b2)));

        // Now we need a collection of the above, and we want the id/version/features to be indexed together, and thus
        // we cannot use an "object" type. Instead we need a "nested" type
        // (see https://www.elastic.co/guide/en/elasticsearch/reference/current/nested.html).
        return Collections.singletonMap(PROPERTY_EXTENSIONS, Property.of(b0 -> b0.nested(b1 ->
            b1.properties(propertiesMap))));
    }

    @Override
    public void provideData(Ping ping)
    {
        Collection<ExtensionPing> pingExtensions = new ArrayList<>();
        Collection<InstalledExtension> installedExtensions = this.extensionRepository.getInstalledExtensions();
        for (InstalledExtension extension : installedExtensions) {
            ExtensionPing extensionPing = new ExtensionPing();
            extensionPing.setId(extension.getId().getId());
            extensionPing.setVersion(extension.getId().getVersion().toString());
            extensionPing.setFeatures(extension.getExtensionFeatures().stream()
                .map(ExtensionId::toString)
                .toList());
            pingExtensions.add(extensionPing);
        }
        ping.setExtensions(pingExtensions);
    }
}
