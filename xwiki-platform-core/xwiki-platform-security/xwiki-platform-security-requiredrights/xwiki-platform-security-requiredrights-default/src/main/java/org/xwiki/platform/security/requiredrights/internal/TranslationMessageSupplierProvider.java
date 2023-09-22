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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;

/**
 * Provides a way to easily construct a supplier using a translation message.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component(roles = TranslationMessageSupplierProvider.class)
@Singleton
public class TranslationMessageSupplierProvider
{
    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    /**
     * @param translationMessage the translation message
     * @param parameters the parameters to pass to the translation message
     * @return a supplier that returns the translation message
     */
    public Supplier<Block> get(String translationMessage, Object... parameters)
    {
        return () -> {
            Translation translation = this.contextualLocalizationManager.getTranslation(translationMessage);
            if (translation != null) {
                return translation.render(parameters);
            } else {
                // TODO: provide a better error block.
                return new WordBlock(translationMessage);
            }
        };
    }
}
