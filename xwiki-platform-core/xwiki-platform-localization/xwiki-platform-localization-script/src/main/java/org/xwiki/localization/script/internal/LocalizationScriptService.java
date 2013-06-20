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
package org.xwiki.localization.script.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;

/**
 * Provides Component-specific Scripting APIs.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("localization")
@Singleton
public class LocalizationScriptService implements ScriptService
{
    /**
     * Used to access translations.
     */
    @Inject
    private LocalizationManager localization;

    /**
     * Used to access current {@link java.util.Locale}.
     */
    @Inject
    private LocalizationContext localizationContext;

    /**
     * Used to lookup renderers.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    /**
     * @param key the translation key
     * @return the translation, null if none can be found
     */
    public Translation get(String key)
    {
        return this.localization.getTranslation(key, this.localizationContext.getCurrentLocale());
    }

    /**
     * @param bundleType the hint of the {@link org.xwiki.localization.TranslationBundleFactory} to use to get the
     *            actual bundle
     * @param bundleId the identifier of the bundle for the passed type
     * @return true if the bundle has been found and properly added to the list of current translation bundles, false
     *         otherwise
     */
    public boolean use(String bundleType, String bundleId)
    {
        try {
            this.localization.use(bundleType, bundleId);
        } catch (Exception e) {
            // TODO set current error
            return false;
        }

        return true;
    }

    /**
     * @return the {@link Locale} to use by default in the current context
     */
    public Locale getCurrentLocale()
    {
        return this.localizationContext.getCurrentLocale();
    }

    // Helpers

    /**
     * @param key the translation key
     * @return the rendered translation message
     */
    public String render(String key)
    {
        return render(key, Collections.EMPTY_LIST);
    }

    /**
     * @param key the translation key
     * @param parameters the translation parameters
     * @return the rendered translation message
     */
    public String render(String key, Collection< ? > parameters)
    {
        return render(key, Syntax.PLAIN_1_0, parameters);
    }

    /**
     * @param key the translation key
     * @param syntax the syntax in which to render the translation message
     * @return the rendered translation message, the key if no translation can be found and null if the rendering failed
     * @since 5.1M2
     */
    public String render(String key, Syntax syntax)
    {
        return render(key, syntax, Collections.EMPTY_LIST);
    }

    /**
     * @param key the translation key
     * @param syntax the syntax in which to render the translation message
     * @param parameters the translation parameters
     * @return the rendered translation message, the key if no translation can be found and null if the rendering failed
     */
    public String render(String key, Syntax syntax, Collection< ? > parameters)
    {
        String result = null;

        Locale currentLocale = this.localizationContext.getCurrentLocale();

        Translation translation = this.localization.getTranslation(key, currentLocale);

        if (translation != null) {
            Block block = translation.render(currentLocale, parameters.toArray());

            // Render the block

            try {
                BlockRenderer renderer =
                    this.componentManager.get().getInstance(BlockRenderer.class, syntax.toIdString());

                DefaultWikiPrinter wikiPrinter = new DefaultWikiPrinter();
                renderer.render(block, wikiPrinter);

                result = wikiPrinter.toString();
            } catch (ComponentLookupException e) {
                // TODO set current error
                block = null;
            }
        } else {
            result = key;
        }

        return result;
    }
}
