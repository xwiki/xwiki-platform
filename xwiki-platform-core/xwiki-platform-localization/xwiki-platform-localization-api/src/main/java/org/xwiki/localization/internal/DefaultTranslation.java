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

import org.xwiki.localization.TranslationBundleContext;
import org.xwiki.localization.message.TranslationMessage;

/**
 * Default implementation of {@link org.xwiki.localization.Translation}.
 * 
 * @version $Id$
 * @since 4.3M2
 */
public class DefaultTranslation extends AbstractTranslation
{
    /**
     * @param context used to resolve variables
     * @param localeBundle the bundle containing the translation
     * @param key the key associated to the translation
     * @param message the actual translation message
     */
    public DefaultTranslation(TranslationBundleContext context, LocalizedTranslationBundle localeBundle, String key,
        TranslationMessage message)
    {
        super(context, localeBundle, key, message);
    }
}
