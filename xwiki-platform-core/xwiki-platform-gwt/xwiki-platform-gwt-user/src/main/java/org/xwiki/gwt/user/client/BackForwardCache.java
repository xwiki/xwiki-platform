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
package org.xwiki.gwt.user.client;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.JavaScriptObject;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONString;

/**
 * Allows us to preserve the state of the editor against the browser's <em>Back</em> button, the <em>soft</em> page
 * refresh and the browser crash by using the browser's cache mechanism. Modern browsers cache HTML form fields like
 * {@code input} and {@code textarea} as long as they are generated on the server and not on the client using
 * JavaScript. This class uses the value of such an element to save string properties.
 * 
 * @version $Id$
 */
public class BackForwardCache implements Updatable
{
    /**
     * The name of the boolean property that specifies if the {@link #PROPERTY_VALUE} needs to be updated with the
     * latest {@link #PROPERTY_CACHE} value.
     */
    private static final String PROPERTY_DIRTY = "__dirty";

    /**
     * The name of the property which holds the map of key-value pairs that is used for quick access to the cache.
     */
    private static final String PROPERTY_CACHE = "__cache";

    /**
     * The name of the property that is cached by the browser.
     */
    private static final String PROPERTY_VALUE = "value";

    /**
     * An HTML element whose value can be cached by the browser.
     */
    private final Element cacheable;

    /**
     * Schedules updates for the value of the {@link #cacheable} element.
     */
    private final DeferredUpdater updater = new DeferredUpdater(this);

    /**
     * Creates a new cache based on the given HTML element whose value can be cached by the browser. Examples of such
     * elements are {@code input} and {@code textarea}.
     * <p>
     * The browser doesn't cache the HTML elements added dynamically through JavaScript but only the static ones. Moving
     * an element inside the DOM tree prevents the browser from caching its value even if that element was generated on
     * the server.
     * 
     * @param cacheable any HTML element whose value can be cached by the browser
     */
    public BackForwardCache(Element cacheable)
    {
        // Fall back on an orphan hidden input element if the cache is not enabled.
        this.cacheable = cacheable != null ? cacheable : (Element) Document.get().createHiddenInputElement().cast();
    }

    /**
     * Retrieves the value of a cached property specified by the given key.
     * 
     * @param key a key in the cache
     * @return the value associated with the given key or {@code null} if there's no such key in the cache
     */
    public String get(String key)
    {
        return getMap().get(key);
    }

    /**
     * Retrieves the value of a cached property specified by the given key, falling back on a default value if the key
     * is not found in the cache.
     * 
     * @param key a key in the cache
     * @param defaultValue the value returned if the specified key is not found in the cache
     * @return the value associated with the given key or {@code defaultValue} if there's no such key in the cache
     */
    public String get(String key, String defaultValue)
    {
        String cachedValue = get(key);
        return cachedValue != null ? cachedValue : defaultValue;
    }

    /**
     * Adds a new key in the cache or updates the value of an existing key.
     * 
     * @param key a key in the cache
     * @param value the new value for the given key
     * @return the previous value associated with the given key
     */
    public String put(String key, String value)
    {
        String previousValue = getMap().put(key, value);
        if (value != previousValue && (value == null || !value.equals(previousValue))) {
            cacheable.setPropertyBoolean(PROPERTY_DIRTY, true);
            updater.deferUpdate();
        }
        return previousValue;
    }

    /**
     * @return the {@link Map} used to store the cache for quick access
     */
    @SuppressWarnings("unchecked")
    protected Map<String, String> getMap()
    {
        Map<String, String> map = (Map<String, String>) ((JavaScriptObject) cacheable.cast()).get(PROPERTY_CACHE);
        if (map == null) {
            map = deserialize(cacheable.getPropertyString(PROPERTY_VALUE));
            ((JavaScriptObject) cacheable.cast()).set(PROPERTY_CACHE, map);
        }
        return map;
    }

    @Override
    public boolean canUpdate()
    {
        // Don't update if the cacheable element is orphan or not dirty.
        return cacheable.getParentNode() != null && cacheable.getPropertyBoolean(PROPERTY_DIRTY);
    }

    @Override
    public void update()
    {
        cacheable.setPropertyString(PROPERTY_VALUE, serialize(getMap()));
        cacheable.setPropertyBoolean(PROPERTY_DIRTY, false);
    }

    /**
     * Parses the given string and extracts a map of key-value pairs.
     * 
     * @param data the string to be deserialized
     * @return the map whose serialization is the given string
     * @see #serialize(Map)
     */
    protected Map<String, String> deserialize(String data)
    {
        Map<String, String> map = new HashMap<String, String>();
        if (!StringUtils.isEmpty(data)) {
            JavaScriptObject jsObject = JavaScriptObject.fromJson(data);
            JsArrayString keys = jsObject.getKeys();
            for (int i = 0; i < keys.length(); i++) {
                String key = keys.get(i);
                Object value = jsObject.get(key);
                if (value != null) {
                    map.put(key, String.valueOf(value));
                }
            }
        }
        return map;
    }

    /**
     * Converts the given map of key-value pairs to a string from which it can be recomputed.
     * 
     * @param map the map to be serialized
     * @return a string representation of the given map
     * @see #deserialize(String)
     */
    protected String serialize(Map<String, String> map)
    {
        StringBuffer output = new StringBuffer("{");
        String separator = "";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            output.append(separator);
            output.append(new JSONString(entry.getKey()));
            output.append(':');
            output.append(new JSONString(entry.getValue()));
            separator = ",";
        }
        output.append("}");
        return output.toString();
    }
}
