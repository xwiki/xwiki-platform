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
package org.xwiki.livedata.rest.model.jaxb;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rest.model.jaxb.MapAdapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A Java equivalent of a JSON map.
 * <p>
 * We don't generate this class from the schema because we need the {@link XmlJavaTypeAdapter} annotation. We tried
 * various ways to add the {@link XmlJavaTypeAdapter} annotation to the schema-generated class from the separate
 * bindings file but none succeeded.
 * 
 * @version $Id$
 * @since 12.10
 */
@XmlJavaTypeAdapter(MapAdapter.class)
public class StringMap extends HashMap<String, Object>
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = LoggerFactory.getLogger(StringMap.class);

    /**
     * Parses the given JSON representation of a string map.
     * 
     * @param json a serialized JSON object (the equivalent of a Java {@link Map}).
     * @return the corresponding {@link StringMap} instance
     * @see <a href="https://blog.dejavu.sk/inject-custom-java-types-via-jax-rs-parameter-annotations/">Inject custom
     *      Java types via JSX-RS parameter annotations</a>
     */
    public static StringMap fromString(String json)
    {
        try {
            return new ObjectMapper().readerFor(StringMap.class).readValue(json);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to convert [{}] to StringMap. Root cause is: [{}].", json,
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * Converts a generic map to a string map.
     * 
     * @param map the generic map to take the entries from
     * @return the string map
     */
    public static StringMap fromMap(Map<?, ?> map)
    {
        StringMap stringMap = new StringMap();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey().toString() : null;
            stringMap.put(key, entry.getValue());
        }
        return stringMap;
    }

    @Override
    public String toString()
    {
        try {
            return new ObjectMapper().writerFor(StringMap.class).writeValueAsString(this);
        } catch (JsonProcessingException e) {
            String result = super.toString();
            LOGGER.warn("Failed to serialize StringMap [{}] as JSON. Root cause is: [{}].", result,
                ExceptionUtils.getRootCauseMessage(e));
            return result;
        }
    }
}
