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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.version.Version;
import org.xwiki.instance.InstanceIdManager;

import co.elastic.clients.elasticsearch._types.mapping.Property;

/**
 * Provide the instance's unique id and distribution id and version.
 *
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named("distribution")
@Singleton
public class DistributionPingDataProvider extends AbstractPingDataProvider
{
    static final String PROPERTY_INSTANCE_ID = "instanceId";

    static final String PROPERTY_DISTRIBUTION = "distribution";

    private static final String PROPERTY_DISTRIBUTION_VERSION = "version";

    private static final String PROPERTY_DISTRIBUTION_ID = "id";

    private static final String PROPERTY_DISTRIBUTION_FEATURES = "features";

    private static final String PROPERTY_EXTENSION = "extension";

    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    /**
     * Note that we use a Provider to make sure that the Instance Manager is initialized as late as possible (it
     * requires the database to be ready for its initialization).
     */
    @Inject
    private Provider<InstanceIdManager> instanceIdManagerProvider;

    @Override
    public Map<String, Property> provideMapping()
    {
        Map<String, Property> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_INSTANCE_ID, Property.of(b1 -> b1.keyword(b2 -> b2)));

        Map<String, Property> extensionMap = new HashMap<>();
        extensionMap.put(PROPERTY_DISTRIBUTION_VERSION, Property.of(b1 -> b1.keyword(b2 -> b2)));
        extensionMap.put(PROPERTY_DISTRIBUTION_ID, Property.of(b1 -> b1.keyword(b2 -> b2)));
        // Thanks to the mapping as a text string, ES will convert Collection<ExtensionId> into a comma-delimited
        // string.
        extensionMap.put(PROPERTY_DISTRIBUTION_FEATURES, Property.of(b1 -> b1.keyword(b2 -> b2)));
        propertiesMap.put(PROPERTY_EXTENSION, Property.of(b0 -> b0.object(b1 -> b1.properties(extensionMap))));

        return Collections.singletonMap(PROPERTY_DISTRIBUTION, Property.of(b0 -> b0.object(b1 ->
            b1.properties(propertiesMap))));
    }

    @Override
    public void provideData(Ping ping)
    {
        DistributionPing distributionPing = new DistributionPing();
        String instanceId = this.instanceIdManagerProvider.get().getInstanceId().toString();
        distributionPing.setInstanceId(instanceId);

        CoreExtension distributionExtension = this.coreExtensionRepository.getEnvironmentExtension();
        if (distributionExtension != null) {
            ExtensionPing extensionPing = new ExtensionPing();
            String distributionId = distributionExtension.getId().getId();
            if (distributionId != null) {
                extensionPing.setId(distributionId);
            }
            Version distributionVersion = distributionExtension.getId().getVersion();
            if (distributionVersion != null) {
                extensionPing.setVersion(distributionVersion.toString());
            }
            Collection<ExtensionId> features = distributionExtension.getExtensionFeatures();
            if (!features.isEmpty()) {
                // Convert ExtensionId to String since we don't want to index all the different fields of the
                // ExtensionId class, and we want to make it easy to query later on.
                extensionPing.setFeatures(features.stream()
                    .map(ExtensionId::toString)
                    .toList());
            }
            distributionPing.setExtension(extensionPing);
        }
        ping.setDistribution(distributionPing);
    }
}
