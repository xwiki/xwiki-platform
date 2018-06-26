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
package org.xwiki.panels.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionManager;

/**
 * Abstract panels UI extension manager. Implementations must provide a list of panel IDs to be displayed, this class
 * handles the retrieval of the UI extensions corresponding to the configured list.
 *
 * @version $Id$
 * @since 4.3.1
 */
public abstract class AbstractPanelsUIExtensionManager implements UIExtensionManager
{
    /**
     * The default configuration source.
     */
    @Inject
    protected ConfigurationSource configurationSource;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Resolver allowing to retrieve reference from the panels configuration.
     */
    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> resolver;

    /**
     * Serializer allowing to serialize references into their absolute representation.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * We use the Context Component Manager to lookup UI Extensions registered as components.
     * The Context Component Manager allows Extensions to be registered for a specific user, for a specific wiki or for
     * a whole farm.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    /**
     * Method returning the list of configured panels.
     *
     * @return a comma separated list of panel IDs
     */
    protected abstract String getConfiguration();

    @Override
    public List<UIExtension> get(String extensionPointId)
    {
        List<UIExtension> panels = new ArrayList<UIExtension>();

        String panelConfigurationString = getConfiguration();

        // Verify that there's a panel configuration property defined, and if not don't return any panel extension.
        if (!StringUtils.isEmpty(panelConfigurationString)) {
            List<String> panelSerializedReferences = new ArrayList<String>();
            for (String serializedReference : getConfiguration().split(",")) {
                panelSerializedReferences.add(serializer.serialize(resolver.resolve(serializedReference.trim())));
            }

            try {
                List<UIExtension> allExtensions =
                    contextComponentManagerProvider.get().getInstanceList(UIExtension.class);
                for (String panelSerializedReference : panelSerializedReferences) {
                    // TODO: This is not performant and will not scale well when the number of UIExtension instances
                    // increase in the wiki
                    for (UIExtension extension : allExtensions) {
                        // Resolve and serialize the extension id to make it absolute before we can compare it with
                        // the serialized panel reference.
                        String extensionId = serializer.serialize(resolver.resolve(extension.getId()));
                        if (extensionId.equals(panelSerializedReference)) {
                            panels.add(extension);
                        }
                    }
                }
            } catch (ComponentLookupException e) {
                logger.error("Failed to lookup Panels instances, error: [{}]", e);
            }
        }

        return panels;
    }
}
