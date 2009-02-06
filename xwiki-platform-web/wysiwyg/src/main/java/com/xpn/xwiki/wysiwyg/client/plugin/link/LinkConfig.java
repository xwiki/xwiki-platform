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
package com.xpn.xwiki.wysiwyg.client.plugin.link;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Stores the data about a link: information about the link: reference (wiki, space, page), URL and also about its
 * representation in a document (parameters, etc).
 * 
 * @version $Id$
 */
public class LinkConfig implements IsSerializable
{
    /**
     * The URL of this link. This can be either a relative or an absolute URL.
     */
    private String url;

    /**
     * The name of the wiki where the target document of this link is located.
     */
    private String wiki;

    /**
     * The name of the space of the target page of this link.
     */
    private String space;

    /**
     * The name of the target page of this link.
     */
    private String page;

    /**
     * @return the url
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * @return the wiki
     */
    public String getWiki()
    {
        return wiki;
    }

    /**
     * @param wiki the wiki to set
     */
    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    /**
     * @return the space
     */
    public String getSpace()
    {
        return space;
    }

    /**
     * @param space the space to set
     */
    public void setSpace(String space)
    {
        this.space = space;
    }

    /**
     * @return the page
     */
    public String getPage()
    {
        return page;
    }

    /**
     * @param page the page to set
     */
    public void setPage(String page)
    {
        this.page = page;
    }
}
