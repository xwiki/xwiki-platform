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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.edit.EditConfiguration;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorConfiguration;
import org.xwiki.rendering.block.XDOM;

/**
 * Configures the default {@link XDOM} {@link Editor}. It overwrites the default {@link EditConfiguration} in order to
 * preserve backward compatibility (because it looks for configuration properties that existed before the edit module
 * was written).
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Singleton
public class XDOMEditorConfiguration implements EditorConfiguration<XDOM>
{
    @Inject
    @Named("user")
    private ConfigurationSource userConfig;

    @Inject
    @Named("documents")
    private ConfigurationSource documentsConfig;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikiConfig;

    @Override
    public String getDefaultEditor()
    {
        String propertyName = "editor";
        String defaultEditor = this.userConfig.getProperty(propertyName, String.class);
        if (StringUtils.isEmpty(defaultEditor)) {
            defaultEditor = this.documentsConfig.getProperty(propertyName, String.class);
            if (StringUtils.isEmpty(defaultEditor)) {
                defaultEditor = this.xwikiConfig.getProperty("xwiki.editor", String.class);
            }
        }
        return defaultEditor;
    }

    @Override
    public String getDefaultEditor(String category)
    {
        // Fall-back on the default edit configuration.
        return null;
    }
}
