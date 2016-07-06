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
package org.xwiki.edit.internal;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.edit.EditConfiguration;
import org.xwiki.edit.EditorConfiguration;

/**
 * Default {@link EditConfiguration} implementation.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Singleton
public class DefaultEditConfiguration implements EditConfiguration
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named("editorBindings/all")
    private ConfigurationSource allEditorBindingsSource;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @Override
    public String getDefaultEditor(Type dataType)
    {
        String defaultEditor = null;
        EditorConfiguration<?> customConfig = getCustomConfiguration(dataType);
        if (customConfig != null) {
            defaultEditor = customConfig.getDefaultEditor();
        }
        if (StringUtils.isEmpty(defaultEditor)) {
            defaultEditor = getDefaultEditor(dataType.getTypeName());
        }
        return defaultEditor;
    }

    @Override
    public String getDefaultEditor(Type dataType, String category)
    {
        String defaultEditor = null;
        EditorConfiguration<?> customConfig = this.getCustomConfiguration(dataType);
        if (customConfig != null) {
            defaultEditor = customConfig.getDefaultEditor(category);
        }
        if (StringUtils.isEmpty(defaultEditor)) {
            defaultEditor = getDefaultEditor(String.format("%s#%s", dataType.getTypeName(), category));
        }
        return defaultEditor;
    }

    private <D extends Type> EditorConfiguration<D> getCustomConfiguration(D dataType)
    {
        DefaultParameterizedType customConfigType =
            new DefaultParameterizedType(null, EditorConfiguration.class, dataType);
        try {
            return this.componentManagerProvider.get().getInstance(customConfigType);
        } catch (ComponentLookupException e) {
            return null;
        }
    }

    private String getDefaultEditor(String dataType)
    {
        String defaultEditor = this.allEditorBindingsSource.getProperty(dataType, String.class);
        if (StringUtils.isEmpty(defaultEditor)) {
            defaultEditor = this.xwikiPropertiesSource.getProperty("edit.defaultEditor." + dataType, String.class);
        }
        return defaultEditor;
    }
}
