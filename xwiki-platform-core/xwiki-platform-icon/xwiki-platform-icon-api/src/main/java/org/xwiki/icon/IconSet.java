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

import java.util.HashMap;
import java.util.Map;

public class IconSet
{
    private String name;

    private Map<String, Icon> iconMap = new HashMap<>();

    private String css;

    private String ssx;

    private String renderWiki;

    private String renderHTML;

    private IconType type;

    public IconSet(String name)
    {
        this.name = name;
    }

    public Icon getIcon(String name)
    {
        return iconMap.get(name);
    }

    public void addIcon(Icon icon)
    {
        iconMap.put(icon.getName(), icon);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getCss()
    {
        return css;
    }

    public void setCss(String css)
    {
        this.css = css;
    }

    public String getSsx()
    {
        return ssx;
    }

    public void setSsx(String ssx)
    {
        this.ssx = ssx;
    }

    public String getRenderWiki()
    {
        return renderWiki;
    }

    public void setRenderWiki(String renderWiki)
    {
        this.renderWiki = renderWiki;
    }

    public String getRenderHTML()
    {
        return renderHTML;
    }

    public void setRenderHTML(String renderHTML)
    {
        this.renderHTML = renderHTML;
    }

    public IconType getType()
    {
        return type;
    }

    public void setType(IconType type)
    {
        this.type = type;
    }
}
