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

import java.util.List;

/**
 * <p>
 * Internationalization service based on key/property values. The key is the id of the message being looked for, and the
 * returned value is the message in the specified language. If no language is specified, then the language configured
 * for the current execution (generally a client request) is used.
 * </p>
 * <p>
 * Translations can be parameterized using the format accepted by {@link java.text.MessageFormat}.
 * </p>
 * <p>
 * Properties are looked for in several {@link Bundle bundles}, in the order of their {@link Bundle#getPriority()
 * priority}. The first translation found in one of these bundles is the one returned. If the property is not found in
 * any of these sources, then the key is returned unchanged in place of the value.
 * </p>
 * 
 * @version $Id$
 */
public interface LocalizationManager
{
    /**
     * Registers a resource location as a possible localization bundle that should be used in the current execution. The
     * order in which resource of the same type are considered when searching for a translation corresponds to the order
     * in which they were pulled. Each execution (generally a client request) has its own list of pulled resources, and
     * at the end of an execution, its list of pulled resources is cleared.
     * 
     * @param bundleTypeHint A hint identifying the bundle type.
     * @param bundleLocation The location of the resource to use, for example a wiki document name, or the name of a
     *            <tt>.properties</tt> resource bundle.
     */
    void use(String bundleTypeHint, String bundleLocation);

    /**
     * Find a translation in the current language (taken from the execution context).
     * 
     * @param key The key identifying the message to look for.
     * @return The message in the current language. The message should be a simple string without any parameters. If you
     *         need to pass parameters see {@link #get(String, java.util.List)}
     * @see #get(String, List)
     * @see #get(String, String)
     * @see LocalizationManager
     */
    String get(String key);

    /**
     * Find a translation in the specified language.
     * 
     * @param key The key identifying the message to look for.
     * @param language The 2-character code of the target language.
     * @return the message in the defined language. The message should be a simple string without any parameters. If you
     *         need to pass parameters see {@link #get(String, java.util.List)}
     * @see #get(String, List)
     * @see #get(String)
     * @see LocalizationManager
     */
    String get(String key, String language);

    /**
     * Find a translation and then format it using the passed <code>params</code>. The format is the one used by
     * {@link java.text.MessageFormat}. The language is the one configured in the execution context.
     * 
     * @param key The key identifying the message to look for.
     * @param params the list of parameters to use for replacing "{N}" elements in the string. See
     *            {@link java.text.MessageFormat} for the full syntax.
     * @return The translated string corresponding to the given key, formatted using the provided parameters.
     * @see #get(String)
     * @see #get(String, List, String)
     * @see java.text.MessageFormat
     */
    String get(String key, List< ? > params);

    /**
     * Find a translation in the specified language, and then format it using the passed <code>params</code>. The format
     * is the one used by {@link java.text.MessageFormat}.
     * 
     * @param key The key identifying the message to look for.
     * @param params The list of parameters to use for replacing "{N}" elements in the string. See
     *            {@link java.text.MessageFormat} for the full syntax.
     * @param language The 2-character code of the target language.
     * @return The translated string corresponding to the given key, formatted using the provided parameters.
     * @see #get(String, String)
     * @see #get(String, List)
     * @see java.text.MessageFormat
     */
    String get(String key, List< ? > params, String language);
}
