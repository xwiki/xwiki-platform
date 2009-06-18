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
package org.xwiki.configuration;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;

/**
 * @version $Id$
 * @since 1.6M1
 */
@ComponentRole
public interface ConfigurationSource
{
    /**
     * @param key the property key for which we want the value
     * @param defaultValue the value to use if the key isn't found
     * @return the property value is found or the default value if the key wasn't found
     * @since 2.0M1
     */
    <T> T getProperty(String key, T defaultValue);
   
    /**
     * @param key the property key for which we want the value
     * @param valueClass the type of object that should be returned. The value is converted to the passed type.
     * @return the property value is found. If the key wasn't found the returned value depends on the passed valueClass:
     *         <ul>
     *           <li>String: null</li>
     *           <li>Boolean: false</li>
     *           <li>List: empty List</li>
     *           <li>Properties: empty Properties</li>
     *         </ul>
     * @since 2.0M1
     */
    <T> T getProperty(String key, Class<T> valueClass);

    /**
     * @param key the property key for which we want the value
     * @return the property as an untyped Object or null if the key wasn't found. In general you should prefer 
     *         {@link #getProperty(String, Class)} or {@link #getProperty(String, Object)}
     */
    <T> T getProperty(String key);

    /**
     * @return the list of available keys in the configuration source
     */
    List<String> getKeys();

    /**
     * @param key the key to check
     * @return true if the key is present in the configuration source or false otherwise
     */
    boolean containsKey(String key);

    /**
     * @return true if the configuration source doesn't have any key or false otherwise
     */
    boolean isEmpty();
}
