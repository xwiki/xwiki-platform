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

import org.xwiki.component.annotation.Role;

/**
 * A helper for {@link LocalizationManager} which get the {@link java.util.Locale} from {@link LocalizationContext} and
 * provide various common use cases methods.
 * 
 * @see LocalizationManager
 * @version $Id$
 * @since 5.0M1
 */
@Role
public interface ContextualLocalizationManager
{
    /**
     * Find a translation in the current language.
     * 
     * @param key the key identifying the message to look for
     * @return the translation in the current language, null if no translation could be found
     * @see LocalizationManager#getTranslation(String, java.util.Locale)
     * @see LocalizationContext#getCurrentLocale()
     */
    Translation getTranslation(String key);

    /**
     * Find a translation in the current language.
     * 
     * @param key the key identifying the message to look for
     * @param parameters the parameters
     * @return the translation in the current language rendered as plain text, null if no translation could be found
     * @see #getTranslation(String)
     */
    String getTranslationPlain(String key, Object... parameters);
}
