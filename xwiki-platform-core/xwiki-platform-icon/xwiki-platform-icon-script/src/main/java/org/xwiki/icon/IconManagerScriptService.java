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
package org.xwiki.icon;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

/**
 * Script services to render an icon from the current icon set.
 *
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Named("icon")
@Singleton
public class IconManagerScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String ERROR_KEY = "scriptservice.icon.error";

    @Inject
    private IconManager iconManager;

    @Inject
    private IconSetManager iconSetManager;

    @Inject
    private IconRenderer iconRenderer;

    @Inject
    private Execution execution;

    /**
     * Display an icon with wiki code from the current {@link org.xwiki.icon.IconSet}.
     *
     * @param iconName name of the icon to display
     * @return the wiki code that displays the icon
     */
    public String render(String iconName)
    {
        try {
            return iconManager.render(iconName);
        } catch (IconException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Display an icon with wiki code from the specified {@link org.xwiki.icon.IconSet}.
     *
     * @param iconName name of the icon to display
     * @param iconSetName name of the icon set
     * @return the wiki code that displays the icon
     * @since 6.3RC1
     */
    public String render(String iconName, String iconSetName)
    {
        try {
            return iconManager.render(iconName, iconSetName);
        } catch (IconException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Display an icon with wiki code from the specified {@link org.xwiki.icon.IconSet}.
     *
     * @param iconName name of the icon to display
     * @param iconSetName name of the icon set
     * @param fallback enable the fallback to the default icon theme if the icon does not exist
     * @return the wiki code that displays the icon
     * @since 6.3RC1
     */
    public String render(String iconName, String iconSetName, boolean fallback)
    {
        try {
            return iconManager.render(iconName, iconSetName, fallback);
        } catch (IconException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Display an icon with HTML code from the current {@link org.xwiki.icon.IconSet}.
     *
     * @param iconName name of the icon to display
     * @return the HTML code that displays the icon
     */
    public String renderHTML(String iconName)
    {
        try {
            return iconManager.renderHTML(iconName);
        } catch (IconException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Display an icon with HTML code from the specified {@link org.xwiki.icon.IconSet}.
     *
     * @param iconName name of the icon to display
     * @param iconSetName name of the icon set
     * @return the HTML code that displays the icon
     * @since 6.3RC1
     */
    public String renderHTML(String iconName, String iconSetName)
    {
        try {
            return iconManager.renderHTML(iconName, iconSetName);
        } catch (IconException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Display an icon from the specified {@link org.xwiki.icon.IconSet}.
     *
     * @param iconName name of the icon to display
     * @param iconSetName name of the icon set
     * @param fallback enable the fallback to the default icon theme if the icon does not exist
     * @return the HTML code that displays the icon
     * @since 6.3RC1
     */
    public String renderHTML(String iconName, String iconSetName, boolean fallback)
    {
        try {
            return iconManager.renderHTML(iconName, iconSetName, fallback);
        } catch (IconException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Display an icon with custom code from the current {@link org.xwiki.icon.IconSet}.
     *
     * @param iconName name of the icon to display
     * @return the custom code that displays the icon
     * @since 10.6RC1
     */
    public String renderCustom(String iconName)
    {
        try {
            return iconManager.renderCustom(iconName);
        } catch (IconException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Display an icon with custom code from the specified {@link org.xwiki.icon.IconSet}.
     *
     * @param iconName name of the icon to display
     * @param iconSetName name of the icon set
     * @return the custom code that displays the icon
     * @since 10.6RC1
     */
    public String renderCustom(String iconName, String iconSetName)
    {
        try {
            return iconManager.renderCustom(iconName, iconSetName);
        } catch (IconException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Display an icon with custom code from the specified {@link org.xwiki.icon.IconSet}.
     *
     * @param iconName name of the icon to display
     * @param iconSetName name of the icon set
     * @param fallback enable the fallback to the default icon theme if the icon does not exist
     * @return the custom code that displays the icon
     * @since 10.6RC1
     */
    public String renderCustom(String iconName, String iconSetName, boolean fallback)
    {
        try {
            return iconManager.renderCustom(iconName, iconSetName, fallback);
        } catch (IconException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Pull the necessary resources to use the default icon set.
     *
     * @since 10.6RC1
     */
    public void use()
    {
        try {
            IconSet iconSet = iconSetManager.getDefaultIconSet();
            use(iconSet.getName());
        } catch (IconException e) {
            setLastError(e);
        }
    }

    /**
     * Pull the necessary resources to use the specified icon set.
     *
     * @param iconSetName name of the icon set
     * @since 10.6RC1
     */
    public void use(String iconSetName)
    {
        try {
            IconSet iconSet = iconSetManager.getIconSet(iconSetName);
            if (iconSet == null) {
                iconSet = iconSetManager.getDefaultIconSet();
            }
            iconRenderer.use(iconSet);
        } catch (IconException e) {
            setLastError(e);
        }
    }

    /**
     * Get the name of all the icon sets present in the current wiki.
     *
     * @return the list of the name of the icon sets present in the current wiki.
     * @since 6.4M1
     */
    public List<String> getIconSetNames()
    {
        try {
            return iconSetManager.getIconSetNames();
        } catch (IconException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Get the list of the names of all available icons in the current icon set.
     *
     * @return the icon names
     * @since 6.4M1
     */
    public List<String> getIconNames()
    {
        try {
            return iconManager.getIconNames();
        } catch (IconException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Get the list of the names of all available icons in the specified icon set.
     *
     * @param iconSetName name of the icon set
     * @return the icon names
     * @since 6.4M1
     */
    public List<String> getIconNames(String iconSetName)
    {
        try {
            return iconManager.getIconNames(iconSetName);
        } catch (IconException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Get the name of the current icon set.
     *
     * @return the name of the current icon set
     * @since 6.4M2
     */
    public String getCurrentIconSetName()
    {
        try {
            IconSet currentIconSet = iconSetManager.getCurrentIconSet();
            if (currentIconSet != null) {
                return currentIconSet.getName();
            }
        } catch (IconException e) {
            setLastError(e);
        }

        return null;
    }

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return an eventual exception or {@code null} if no exception was thrown
     * @since 6.3RC1
     */
    public IconException getLastError()
    {
        return (IconException) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     * @since 6.3RC1
     */
    private void setLastError(IconException e)
    {
        this.execution.getContext().setProperty(ERROR_KEY, e);
    }
}
