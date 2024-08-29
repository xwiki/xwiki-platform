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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * A collection of icons, with some properties to display them.
 *
 * @version $Id$
 * @since 6.2M1
 */
public class IconSet
{
    private String name;

    private Map<String, Icon> iconMap = new HashMap<>();

    private String css;

    private String ssx;

    private String jsx;

    private String renderWiki;

    private String renderHTML;

    private String iconUrl;

    private String iconCssClass;

    private IconType type;

    private DocumentReference sourceDocumentReference;

    /**
     * Constructor.
     *
     * @param name name of the icon set
     */
    public IconSet(String name)
    {
        this.name = name;
    }

    /**
     * @param name name of the icon to get
     * @return the icon corresponding to the given name
     */
    public Icon getIcon(String name)
    {
        return iconMap.get(name);
    }

    /**
     * Add an icon to the icon set.
     *
     * @param name name of the icon
     * @param icon the icon to add
     */
    public void addIcon(String name, Icon icon)
    {
        iconMap.put(name, icon);
    }

    /**
     * @return the name of the icon set
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the name of the icon set.
     *
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the URL of a CSS file to enable to display this icon set properly, or null if it is not necessary
     */
    public String getCss()
    {
        return css;
    }

    /**
     * Set the URL of a CSS file to enable to display this icon set properly.
     *
     * @param css URL of the CSS file (it can contains velocity code).
     */
    public void setCss(String css)
    {
        this.css = css;
    }

    /**
     * @return the name of a SSX document to enable to display this icon set properly, or null if it is not necessary
     */
    public String getSsx()
    {
        return ssx;
    }

    /**
     * Set the page name of a SSX document to enable to display the icon set properly.
     *
     * @param ssx the SSX document name
     */
    public void setSsx(String ssx)
    {
        this.ssx = ssx;
    }

    /**
     * @return the name of a JSX document to enable to display this icon set properly, or null if it is not necessary
     */
    public String getJsx()
    {
        return jsx;
    }

    /**
     * Set the page name of a JSX document to enable to display the icon set properly.
     *
     * @param jsx the JSX document name
     */
    public void setJsx(String jsx)
    {
        this.jsx = jsx;
    }

    /**
     * @return the wiki code (containing velocity), to display an icon from this set.
     */
    public String getRenderWiki()
    {
        return renderWiki;
    }

    /**
     * Set the wiki code (containing velocity) to display an icon from this set.
     *
     * @param renderWiki wiki code to set
     */
    public void setRenderWiki(String renderWiki)
    {
        this.renderWiki = renderWiki;
    }

    /**
     * @return the HTML code (containing velocity) to display an icon from this set
     */
    public String getRenderHTML()
    {
        return renderHTML;
    }

    /**
     * Set the HTML code (containing velocity) to display an icon from this set.
     *
     * @param renderHTML the HTML code to set
     */
    public void setRenderHTML(String renderHTML)
    {
        this.renderHTML = renderHTML;
    }

    /**
     * @return the icon url
     * @since 10.6RC1
     */
    public String getIconUrl()
    {
        return iconUrl;
    }

    /**
     * Set the url of the icon.
     *
     * @param iconUrl the icon url
     * @since 10.6RC1
     */
    public void setIconUrl(String iconUrl)
    {
        this.iconUrl = iconUrl;
    }

    /**
     * @return the icon css class
     * @since 10.6RC1
     */
    public String getIconCssClass()
    {
        return iconCssClass;
    }

    /**
     * Set the css class of the icon.
     *
     * @param iconCssClass the icon css class
     * @since 10.6RC1
     */
    public void setIconCssClass(String iconCssClass)
    {
        this.iconCssClass = iconCssClass;
    }

    /**
     * @return the type of icons that contains this set
     */
    public IconType getType()
    {
        return type;
    }

    /**
     * Set the type of icons that contains this set.
     *
     * @param type type to set
     */
    public void setType(IconType type)
    {
        this.type = type;
    }

    /**
     * @return the list of names of all icons
     * @since 6.4M1
     */
    public List<String> getIconNames()
    {
        return new ArrayList<>(iconMap.keySet());
    }

    /**
     * Checks if the provided icon name exists in the icon set.
     *
     * @param iconName an icon name (for instance, {@code add})
     * @return {@code true} if the icon name exists in the icon set, {@code false} otherwise
     * @since 13.4RC1
     */
    public boolean hasIcon(String iconName)
    {
        return this.iconMap.containsKey(iconName);
    }

    /**
     * @return the document reference of the source of the icon theme, may be {@code null} if the icon set hasn't
     * been loaded from a document
     * @since 14.10.6
     * @since 15.2RC1
     */
    @Unstable
    public DocumentReference getSourceDocumentReference()
    {
        return this.sourceDocumentReference;
    }

    /**
     * @param sourceDocumentReference the reference to the source of the icon theme
     * @since 14.10.6
     * @since 15.2RC1
     */
    @Unstable
    public void setSourceDocumentReference(DocumentReference sourceDocumentReference)
    {
        this.sourceDocumentReference = sourceDocumentReference;
    }
}
