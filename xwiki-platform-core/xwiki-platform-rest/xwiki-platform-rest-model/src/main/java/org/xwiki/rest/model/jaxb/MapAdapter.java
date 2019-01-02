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
package org.xwiki.rest.model.jaxb;

import java.util.HashMap;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * {@link java.util.Map} is serialized by default as a list of entries. This is fine for XML but not that nice for JSON
 * when the map keys are strings. We would like the {@link java.util.Map} to be serialized as a list of entries when the
 * output format is XML and as a JSON object when the output format is JSON. This adapter allows us to use
 * {@link java.util.Map} in the REST resource that retrieves the class property values, while the XML serialization will
 * be done using the schema-generated {@link Map}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public class MapAdapter extends XmlAdapter<Map, java.util.Map<String, java.lang.Object>>
{
    @Override
    public Map marshal(java.util.Map<String, java.lang.Object> input) throws Exception
    {
        if (input == null) {
            return null;
        } else {
            Map output = new Map();
            for (java.util.Map.Entry<String, java.lang.Object> entry : input.entrySet()) {
                String key = entry.getKey();
                java.lang.Object value = entry.getValue();
                if (value instanceof java.util.Map) {
                    java.util.Map<String, java.lang.Object> nestedMap = new HashMap<>();
                    ((java.util.Map<?, ?>) value).forEach((k, v) -> nestedMap.put(k.toString(), v));
                    value = marshal(nestedMap);
                }
                output.getEntries().add(new MapEntry().withKey(key).withValue(value));
            }
            return output;
        }
    }

    @Override
    public java.util.Map<String, java.lang.Object> unmarshal(Map input) throws Exception
    {
        if (input == null) {
            return null;
        } else {
            java.util.Map<String, java.lang.Object> output = new HashMap<>();
            input.getEntries().forEach((entry) -> output.put(entry.getKey(), entry.getValue()));
            return output;
        }
    }
}
