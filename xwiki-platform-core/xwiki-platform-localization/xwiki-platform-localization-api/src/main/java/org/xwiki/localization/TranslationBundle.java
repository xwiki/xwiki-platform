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

import java.util.Locale;

import org.xwiki.component.annotation.Role;

/**
 * A collection of translations in various Locales.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Role
public interface TranslationBundle extends Comparable<TranslationBundle>
{
    /**
     * The default priority of a Bundle.
     */
    int DEFAULTPRIORITY = 1000;

    /**
     * @return the unique identifier of the bundle
     */
    String getId();

    /**
     * When searching for a translation, the bundles are searched in order until one of these bundles contains a value
     * for the searched key. The bundle priority defines this order. Lower is better, meaning that a bundle with a
     * smaller priority value will be searched before a bundle with a higher value.
     * 
     * @return the priority
     * @see #compareTo(TranslationBundle)
     */
    int getPriority();

    /**
     * Return the translation for the given key, in the specified {@link Locale}. If not translation for the exact
     * Locale can be found it fallback on the language and then on the default translation if any.
     * 
     * @param key the key to translate.
     * @param locale the locale to translate into.
     * @return the {@link Translation} or null if none can be found.
     */
    Translation getTranslation(String key, Locale locale);
}
