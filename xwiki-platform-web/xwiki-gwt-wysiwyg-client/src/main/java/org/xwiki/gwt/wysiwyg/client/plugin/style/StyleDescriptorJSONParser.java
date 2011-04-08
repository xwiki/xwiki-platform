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
package org.xwiki.gwt.wysiwyg.client.plugin.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

/**
 * Parses a list of style descriptors from a JDON input string.
 * 
 * @version $Id$
 */
public class StyleDescriptorJSONParser
{
    /**
     * @param json JSON representation of a list of style descriptors
     * @return the list of style descriptors read from the given JSON string
     */
    public List<StyleDescriptor> parse(String json)
    {
        JSONArray jsDescriptors = JSONParser.parseStrict(json).isArray();
        if (jsDescriptors == null) {
            return Collections.emptyList();
        }
        List<StyleDescriptor> descriptors = new ArrayList<StyleDescriptor>();
        for (int i = 0; i < jsDescriptors.size(); i++) {
            JSONObject jsDescriptor = jsDescriptors.get(i).isObject();
            if (jsDescriptor == null) {
                continue;
            }
            StyleDescriptor descriptor = createStyleDescriptor(jsDescriptor);
            if (descriptor != null) {
                descriptors.add(descriptor);
            }
        }
        return descriptors;
    }

    /**
     * Creates a new style descriptor from a JSON object representing a style descriptor.
     * 
     * @param jsDescriptor a JSON object representing a style descriptor
     * @return a new style descriptor matching the given JSON object
     */
    private StyleDescriptor createStyleDescriptor(JSONObject jsDescriptor)
    {
        JSONValue oName = jsDescriptor.get("name");
        if (oName == null) {
            return null;
        }
        JSONString sName = oName.isString();
        if (sName == null) {
            return null;
        }
        String name = sName.stringValue().trim();
        if (name.length() == 0) {
            return null;
        }
        JSONValue oLabel = jsDescriptor.get("label");
        JSONString sLabel = oLabel != null ? oLabel.isString() : null;
        String label = sLabel != null ? sLabel.stringValue().trim() : name;
        JSONValue inline = jsDescriptor.get("inline");
        JSONBoolean bInline = inline != null ? inline.isBoolean() : null;
        return new StyleDescriptor(name, label, bInline != null ? bInline.booleanValue() : true);
    }
}
