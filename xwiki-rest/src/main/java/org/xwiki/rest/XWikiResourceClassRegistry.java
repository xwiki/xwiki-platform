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
package org.xwiki.rest;

import java.util.HashMap;
import java.util.Map;

import org.restlet.resource.Resource;

/**
 * A registry for keeping track of what resource class is attached to which URI pattern.
 * 
 * @version $Id$
 */
public class XWikiResourceClassRegistry
{
    private Map<Class< ? extends Resource>, String> resourceClassToUriPatternMap;

    public XWikiResourceClassRegistry()
    {
        resourceClassToUriPatternMap = new HashMap<Class< ? extends Resource>, String>();
    }

    public void registerResourceClass(Class< ? extends Resource> resource, String uriPattern)
    {
        resourceClassToUriPatternMap.put(resource, uriPattern);
    }

    public void removeResourceClass(Class resource)
    {
        resourceClassToUriPatternMap.remove(resource);
    }

    public String getUriPatternForResourceClass(Class< ? extends Resource> resourceClass)
    {
        return resourceClassToUriPatternMap.get(resourceClass);
    }

}
