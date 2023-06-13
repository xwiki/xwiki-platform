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
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

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
     * Find a translation in the specified locale.
     * 
     * @param key the key identifying the message to look for
     * @param locale the {@link Locale} for which this translation is searched. The result might me associated to a
     *            different {@link Locale} (for example getting the {@code fr} translation when asking for the
     *            {@code fr_FR} one).
     * @return the translation in the defined language
     * @see LocalizationManager
     */
    Translation getTranslation(String key, Locale locale);

    /**
     * Find a translation in the specified locale.
     * 
     * @param key the key identifying the message to look for
     * @param locale the {@link Locale} for which this translation is searched. The result might be associated to a
     *            different {@link Locale} (for example getting the {@code fr} translation when asking for the
     *            {@code fr_FR} one).
     * @param parameters the parameters
     * @return the translation in the defined language, rendered as plain text, null if no translation could be found
     * @see #getTranslation(String, Locale)
     * @since 14.1RC1
     * @since 13.10.3
     */
    default String getTranslationPlain(String key, Locale locale, Object... parameters)
    {
        String result;
        try {
            result = getTranslation(key, locale, Syntax.PLAIN_1_0, parameters);
        } catch (LocalizationException e) {
            // This shouldn't happen since a Plain Text Renderer should always be present in XWiki
            result = null;
        }
        return result;
    }

    /**
     * Find a translation in the specified locale.
     *
     * @param key the key identifying the message to look for
     * @param locale the {@link Locale} for which this translation is searched. The result might be associated to a
     *            different {@link Locale} (for example getting the {@code fr} translation when asking for the
     *            {@code fr_FR} one).
     * @param targetSyntax the syntax in which to render the translation
     * @param parameters the parameters
     * @return the translation in the defined language, rendered in the target syntax, null if no translation could be
     *         found or it couldn't be rendered
     * @throws LocalizationException if there's an error while getting the Renderer for the passed syntax
     * @see #getTranslation(String, Locale)
     * @since 15.5RC1
     * @since 14.10.12
     */
    @Unstable
    default String getTranslation(String key, Locale locale, Syntax targetSyntax, Object... parameters)
        throws LocalizationException
    {
        return null;
    }

    /**
     * Find a bundle.
     * 
     * @param bundleType a hint identifying the bundle type.
     * @param bundleId the identifier of the bundle, for example a wiki document name, or the URL to a
     *            {@code .properties} file.
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
     *            {@code .properties} file.
     * @throws TranslationBundleDoesNotExistsException when no bundle could be found for the passed identifier
     * @throws TranslationBundleFactoryDoesNotExistsException when no bundle factory could be found for the passed type
     */
    void use(String bundleType, String bundleId)
        throws TranslationBundleDoesNotExistsException, TranslationBundleFactoryDoesNotExistsException;

    /**
     * @return the {@link Locale} configured as the default
     * @since 14.1RC1
     * @since 13.10.3
     */
    default Locale getDefaultLocale()
    {
        return Locale.getDefault();
    }
}
