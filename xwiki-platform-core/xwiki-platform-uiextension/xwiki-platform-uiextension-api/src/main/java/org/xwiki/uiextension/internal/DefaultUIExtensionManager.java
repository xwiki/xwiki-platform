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
package org.xwiki.uiextension.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionManager;

/**
 * Default UIExtensionManager, retrieves all the extensions for a given extension point.
 *
 * @version $Id$
 * @since 4.3.1
 */
@Component
@Singleton
public class DefaultUIExtensionManager implements UIExtensionManager
{
    private static final String FAILED_INSTANCES = "Failed to lookup UIExtension instances";

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * We use the Context Component Manager to lookup UI Extensions registered as components. The Context Component
     * Manager allows Extensions to be registered for a specific user, for a specific wiki or for a whole farm.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    private AsyncContext asyncContext;

    @Override
    public List<UIExtension> get(String extensionPointId)
    {
        List<UIExtension> extensions = new ArrayList<>();

        ComponentManager componentManager = this.contextComponentManagerProvider.get();

        // Look for a specific UI extension manager for the given extension point
        if (StringUtils.isNotEmpty(extensionPointId) && !extensionPointId.equals("default")
            && componentManager.hasComponent(UIExtensionManager.class, extensionPointId)) {
            try {
                UIExtensionManager manager = componentManager.getInstance(UIExtensionManager.class, extensionPointId);

                return manager.get(extensionPointId);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to initialize lookup a specific UIExtensionManager for the hint [{}]",
                    extensionPointId, e);

                return extensions;
            }
        }

        // Fallback on the default behavior
        try {
            List<UIExtension> allExtensions = componentManager.getInstanceList(UIExtension.class);
            for (UIExtension extension : allExtensions) {
                if (StringUtils.equals(extension.getExtensionPointId(), extensionPointId)) {
                    extensions.add(extension);
                }
            }

            // Indicate that any currently running asynchronous execution result should be removed from the cache as
            // soon as a UIExtension component is modified
            this.asyncContext.useComponent(UIExtension.class);
        } catch (ComponentLookupException e) {
            this.logger.error(FAILED_INSTANCES, e);
        }

        return extensions;
    }

    @Override
    public Optional<UIExtension> getUIExtension(String id)
    {
        ComponentManager componentManager = this.contextComponentManagerProvider.get();

        try {
            List<UIExtension> allExtensions = componentManager.getInstanceList(UIExtension.class);
            for (UIExtension extension : allExtensions) {
                if (id.equals(extension.getId())) {
                    return Optional.of(extension);
                }
            }
        } catch (ComponentLookupException e) {
            this.logger.error(FAILED_INSTANCES, e);
        }

        return Optional.empty();
    }
}
