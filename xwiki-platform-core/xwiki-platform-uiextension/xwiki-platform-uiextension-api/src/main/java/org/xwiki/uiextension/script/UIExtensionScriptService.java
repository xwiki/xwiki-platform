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
package org.xwiki.uiextension.script;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

/**
 * Allows scripts to easily access Interface Extensions APIs.
 *
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("uix")
@Singleton
public class UIExtensionScriptService implements ScriptService
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * We use the Context Component Manager to lookup UI Extensions registered as components.
     * The Context Component Manager allows Extensions to be registered for a specific user, for a specific wiki or for
     * a whole farm.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    /**
     * The default UIExtensionManager.
     */
    @Inject
    private UIExtensionManager uiExtensionManager;
    
    /**
     * Utility method to split a list of extension names, for example {code}"Panels.Apps,Panels.QuickLinks"{code} to get
     * a List containing those names.
     *
     * @param nameList the list of extension names to split
     * @return a List containing all the names from the given String.
     */
    private String[] parseFilterParameters(String nameList)
    {
        return nameList.replaceAll(" ", "").split(",");
    }

    /**
     * Retrieves all the {@link UIExtension}s for a given Extension Point.
     *
     * @param extensionPointId The ID of the Extension Point to retrieve the {@link UIExtension}s for
     * @return the list of {@link UIExtension} for the given Extension Point
     */
    public List<UIExtension> getExtensions(String extensionPointId)
    {
        return this.uiExtensionManager.get(extensionPointId);
    }

    /**
     * Retrieves the list of {@link UIExtension} for a given Extension Point.
     *
     * Examples:
     * <ul>
     * <li>Get only the {@link UIExtension}s with the given IDs for the Extension Point "platform.example"
     * <pre>$services.uix.getExtensions('platform.example', {'select' : 'id1, id2, id3'})</pre></li>
     * <li>Get all the {@link UIExtension}s for the Extension Point "platform.example" except the
     * {@link UIExtension}s with the IDs "id2" and "id3"
     * <pre>$services.uix.getExtensions('platform.example', {'exclude' : 'id2, id3'})</pre></li>
     * <li>Get all the {@link UIExtension}s for the Extension Point "platform.example" and order them by one of their
     * parameter
     * <pre>$services.uix.getExtensions('platform.example', {'sortByParameter' : 'parameterKey'})</pre></li>
     * <li>Get only the {@link UIExtension}s with the given IDs for the Extension Point "platform.example" and order
     * them by one of their parameter
     * <pre>$services.uix.getExtensions('platform.example',
     * {'select' : 'id1, id2, id3', 'sortByParameter' : 'parameterKey'})</pre></li>
     * </ul>
     *
     * @param extensionPointId The ID of the Extension Point to retrieve the {@link UIExtension}s for
     * @param filters Optional filters to apply before retrieving the list
     * @return the list of {@link UIExtension} for the given Extension Point
     */
    public List<UIExtension> getExtensions(String extensionPointId, Map<String, String> filters)
    {
        List<UIExtension> extensions = getExtensions(extensionPointId);

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String filterHint = entry.getKey();

            try {
                UIExtensionFilter filter =
                    contextComponentManagerProvider.get().getInstance(UIExtensionFilter.class, filterHint);
                extensions = filter.filter(extensions, this.parseFilterParameters(entry.getValue()));
            } catch (ComponentLookupException e) {
                logger.warn("Unable to find a UIExtensionFilter for hint [{}] "
                    + "while getting UIExtensions for extension point [{}]", filterHint, extensionPointId);
            }
        }

        return extensions;
    }
}
