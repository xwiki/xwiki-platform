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
 * returned value is the message in the specified language.
 * <p>
 * Properties are looked for in several {@link TranslationBundle bundles}, in the order of their
 * {@link TranslationBundle#getPriority() priority}. The first translation found in one of these bundles is the one
 * returned. If the property is not found in any of these sources, then null is returned.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Role
public interface LocalizationManager
{
    /**
     * Find a translation in the specified language.
     * 
     * @param key the key identifying the message to look for
     * @param locale the locale of the target language
     * @return the translation in the defined language
     * @see LocalizationManager
     */
    Translation getTranslation(String key, Locale locale);

    /**
     * Find a bundle.
     * 
     * @param bundleType a hint identifying the bundle type.
     * @param bundleId the identifier of the bundle, for example a wiki document name, or the URL to a
     *            <tt>.properties</tt> file.
     * @return the {@link TranslationBundle} or null if none could be found
     * @throws TranslationBundleDoesNotExistsException when no bundle could be found for the passed identifier
     * @throws TranslationBundleFactoryDoesNotExistsException when no bundle factory could be found for the passed type
     * @since 4.5M1
     */
    TranslationBundle getTranslationBundle(String bundleType, String bundleId)
        throws TranslationBundleDoesNotExistsException, TranslationBundleFactoryDoesNotExistsException;

    /**
     * Registers a resource location as a possible localization bundle that should be used in the current execution. The
     * order in which resource of the same type are considered when searching for a translation corresponds to the order
     * in which they were pulled. Each execution (generally a client request) has its own list of pulled resources, and
     * at the end of an execution, its list of pulled resources is cleared.
     * 
     * @param bundleType a hint identifying the bundle type.
     * @param bundleId the identifier of the bindle, for example a wiki document name, or the URL to a
     *            <tt>.properties</tt> file.
     * @throws TranslationBundleDoesNotExistsException when no bundle could be found for the passed identifier
     * @throws TranslationBundleFactoryDoesNotExistsException when no bundle factory could be found for the passed type
     */
    void use(String bundleType, String bundleId) throws TranslationBundleDoesNotExistsException,
        TranslationBundleFactoryDoesNotExistsException;
}
