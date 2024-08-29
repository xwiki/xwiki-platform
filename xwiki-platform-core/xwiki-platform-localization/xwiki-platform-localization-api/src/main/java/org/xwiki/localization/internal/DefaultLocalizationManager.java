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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.localization.LocalizationException;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.TranslationBundleContext;
import org.xwiki.localization.TranslationBundleDoesNotExistsException;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.localization.TranslationBundleFactoryDoesNotExistsException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Default implementation of the {@link LocalizationManager} component.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Singleton
public class DefaultLocalizationManager implements LocalizationManager
{
    /**
     * Provides access to different bundles based on their hint (needed in {@link #use(String, String)} and access to
     * the various syntax renderers (needed in {@link #getTranslation(String, Locale, Syntax, Object...)}.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    /**
     * Used to access the current bundles.
     */
    @Inject
    private TranslationBundleContext bundleContext;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public Translation getTranslation(String key, Locale locale)
    {
        for (TranslationBundle bundle : this.bundleContext.getBundles()) {
            try {
                Translation translation = bundle.getTranslation(key, locale);
                if (translation != null && translation.getLocale().equals(locale)) {
                    return translation;
                }
            } catch (Exception e) {
                this.logger.error("Failed to get translation", e);
            }
        }

        // Try parent locale
        Locale parentLocale = LocaleUtils.getParentLocale(locale);
        if (parentLocale != null) {
            return getTranslation(key, parentLocale);
        }

        return null;
    }

    @Override
    public String getTranslationPlain(String key, Locale locale, Object... parameters)
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

    @Override
    public String getTranslation(String key, Locale locale, Syntax targetSyntax, Object... parameters)
        throws LocalizationException
    {
        String result;

        Translation translation = getTranslation(key, locale);
        if (translation == null) {
            result = null;
        } else {
            Block block = translation.render(parameters);
            DefaultWikiPrinter wikiPrinter = new DefaultWikiPrinter();
            BlockRenderer renderer = getSyntaxRenderer(targetSyntax);
            renderer.render(block, wikiPrinter);
            result = wikiPrinter.toString();
        }

        return result;
    }

    @Override
    public TranslationBundle getTranslationBundle(String bundleType, String bundleId)
        throws TranslationBundleDoesNotExistsException, TranslationBundleFactoryDoesNotExistsException
    {
        if (this.componentManagerProvider.get().hasComponent(TranslationBundle.class, bundleType + ':' + bundleId)) {
            try {
                return this.componentManagerProvider.get().<TranslationBundle> getInstance(TranslationBundle.class,
                    bundleType + ':' + bundleId);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to lookup component", e);
            }
        }

        TranslationBundleFactory bundleFactory;
        try {
            bundleFactory = this.componentManagerProvider.get().getInstance(TranslationBundleFactory.class, bundleType);
        } catch (ComponentLookupException e) {
            throw new TranslationBundleFactoryDoesNotExistsException(String.format(
                "Failed to lookup BundleFactory for type [%s]", bundleType), e);
        }

        return bundleFactory.getBundle(bundleId);
    }

    @Override
    public void use(String bundleType, String bundleId) throws TranslationBundleDoesNotExistsException,
        TranslationBundleFactoryDoesNotExistsException
    {
        TranslationBundle bundle = getTranslationBundle(bundleType, bundleId);

        this.bundleContext.addBundle(bundle);
    }

    @Override
    public Locale getDefaultLocale()
    {
        return Locale.getDefault();
    }

    private BlockRenderer getSyntaxRenderer(Syntax syntax) throws LocalizationException
    {
        BlockRenderer result;
        try {
            result = this.componentManagerProvider.get().getInstance(BlockRenderer.class, syntax.toIdString());
        } catch (ComponentLookupException e) {
            throw new LocalizationException(String.format("Failed to render the translation using the [%s] syntax",
                syntax), e);
        }
        return result;
    }
}
