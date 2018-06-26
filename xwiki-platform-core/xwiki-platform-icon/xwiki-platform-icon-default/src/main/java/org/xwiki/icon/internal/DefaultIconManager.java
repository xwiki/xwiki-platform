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

import java.util.List;

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
 * @version $Id$
 * @since 6.2M1
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
    public String render(String iconName, String iconSetName) throws IconException
    {
        return this.render(iconName, iconSetName, true);
    }

    @Override
    public String render(String iconName, String iconSetName, boolean fallback) throws IconException
    {
        IconSet iconSet = getIconSet(iconName, iconSetName, fallback);
        if (iconSet == null) {
            return "";
        }
        return iconRenderer.render(iconName, iconSet);
    }

    @Override
    public String renderHTML(String iconName) throws IconException
    {
        return iconRenderer.renderHTML(iconName, getIconSet(iconName));
    }

    @Override
    public String renderHTML(String iconName, String iconSetName) throws IconException
    {
        return this.renderHTML(iconName, iconSetName, true);
    }

    @Override
    public String renderHTML(String iconName, String iconSetName, boolean fallback) throws IconException
    {
        IconSet iconSet = getIconSet(iconName, iconSetName, fallback);
        if (iconSet == null) {
            return "";
        }
        return iconRenderer.renderHTML(iconName, iconSet);
    }

    @Override
    public String renderCustom(String iconName) throws IconException
    {
        return iconRenderer.renderCustom(iconName, getIconSet(iconName));
    }

    @Override
    public String renderCustom(String iconName, String iconSetName) throws IconException
    {
        return this.renderCustom(iconName, iconSetName, true);
    }

    @Override
    public String renderCustom(String iconName, String iconSetName, boolean fallback) throws IconException
    {
        IconSet iconSet = getIconSet(iconName, iconSetName, fallback);
        if (iconSet == null) {
            return "";
        }
        return iconRenderer.renderCustom(iconName, iconSet);
    }

    @Override
    public List<String> getIconNames() throws IconException
    {
        return iconSetManager.getCurrentIconSet().getIconNames();
    }

    @Override
    public List<String> getIconNames(String iconSetName) throws IconException
    {
        return iconSetManager.getIconSet(iconSetName).getIconNames();
    }

    @Override
    public IconSet getIconSet(String iconName) throws IconException
    {
        return getIconSet(iconName, iconSetManager.getCurrentIconSet().getName());
    }

    @Override
    public IconSet getIconSet(String iconName, String iconSetName) throws IconException
    {
        return getIconSet(iconName, iconSetName, true);
    }

    @Override
    public IconSet getIconSet(String iconName, String iconSetName, boolean fallback) throws IconException
    {
        // Get the specified icon set
        IconSet iconSet = iconSetManager.getIconSet(iconSetName);

        // Fallback if necessary
        if (iconSet == null || iconSet.getIcon(iconName) == null) {
            if (fallback) {
                iconSet = iconSetManager.getDefaultIconSet();
            } else {
                return null;
            }
        }

        // Return the icon set
        return iconSet;
    }
}
