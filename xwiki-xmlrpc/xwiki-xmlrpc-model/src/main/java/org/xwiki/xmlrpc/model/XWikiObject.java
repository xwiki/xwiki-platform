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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class representing XWiki objects. An XWiki object contains the association between the properties defined in the
 * object's XWikiClass and their corresponding values.
 * 
 * @version $Id$
 */
public class XWikiObject extends XWikiObjectSummary
{
    /*
     * These are special suffixes for attaching to properties additional metadata. They are put in the same map as other
     * object's properties but they should not be returned directly to the client requesting the property list of the
     * object. If you add additional metadata properties be sure to modify the getProperties method to exclude them and
     * to add specific methods for accessing this kind of metadata
     */
    public static final String PROPERTY_ALLOWED_VALUES_SUFFIX = "##allowed_values";

    public static final String PROPERTY_TYPE_SUFFIX = "##type";

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
        Set<String> result = new HashSet<String>();

        Map<String, Object> propertyToValueMap = getMap("propertyToValueMap");
        /*
         * Don't put in the property list the pseudo-properties containing metadata such as the property type and the
         * property allowed values. They are accessed through special methods.
         */
        for (String property : propertyToValueMap.keySet()) {
            if (!property.endsWith(PROPERTY_ALLOWED_VALUES_SUFFIX) && !property.endsWith(PROPERTY_TYPE_SUFFIX)) {
                result.add(property);
            }
        }

        return result;
    }

    public void setPropertyAllowedValues(String propertyName, List values)
    {
        setProperty(String.format("%s%s", propertyName, PROPERTY_ALLOWED_VALUES_SUFFIX), values);
    }

    public List getPropertyAllowedValues(String propertyName)
    {
        return (List) getProperty(String.format("%s%s", propertyName, PROPERTY_ALLOWED_VALUES_SUFFIX));
    }

    public void setPropertyType(String propertyName, String type)
    {
        setProperty(String.format("%s%s", propertyName, PROPERTY_TYPE_SUFFIX), type);
    }

    public String getPropertyType(String propertyName)
    {
        return (String) getProperty(String.format("%s%s", propertyName, PROPERTY_TYPE_SUFFIX));
    }
}
