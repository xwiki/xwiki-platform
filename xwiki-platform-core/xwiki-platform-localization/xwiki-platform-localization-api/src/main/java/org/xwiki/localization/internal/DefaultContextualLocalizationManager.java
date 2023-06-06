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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationException;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Default implementation of {@link ContextualLocalizationManager}.
 * 
 * @version $Id$
 * @since 5.0M1
 */
@Component
@Singleton
public class DefaultContextualLocalizationManager implements ContextualLocalizationManager
{
    /**
     * The actual localization manager.
     */
    @Inject
    private LocalizationManager localizationManager;

    /**
     * Used to get the current {@link java.util.Locale}.
     */
    @Inject
    private LocalizationContext localizationContext;

    @Override
    public Translation getTranslation(String key)
    {
        return this.localizationManager.getTranslation(key, this.localizationContext.getCurrentLocale());
    }

    @Override
    public String getTranslationPlain(String key, Object... parameters)
    {
        String result;
        try {
            result = getTranslation(key, Syntax.PLAIN_1_0, parameters);
        } catch (LocalizationException e) {
            // This shouldn't happen since a Plain Text Renderer should always be present in XWiki
            result = null;
        }
        return result;
    }

    @Override
    public String getTranslation(String key, Syntax targetSyntax, Object... parameters) throws LocalizationException
    {
        return this.localizationManager.getTranslation(key, this.localizationContext.getCurrentLocale(), targetSyntax,
            parameters);
    }
}
