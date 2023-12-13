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
package org.xwiki.rendering.internal.transformation.icon;

import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconRenderer;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.syntax.Syntax;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Component to use the icon theme to provide a proper block for displaying an icon.
 *
 * @version $Id$
 * @since 15.10.2
 */
@Component
@Singleton
public class XWikiIconProvider extends DefaultIconProvider
{
    /*
    We need to update the icon class in order to take into account the situations
    where we fall back on the default behaviour (the current icon theme does not have
    the requested icon).
     */
    private static final List<Class> ICON_CLASS = new ArrayList<Class>(List.of(RawBlock.class, ImageBlock.class));

    @Inject
    private IconSetManager iconSetManager;
    @Inject
    private IconRenderer iconRenderer;

    /**
     * Uses the icon theme to provide the right block for displaying an icon.
     * @param iconName the name of the icon to display
     * @return the block containing an icon.
     */
    @Override
    public Block get(String iconName)
    {
        IconSet iconSet = null;
        try {
            iconSet = getIconSet(iconName);
            String iconContent = this.iconRenderer.renderHTML(iconName, iconSet);
            return new RawBlock(iconContent, Syntax.HTML_5_0);
        } catch (IconException e) {
            return super.get(iconName);
        }
    }

    /**
     * @return the java class of the icon block created.
     */
    public List<Class> getIconClass()
    {
        return ICON_CLASS;
    }

    private IconSet getIconSet(String iconName) throws IconException
    {
        IconSet iconSet = this.iconSetManager.getCurrentIconSet();
        if (iconSet == null || !iconSet.hasIcon(iconName)) {
            iconSet = this.iconSetManager.getDefaultIconSet();
        }
        return iconSet;
    }
}
