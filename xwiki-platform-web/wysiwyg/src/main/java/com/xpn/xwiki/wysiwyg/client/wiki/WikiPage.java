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
package com.xpn.xwiki.wysiwyg.client.wiki;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Stores information about a wiki page.
 * 
 * @version $Id$
 */
public class WikiPage implements IsSerializable
{
    /**
     * The page name.
     */
    private String name;

    /**
     * The page title.
     */
    private String title;

    /**
     * The page URL.
     */
    private String url;

    /**
     * @return the page name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the page name.
     * 
     * @param name the new page name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the page title
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Sets the page title.
     * 
     * @param title the new page title
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * @return the page URL
     */
    public String getURL()
    {
        return url;
    }

    /**
     * Sets the page URL.
     * 
     * @param url the new page URL
     */
    public void setURL(String url)
    {
        this.url = url;
    }
}
