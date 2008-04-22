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
 *
 */
package org.xwiki.xmlrpc.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A class representing XWiki objects. An XWiki object contains the association between the
 * properties defined in the object's XWikiClass and their corresponding values.
 * 
 * @author fmancinelli
 */

public class XWikiObject extends XWikiObjectSummary
{
    public XWikiObject()
    {
        super();
        setMap("propertyToValueMap", new HashMap<String, Object>());
    }

    public XWikiObject(Map map)
    {
        super(map);
        if (getMap("propertyToValueMap") == null) {
            setMap("propertyToValueMap", new HashMap<String, Object>());
        }
    }

    public void setPropertyToValueMap(Map propertyToValueMap)
    {
        setMap("propertyToValueMap", propertyToValueMap);
    }

    public Object getProperty(String propertyName)
    {
        Map propertyToValueMap = getMap("propertyToValueMap");
        Object value = propertyToValueMap.get(propertyName);
        
        if (value != null) {
            /* Convert arrays to list for easier management */
            if (value.getClass().isArray()) {
                ArrayList array = new ArrayList();
                for (Object o : (Object[]) value) {
                    array.add(o);
                }
                value = array;
                propertyToValueMap.put(propertyName, value);
            }
        }
        
        return value; 
    }

    public void setProperty(String propertyName, Object value)
    {
        Map propertyToValueMap = getMap("propertyToValueMap");
        propertyToValueMap.put(propertyName, value);
    }

    public Set<String> getProperties()
    {
        Map propertToValueMap = getMap("propertyToValueMap");
        return propertToValueMap.keySet();
    }
}
