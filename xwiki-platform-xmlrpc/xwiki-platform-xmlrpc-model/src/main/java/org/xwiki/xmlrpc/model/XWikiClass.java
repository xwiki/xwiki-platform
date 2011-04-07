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

import java.util.Map;
import java.util.Set;

/**
 * A class representing XWiki classes. An XWikiClass is basically a set of properties where each property can have
 * different attributes. For example the standard XWiki.TagClass has one property 'tags', and this property has several
 * attributes such as 'prettyName', 'tooltip', etc.
 * 
 * @version $Id$
 */
public class XWikiClass extends XWikiClassSummary
{
    public static final String XWIKICLASS_ATTRIBUTE = "xwikiclass";

    public XWikiClass()
    {
        super();
    }

    public XWikiClass(Map map)
    {
        super(map);
    }

    public void setPropertyToAttributesMap(Map<String, Map<String, Object>> map)
    {
        setMap("propertyToAttributesMap", map);
    }

    public Set<String> getPropertyAttributes(String propertyName)
    {
        Map<String, Map<String, Object>> propertyToAttributesMap = getMap("propertyToAttributesMap");
        Map<String, Object> attributeToValueMap = propertyToAttributesMap.get(propertyName);
        if (attributeToValueMap != null) {
            return attributeToValueMap.keySet();
        }

        return null;
    }

    public Object getPropertyAttribute(String propertyName, String attributeName)
    {
        Map<String, Map<String, Object>> propertyToAttributesMap = getMap("propertyToAttributesMap");
        Map<String, Object> attributeToValueMap = propertyToAttributesMap.get(propertyName);
        if (attributeToValueMap != null) {
            return attributeToValueMap.get(attributeName);
        }

        return null;
    }

    public String getPropertyClass(String propertyName)
    {
        return (String) getPropertyAttribute(propertyName, XWIKICLASS_ATTRIBUTE);
    }

    public Set<String> getProperties()
    {
        Map<String, Map<String, Object>> propertyToAttributesMap = getMap("propertyToAttributesMap");
        return propertyToAttributesMap.keySet();
    }
}
