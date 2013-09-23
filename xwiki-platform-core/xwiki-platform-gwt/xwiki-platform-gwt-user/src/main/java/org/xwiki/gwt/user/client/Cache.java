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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Generic object cache that can be used to optimize the JavaScript execution. The cached objects are stored in a map
 * and a reference to this map is saved as a property of a DOM element. Anyone who has access to the underlying DOM
 * element has access to the cache.
 * 
 * @version $Id$
 */
public class Cache
{
    /**
     * Interface used to access an object when it is not found in the cache.
     * 
     * @param <T> the type of the cached object
     */
    public interface CacheCallback<T>
    {
        /**
         * @return the object to be cached
         */
        T get();
    }

    /**
     * The name of the {@link #cacheHolder} property that holds the map with the cached objects.
     */
    private static final String CACHE_PROPERTY = "__cache";

    /**
     * The JavaScript object that holds the map with the cached objects in its {@link #CACHE_PROPERTY}.
     */
    private final org.xwiki.gwt.dom.client.JavaScriptObject cacheHolder;

    /**
     * Creates a new cache that uses the given JavaScript object to store the cached objects.
     * 
     * @param cacheHolder the JavaScript object that holds the map with the cached objects
     */
    public Cache(JavaScriptObject cacheHolder)
    {
        this.cacheHolder = (org.xwiki.gwt.dom.client.JavaScriptObject) cacheHolder;
    }

    /**
     * Looks up the given key in the cache and returns the associated object if the key if found. Otherwise caches the
     * object returned by the provided call-back and returns it.
     * 
     * @param <T> the type of the cached object
     * @param key the key that identifies the requested object in the cache
     * @param callback the call-back used to retrieve the requested object when it is not found in the cache
     * @return the object associated with the given key in the cache if the cache contains such a key, otherwise the
     *         object returned by the provided call-back
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, CacheCallback<T> callback)
    {
        org.xwiki.gwt.dom.client.JavaScriptObject map = getMap();
        T object = null;
        if (map != null) {
            object = (T) map.get(key);
        }
        if (object == null) {
            object = callback.get();
            if (map != null) {
                map.set(key, object);
            }
        }
        return object;
    }

    /**
     * Clears the cache and optionally disables it.
     * 
     * @param disable {@code true} to disable the cache, {@code false} to enable the cache
     */
    public void clear(boolean disable)
    {
        if (disable) {
            cacheHolder.remove(CACHE_PROPERTY);
        } else {
            cacheHolder.set(CACHE_PROPERTY, JavaScriptObject.createObject());
        }
    }

    /**
     * NOTE: We use a JavaScript object as a map because the cache keys are strings and the lookup is done in native
     * code and thus should be faster.
     * 
     * @return the map where the cached objects are stored
     */
    private org.xwiki.gwt.dom.client.JavaScriptObject getMap()
    {
        return (org.xwiki.gwt.dom.client.JavaScriptObject) cacheHolder.get(CACHE_PROPERTY);
    }
}
