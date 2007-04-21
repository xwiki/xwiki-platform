/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.xmlrpc;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the details of a Space (SpaceSummary) as described in the <a href="Confluence
 * specification"> http://confluence.atlassian.com/display/DOC/Remote+API+Specification</a>.
 * 
 * @todo We're missing the Space Type field as described in the Confluence specification.
 * @version $Id: $
 */
public class SpaceSummary
{
    /**
     * @see #getKey()
     */
    private static final String KEY = "key";

    /**
     * @see #getName()
     */
    private static final String NAME = "name";

    /**
     * @see #getUrl()
     */
    private static final String URL = "url";

    /**
     * @see #getKey()
     */
    private String key;

    /**
     * @see #getName()
     */
    private String name;

    /**
     * @see #getUrl()
     */
    private String url;

    /**
     * @param key of the space (usually the Space's name as it's unique)
     * @param name of the space
     * @param url to view the space online. Example: "http://server/xwiki/bin/view/Space/WebHome"
     */
    public SpaceSummary(String key, String name, String url)
    {
        this.setKey(key);
        this.setName(name);
        this.setUrl(url);
    }

    /**
     * @param spaceSummaryProperties te Map containing all informations to setup a SpaceSummary
     *            object. More specifically in the map there <b>must</b> be the following keys:
     *            <ul>
     *            <li>"key": key of the SpaceSummary</li>
     *            <li>"name": name of the SpaceSummary</li>
     *            <li>"url": url to view the space online</li>
     *            </ul>
     * @see #SpaceSummary(String, String, String)
     */
    public SpaceSummary(Map spaceSummaryProperties)
    {
        this((String) spaceSummaryProperties.get(KEY), (String) spaceSummaryProperties.get(NAME),
            (String) spaceSummaryProperties.get(URL));
    }

    /**
     * @return the SpaceSummary object represented by a Map. The Map keys are the XML-RPC ids and
     *         the values are the property values. This map will be used to build a XML-RPC message.
     */
    Map getParameters()
    {
        Map params = new HashMap();
        params.put(KEY, getKey());
        params.put(NAME, getName());
        params.put(URL, getUrl());
        return params;
    }

    /**
     * @return the key of the space (usually the Space's name as it's unique)
     */
    public String getKey()
    {
        return key;
    }

    /**
     * @param key the key of the space
     * @see #getKey()
     */
    public void setKey(String key)
    {
        this.key = key;
    }

    /**
     * @return the name of the space
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name the name of the space
     * @see #getName()
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the url to view this space online. Example:
     *         "http://server/xwiki/bin/view/Space/WebHome"
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param url the url url to view this space online
     * @see #getUrl()
     */
    public void setUrl(String url)
    {
        this.url = url;
    }
}
