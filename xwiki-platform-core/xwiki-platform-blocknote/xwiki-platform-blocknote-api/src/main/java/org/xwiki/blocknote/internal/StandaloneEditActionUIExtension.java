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
package org.xwiki.blocknote.internal;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.template.TemplateManager;
import org.xwiki.uiextension.UIExtension;

/**
 * Extends the Edit menu with an entry that opens the standalone edit mode using BlockNote as the WYSIWYG editor.
 *
 * @version $Id$
 * @since 17.6.0RC1
 */
@Component
@Named(StandaloneEditActionUIExtension.ID)
@Singleton
public class StandaloneEditActionUIExtension implements UIExtension, Initializable
{
    /**
     * The id of the UI extension.
     */
    public static final String ID = "org.xwiki.platform.editactions.blocknote.standalone";

    private Map<String, String> parameters;

    @Inject
    private TemplateManager templates;

    @Override
    public void initialize() throws InitializationException
    {
        this.parameters = Map.of("separator", "true", "order", "45000");
    }

    @Override
    public Block execute()
    {
        return this.templates.executeNoException("blocknote/standaloneEditMenuItem.vm");
    }

    @Override
    public String getExtensionPointId()
    {
        // Don't fix the typo in the extension point id. It was defined like this in menus_content.vm .
        return "org.xwiki.plaftorm.editactions";
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public Map<String, String> getParameters()
    {
        return this.parameters;
    }
}
