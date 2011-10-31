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
package com.xpn.xwiki.plugin.webdav.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.webdav.property.DavPropertySet;

import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;

public class XWikiDavUserStorage
{
    /**
     * Class version
     */
    private static final long serialVersionUID = -2125494700535339990L;
    
    /**
     * In-memory resources map for this particular user cache.
     */
    private Map<String, List<XWikiDavResource>> resourcesMap;
    
    /**
     * In-memory properties map for this particular user cache.
     */
    private Map<String, DavPropertySet> propertiesMap;
    
    /**
     * Default constructor.
     */
    public XWikiDavUserStorage(){
        resourcesMap = new HashMap<String, List<XWikiDavResource>>();
        propertiesMap = new HashMap<String, DavPropertySet>();
    }

    public Map<String, List<XWikiDavResource>> getResourcesMap()
    {
        return resourcesMap;
    }

    public Map<String, DavPropertySet> getPropertiesMap()
    {
        return propertiesMap;
    }    
}
