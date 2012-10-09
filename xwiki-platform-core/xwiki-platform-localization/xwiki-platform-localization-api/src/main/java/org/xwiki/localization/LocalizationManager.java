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
 * Internationalization service based on key/property values. The key is the id of the message being looked for, and the
 * returned value is the message in the specified language. If no language is specified, then the language configured
 * for the current execution (generally a client request) is used.
 * <p>
 * Translations can be parameterized using the format accepted by {@link java.text.MessageFormat}.
 * <p>
 * Properties are looked for in several {@link Bundle bundles}, in the order of their {@link Bundle#getPriority()
 * priority}. The first translation found in one of these bundles is the one returned. If the property is not found in
 * any of these sources, then the key is returned unchanged in place of the value.
 * 
 * @version $Id$
 * @since 4.3M1
 */
@Role
public interface LocalizationManager
{
    /**
     * Find a translation in the specified language
     * 
     * @param key the key identifying the message to look for
     * @param locale the locale of the target language
     * @return the translation in the defined language
     * @see LocalizationManager
     */
    Translation getTranslation(String key, Locale locale);

    /**
     * Registers a resource location as a possible localization bundle that should be used in the current execution. The
     * order in which resource of the same type are considered when searching for a translation corresponds to the order
     * in which they were pulled. Each execution (generally a client request) has its own list of pulled resources, and
     * at the end of an execution, its list of pulled resources is cleared.
     * 
     * @param type a hint identifying the bundle type.
     * @param bundleId the identifier of the bindle, for example a wiki document name, or the name of a
     *            <tt>.properties</tt> resource bundle.
     */
    void use(String type, String bundleId) throws BundleDoesNotExistsException, BundleFactoryDoesNotExistsException;
}
