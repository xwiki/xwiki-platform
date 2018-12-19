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
package org.xwiki.localization.script;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.text.StringUtils;

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

    @Inject
    private Environment environment;

    @Inject
    private Logger logger;

    /**
     * @param key the translation key
     * @return the translation, null if none can be found
     */
    public Translation get(String key)
    {
        return get(key, this.localizationContext.getCurrentLocale());
    }

    /**
     * @param key the translation key
     * @param locale the {@link Locale} for which this translation is searched. The result might me associated to a
     *            different {@link Locale} (for example getting the {@code fr} translation when asking for the
     *            {@code fr_FR} one).
     * @return the translation, null if none can be found
     * @since 9.0RC1
     * @since 8.4.2
     */
    public Translation get(String key, Locale locale)
    {
        return this.localization.getTranslation(key, locale);
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

    /**
     * Converts the given string to a locale. E.g. the string "pt_BR" is converted to a locale with the language set to
     * Portuguese and the country set to Brazil.
     * 
     * @param str the String to convert to Locale
     * @return the corresponding locale, or {@link Locale#ROOT} if the given string is {@code null} or empty; if the
     *         given string doesn't represent a locale (e.g. invalid format) then {@code null} is returned
     * @since 5.3M2
     * @see org.apache.commons.lang3.LocaleUtils#toLocale(String)
     */
    public Locale toLocale(String str)
    {
        try {
            return LocaleUtils.toLocale(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Helpers

    /**
     * @param key the translation key
     * @return the rendered translation message
     */
    public String render(String key)
    {
        return render(key, (Collection<?>) null);
    }

    /**
     * @param keys the translations keys to try one by one
     * @return the rendered translation message
     * @since 10.2
     */
    public String render(Collection<String> keys)
    {
        return render(keys, (Collection<?>) null);
    }

    /**
     * @param key the translation key
     * @param locale the {@link Locale} for which this translation is searched. The result might me associated to a
     *            different {@link Locale} (for example getting the {@code fr} translation when asking for the
     *            {@code fr_FR} one).
     * @return the rendered translation message
     * @since 9.0RC1
     * @since 8.4.2
     */
    public String render(String key, Locale locale)
    {
        return render(key, (Collection<?>) null, locale);
    }

    /**
     * @param keys the translations keys to try one by one
     * @param locale the {@link Locale} for which this translation is searched. The result might me associated to a
     *            different {@link Locale} (for example getting the {@code fr} translation when asking for the
     *            {@code fr_FR} one).
     * @return the rendered translation message
     * @since 10.2
     */
    public String render(Collection<String> keys, Locale locale)
    {
        return render(keys, (Collection<?>) null, locale);
    }

    /**
     * @param key the translation key
     * @param parameters the translation parameters
     * @return the rendered translation message
     */
    public String render(String key, Collection<?> parameters)
    {
        return render(key, Syntax.PLAIN_1_0, parameters);
    }

    /**
     * @param keys the translations keys to try one by one
     * @param parameters the translation parameters
     * @return the rendered translation message
     */
    public String render(Collection<String> keys, Collection<?> parameters)
    {
        return render(keys, Syntax.PLAIN_1_0, parameters);
    }

    /**
     * @param key the translation key
     * @param parameters the translation parameters
     * @param locale the {@link Locale} for which this translation is searched. The result might me associated to a
     *            different {@link Locale} (for example getting the {@code fr} translation when asking for the
     *            {@code fr_FR} one).
     * @return the rendered translation message
     * @since 9.0RC1
     */
    public String render(String key, Collection<?> parameters, Locale locale)
    {
        return render(key, Syntax.PLAIN_1_0, parameters, locale);
    }

    /**
     * @param keys the translations keys to try one by one
     * @param parameters the translation parameters
     * @param locale the {@link Locale} for which this translation is searched. The result might me associated to a
     *            different {@link Locale} (for example getting the {@code fr} translation when asking for the
     *            {@code fr_FR} one).
     * @return the rendered translation message
     * @since 10.2
     */
    public String render(Collection<String> keys, Collection<?> parameters, Locale locale)
    {
        return render(keys, Syntax.PLAIN_1_0, parameters, locale);
    }

    /**
     * @param key the translation key
     * @param syntax the syntax in which to render the translation message
     * @return the rendered translation message, the key if no translation can be found and null if the rendering failed
     * @since 5.1M2
     */
    public String render(String key, Syntax syntax)
    {
        return render(key, syntax, (Collection<?>) null);
    }

    /**
     * @param keys the translations keys to try one by one
     * @param syntax the syntax in which to render the translation message
     * @return the rendered translation message, the key if no translation can be found and null if the rendering failed
     * @since 10.2
     */
    public String render(Collection<String> keys, Syntax syntax)
    {
        return render(keys, syntax, (Collection<?>) null);
    }

    /**
     * @param key the translation key
     * @param syntax the syntax in which to render the translation message
     * @param locale the {@link Locale} for which this translation is searched. The result might me associated to a
     *            different {@link Locale} (for example getting the {@code fr} translation when asking for the
     *            {@code fr_FR} one).
     * @return the rendered translation message, the key if no translation can be found and null if the rendering failed
     * @since 9.0RC1
     */
    public String render(String key, Syntax syntax, Locale locale)
    {
        return render(key, syntax, (Collection<?>) null, locale);
    }

    /**
     * @param keys the translations keys to try one by one
     * @param syntax the syntax in which to render the translation message
     * @param locale the {@link Locale} for which this translation is searched. The result might me associated to a
     *            different {@link Locale} (for example getting the {@code fr} translation when asking for the
     *            {@code fr_FR} one).
     * @return the rendered translation message, the key if no translation can be found and null if the rendering failed
     * @since 10.2
     */
    public String render(Collection<String> keys, Syntax syntax, Locale locale)
    {
        return render(keys, syntax, (Collection<?>) null, locale);
    }

    /**
     * @param key the translation key
     * @param syntax the syntax in which to render the translation message
     * @param parameters the translation parameters
     * @return the rendered translation message, the key if no translation can be found and null if the rendering failed
     */
    public String render(String key, Syntax syntax, Collection<?> parameters)
    {
        return render(key, syntax, parameters, this.localizationContext.getCurrentLocale());
    }

    /**
     * @param keys the translations keys to try one by one
     * @param syntax the syntax in which to render the translation message
     * @param parameters the translation parameters
     * @return the rendered translation message, the key if no translation can be found and null if the rendering failed
     * @since 10.2
     */
    public String render(Collection<String> keys, Syntax syntax, Collection<?> parameters)
    {
        return render(keys, syntax, parameters, this.localizationContext.getCurrentLocale());
    }

    /**
     * @param key the translation key
     * @param syntax the syntax in which to render the translation message
     * @param parameters the translation parameters
     * @param locale the {@link Locale} for which this translation is searched. The result might me associated to a
     *            different {@link Locale} (for example getting the {@code fr} translation when asking for the
     *            {@code fr_FR} one).
     * @return the rendered translation message, the key if no translation can be found and null if the rendering failed
     * @since 9.0RC1
     */
    public String render(String key, Syntax syntax, Collection<?> parameters, Locale locale)
    {
        return render(Collections.singletonList(key), syntax, parameters, locale);
    }

    /**
     * @param keys the translations keys to try one by one
     * @param syntax the syntax in which to render the translation message
     * @param parameters the translation parameters
     * @param locale the {@link Locale} for which this translation is searched. The result might me associated to a
     *            different {@link Locale} (for example getting the {@code fr} translation when asking for the
     *            {@code fr_FR} one).
     * @return the rendered translation message, the key if no translation can be found and null if the rendering failed
     * @since 10.2
     */
    public String render(Collection<String> keys, Syntax syntax, Collection<?> parameters, Locale locale)
    {
        if (CollectionUtils.isEmpty(keys)) {
            return null;
        }

        Translation translation = null;

        for (String key : keys) {
            if (key != null) {
                translation = this.localization.getTranslation(key, locale);

                if (translation != null) {
                    break;
                }
            }
        }

        String result;

        if (translation != null) {
            Block block =
                parameters != null ? translation.render(locale, parameters.toArray()) : translation.render(locale);

            // Render the block

            try {
                BlockRenderer renderer =
                    this.componentManager.get().getInstance(BlockRenderer.class, syntax.toIdString());

                DefaultWikiPrinter wikiPrinter = new DefaultWikiPrinter();
                renderer.render(block, wikiPrinter);

                result = wikiPrinter.toString();
            } catch (ComponentLookupException e) {
                // TODO set current error
                result = null;
            }
        } else {
            result = null;

            for (String key : keys) {
                if (key != null) {
                    result = key;
                }
            }
        }

        return result;
    }

    /**
     * @return the list of available locales for XWiki translations
     * @since 9.7RC1
     * @since 8.4.6
     * @since 9.6.1
     */
    @Unstable
    public Set<Locale> getAvailableLocales()
    {
        Set<Locale> locales = new HashSet<>();
        locales.addAll(Arrays.asList(Locale.getAvailableLocales()));

        try (InputStream resource = environment.getResourceAsStream("/WEB-INF/xwiki-locales.txt")) {
            LineIterator iterator = IOUtils.lineIterator(resource, StandardCharsets.US_ASCII);
            while (iterator.hasNext()) {
                String line = iterator.nextLine();
                if (StringUtils.isNotBlank(line)) {
                    locales.add(new Locale(line));
                }
            }
            iterator.close();

        } catch (Exception e) {
            logger.warn("Exception while looking for XWiki Locales.", e);
        }

        return locales;
    }
}
