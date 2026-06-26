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
import org.xwiki.rendering.block.Block;
import org.xwiki.template.TemplateManager;
import org.xwiki.uiextension.UIExtension;

/**
 * Publishes the BlockNote editor as a RequireJS module.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component
@Named(BlockNoteConfigUIExtension.ID)
@Singleton
public class BlockNoteConfigUIExtension implements UIExtension
{
    /**
     * The id of the UI extension.
     */
    public static final String ID = "org.xwiki.platform.template.header.after.blockNoteConfig";

    @Inject
    private TemplateManager templates;

    @Override
    public Block execute()
    {
        return this.templates.executeNoException("blocknote/config.wiki");
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.platform.template.header.after";
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public Map<String, String> getParameters()
    {
        return Map.of();
    }
}
