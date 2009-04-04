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
package org.xwiki.localization;

/**
 * A collection of translation properties.
 * 
 * @version $Id$
 */
public interface Bundle extends Comparable<Bundle>
{
    /**
     * When searching for a translation, the bundles are searched in order until one of these bundles contains a value
     * for the searched key. The bundle priority defines this order. Lower is better, meaning that a bundle with a
     * smaller priority value will be searched before a bundle with a higher value.
     * 
     * @return the priority
     * @see #compareTo(Bundle)
     */
    int getPriority();

    /**
     * Return the translation for the given key, in the current language.
     * 
     * @param key The key to translate.
     * @return If a translation is defined for this key, return it as a <code>String</code>. Otherwise, return the key
     *         unchanged.
     * @see #getTranslation(String, String)
     */
    String getTranslation(String key);

    /**
     * Return the translation for the given key, in the specified language.
     * 
     * @param key The key to translate.
     * @param language The language to translate into.
     * @return If a translation is defined for this key, return it as a <code>String</code>. Otherwise, return the key
     *         unchanged.
     * @see #getTranslation(String, String)
     */
    String getTranslation(String key, String language);

    /**
     * Some bundle types allow the client to add additional bundle locations at runtime. Generally, the given location
     * is registered in the current execution context, and is unregistered after the execution ends,
     * 
     * @param bundleLocation The location to use. Depending on the type of bundle, this could be a location on the
     *            filesystem, or the name of a <tt>.properties</tt> resource, or the name of a wiki document, etc.
     */
    void use(String bundleLocation);
}
