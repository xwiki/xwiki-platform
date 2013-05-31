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
package org.xwiki.localization.internal;

import java.util.Locale;

import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.Translation;

/**
 * A translations bundle in a specific {@link Locale}.
 * 
 * @version $Id$
 * @since 4.5M1
 */
public interface LocalizedTranslationBundle
{
    /**
     * An empty {@link LocalizedTranslationBundle}.
     */
    LocalizedTranslationBundle EMPTY = new LocalizedTranslationBundle()
    {
        @Override
        public TranslationBundle getBundle()
        {
            return null;
        }

        @Override
        public Translation getTranslation(String key)
        {
            return null;
        }

        @Override
        public Locale getLocale()
        {
            return null;
        }
    };

    /**
     * @return the {@link TranslationBundle} containing this {@link LocalizedTranslationBundle}
     */
    TranslationBundle getBundle();

    /**
     * @return the {@link Locale} associated to this bundle
     */
    Locale getLocale();

    /**
     * Get the translation associated to the passed key.
     * 
     * @param key the translation key
     * @return the translation
     */
    Translation getTranslation(String key);
}
