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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.TranslationBundleContext;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.localization.message.TranslationMessageParser;

/**
 * Base class for {@link TranslationBundle}s getting resource from classloader. Provides methods for loading properties
 * from documents, watching loaded documents and invalidating cached translations.
 * 
 * @version $Id$
 * @since 4.5M1
 */
public abstract class AbstractURLResourceTranslationBundle extends AbstractCachedTranslationBundle implements
    TranslationBundle
{
    /**
     * The prefix to use in all resource based translations.
     */
    public static final String ID_PREFIX = "resource:";

    /**
     * The file extension of files containing translations.
     */
    private static final String PROPERTIES_EXT = ".properties";

    /**
     * Used to add no bundles to the list of current translation bundles.
     */
    @Inject
    protected TranslationBundleContext bundleContext;

    /**
     * Used to parse translation messages.
     */
    protected TranslationMessageParser translationMessageParser;

    /**
     * The URL of the Locale.Root translations. Other Locale files will be "calculated" from it.
     */
    protected URL baseURL;

    /**
     * @param baseURL the base URL from which to calculate all translations URLs
     * @param componentManager used to lookup of the components
     * @param translationMessageParser used to parse translation messages
     * @throws ComponentLookupException failed to lookup some component
     */
    public AbstractURLResourceTranslationBundle(URL baseURL, ComponentManager componentManager,
        TranslationMessageParser translationMessageParser) throws ComponentLookupException
    {
        this.bundleContext = componentManager.getInstance(TranslationBundleContext.class);

        this.translationMessageParser = translationMessageParser;

        this.logger = LoggerFactory.getLogger(getClass());

        this.baseURL = baseURL;

        setId(ID_PREFIX + baseURL);
    }

    /**
     * @param locale the locale
     * @return the URL corresponding to the passed {@link Locale}
     */
    protected URL getLocaleURL(Locale locale)
    {
        String urlString = this.baseURL.toString();

        String localeURL = urlString;

        if (!locale.equals(Locale.ROOT)) {
            if (urlString.endsWith(PROPERTIES_EXT)) {
                int index = urlString.lastIndexOf('.');

                localeURL = urlString.substring(0, index);
                localeURL += "_" + locale.toString();
                localeURL += PROPERTIES_EXT;
            } else {
                // No idea what is it
                localeURL = null;
            }
        }

        try {
            return new URL(localeURL);
        } catch (MalformedURLException e) {
            // Should never happen
            return null;
        }
    }

    /**
     * @param locale the locale
     * @return the {@link LocalizedTranslationBundle} corresponding to the passed {@link Locale}, null if none could be
     *         found
     */
    protected LocalizedTranslationBundle loadResourceLocaleBundle(Locale locale)
    {
        // Find resource
        URL localeURL = getLocaleURL(locale);

        if (localeURL == null) {
            return null;
        }

        // Parse resource
        Properties properties = new Properties();

        try {
            InputStream componentListStream = localeURL.openStream();

            properties.load(componentListStream);
        } catch (FileNotFoundException e) {
            // No translation files for the passed locale
            return null;
        } catch (IOException e) {
            this.logger.error("Failed to parse resource [{}] as translation budle", localeURL, e);
        }

        // Convert to LocalBundle
        DefaultLocalizedTranslationBundle localeBundle = new DefaultLocalizedTranslationBundle(this, locale);

        TranslationMessageParser parser = getTranslationMessageParser();

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                String key = (String) entry.getKey();
                String message = (String) entry.getValue();

                TranslationMessage translationMessage = parser.parse(message);

                localeBundle.addTranslation(new DefaultTranslation(this.bundleContext, localeBundle, key,
                    translationMessage));
            }
        }

        return localeBundle;
    }

    /**
     * @return the parser to use
     */
    protected TranslationMessageParser getTranslationMessageParser()
    {
        return this.translationMessageParser;
    }

    @Override
    protected LocalizedTranslationBundle createBundle(Locale locale)
    {
        return loadResourceLocaleBundle(locale);
    }
}
