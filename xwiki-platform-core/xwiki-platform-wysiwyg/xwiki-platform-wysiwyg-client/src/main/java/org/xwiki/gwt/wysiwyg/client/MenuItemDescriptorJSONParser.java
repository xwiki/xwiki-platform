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
package org.xwiki.gwt.wysiwyg.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

/**
 * Parses a list of {@link MenuItemDescriptor} from a JSON input string.
 * 
 * @version $Id$
 */
public class MenuItemDescriptorJSONParser
{
    /**
     * @param json JSON representation of a list of menu item descriptors
     * @return the list of menu item descriptors read from the given JSON string
     */
    public List<MenuItemDescriptor> parse(String json)
    {
        return getMenuItemDescriptors(JSONParser.parseStrict(json));
    }

    /**
     * Creates a list of menu item descriptors from the given JSON value.
     * 
     * @param value a JSON value
     * @return a list of menu item descriptors
     */
    private List<MenuItemDescriptor> getMenuItemDescriptors(JSONValue value)
    {
        JSONArray jsDescriptors = value.isArray();
        if (jsDescriptors == null) {
            return Collections.emptyList();
        }
        List<MenuItemDescriptor> descriptors = new ArrayList<MenuItemDescriptor>();
        for (int i = 0; i < jsDescriptors.size(); i++) {
            MenuItemDescriptor descriptor = getMenuItemDescriptor(jsDescriptors.get(i));
            if (descriptor != null) {
                descriptors.add(descriptor);
            }
        }
        return descriptors;
    }

    /**
     * Creates a menu item descriptor from the given JSON value.
     * 
     * @param value a JSON value
     * @return a menu item descriptor
     */
    private MenuItemDescriptor getMenuItemDescriptor(JSONValue value)
    {
        JSONString feature = value.isString();
        if (feature != null) {
            return new MenuItemDescriptor(feature.stringValue());
        } else {
            JSONObject jsDescriptor = value.isObject();
            if (jsDescriptor == null) {
                return null;
            }
            JSONValue oFeature = jsDescriptor.get("feature");
            if (oFeature == null || oFeature.isString() == null) {
                return null;
            }
            MenuItemDescriptor descriptor = new MenuItemDescriptor(oFeature.isString().stringValue());
            JSONValue subMenu = jsDescriptor.get("subMenu");
            if (subMenu != null) {
                descriptor.getSubMenu().addAll(getMenuItemDescriptors(subMenu));
            }
            return descriptor;
        }
    }
}
