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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
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
public class DefaultUIExtensionManager implements UIExtensionManager
{
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

        try {
            List<UIExtension> allExtensions = contextComponentManagerProvider.get().getInstanceList(UIExtension.class);
            for (UIExtension extension : allExtensions) {
                if (extension.getExtensionPointId().equals(extensionPointId)) {
                    extensions.add(extension);
                }
            }

            // Indicate that any currently running asynchronous execution result should be removed from the cache as
            // soon as a UIExtension component is modified
            this.asyncContext.useComponent(UIExtension.class);
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to lookup UIExtension instances, error: [{}]", e);
        }

        return extensions;
    }
}
