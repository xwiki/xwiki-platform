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
package org.xwiki.icon.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconManager;
import org.xwiki.icon.IconRenderer;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;

/**
 * Default implementation of {@link org.xwiki.icon.IconManager}.
 *
 * @since 6.2M1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultIconManager implements IconManager
{
    @Inject
    private IconSetManager iconSetManager;

    @Inject
    private IconRenderer iconRenderer;

    @Override
    public String render(String iconName) throws IconException
    {
        return iconRenderer.render(iconName, getIconSet(iconName));
    }

    @Override
    public String renderHTML(String iconName) throws IconException
    {

        return iconRenderer.renderHTML(iconName, getIconSet(iconName));
    }

    private IconSet getIconSet(String iconName) throws IconException
    {
        // First: try to render with the current icon set
        IconSet currentIconSet = iconSetManager.getCurrentIconSet();
        if (currentIconSet != null && currentIconSet.getIcon(iconName) != null) {
            return currentIconSet;
        } else {
            return iconSetManager.getDefaultIconSet();
        }
    }
}
