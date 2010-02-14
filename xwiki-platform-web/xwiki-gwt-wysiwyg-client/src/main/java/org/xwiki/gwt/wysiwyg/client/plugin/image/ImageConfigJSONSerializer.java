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
package org.xwiki.gwt.wysiwyg.client.plugin.image;

import org.xwiki.gwt.user.client.StringUtils;

/**
 * Serializes an {@link ImageConfig} to JSON.
 * 
 * @version $Id$
 */
public class ImageConfigJSONSerializer
{
    /**
     * Serializes the given {@link ImageConfig} object to JSON.
     * 
     * @param imageConfig the image configuration object to be serialized
     * @return the JSON serialization
     */
    public String serialize(ImageConfig imageConfig)
    {
        StringBuffer result = new StringBuffer();
        append(result, serialize("reference", imageConfig.getReference()));
        append(result, serialize("url", imageConfig.getImageURL()));
        append(result, serialize("width", imageConfig.getWidth()));
        append(result, serialize("height", imageConfig.getHeight()));
        append(result, serialize("alttext", imageConfig.getAltText()));
        append(result, serialize("alignment", imageConfig.getAlignment()));

        return "{" + result.toString() + "}";
    }

    /**
     * Serializes a property of the {@link ImageConfig}.
     * 
     * @param property the name of the property to serialize
     * @param value the value of the specified property
     * @return the {@code property:value} JSON pair if the property value is not {@code null}, the empty string
     *         otherwise
     */
    private String serialize(String property, Object value)
    {
        return value != null ? property + ":'" + value.toString().replace("'", "\\'") + '\'' : "";
    }

    /**
     * Appends a JSON pair to the given {@link StringBuffer}.
     * 
     * @param result the string buffer where to append the given pair
     * @param pair a {@code property:value} JSON pair
     */
    private void append(StringBuffer result, String pair)
    {
        if (!StringUtils.isEmpty(pair)) {
            if (result.length() > 0) {
                result.append(",");
            }
            result.append(pair);
        }
    }
}
