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

import org.xwiki.component.annotation.ComponentRole;

/**
 * Provides helper methods used by the {@link LocalizationManager} and {@link Bundle}s, such as extracting the current
 * language and current wiki name from the execution context.
 * 
 * @version $Id$
 */
@ComponentRole
public interface WikiInformation
{
    /**
     * The name of the document storing wiki preferences.
     * 
     * @todo To be removed once we have a proper configuration component.
     */
    String WIKI_PREFIX_SEPARATOR = ":";

    /**
     * The name of the document storing wiki preferences.
     * 
     * @todo To be removed once we have a proper configuration component.
     */
    String PREFERENCES_DOCUMENT_NAME = "XWiki.XWikiPreferences";

    /**
     * The name of the class storing L10N preferences.
     * 
     * @todo To be removed once we have a proper configuration component.
     */
    String PREFERENCES_CLASS_NAME = PREFERENCES_DOCUMENT_NAME;

    /**
     * Retrieves the default language configured for the current wiki.
     * 
     * @return The 2-character code of the default language.
     */
    String getDefaultWikiLanguage();

    /**
     * Retrieves the default language configured for the specified wiki.
     * 
     * @param wiki The name of the wiki.
     * @return The 2-character code of the default language.
     */
    String getDefaultWikiLanguage(String wiki);

    /**
     * Retrieves the language configured for the current request.
     * 
     * @return The 2-character code of the current language.
     */
    String getContextLanguage();

    /**
     * Retrieves the {@link Locale} configured for the current request.
     * 
     * @return The {@link Locale} taken from the execution context (which should already be configured), or a locale
     *         corresponding to the default wiki language.
     */
    Locale getContextLocale();

    /**
     * Retrieves the name of the current wiki, as found in the execution context.
     * 
     * @return A <code>String</code> representation of the current wiki name, or the default wiki name if no value is
     *         found in the execution context.
     */
    String getCurrentWikiName();
}
