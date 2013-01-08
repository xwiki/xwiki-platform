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
package org.xwiki.localization.wiki.internal;

import java.io.StringReader;
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
import org.xwiki.localization.internal.AbstractCachedTranslationBundle;
import org.xwiki.localization.internal.DefaultLocalizedTranslationBundle;
import org.xwiki.localization.internal.DefaultTranslation;
import org.xwiki.localization.internal.LocalizedBundle;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.localization.message.TranslationMessageParser;

/**
 * Base class for {@link TranslationBundle}s getting resource from classloader. Provides methods for loading properties
 * from documents, watching loaded documents and invalidating cached translations.
 * 
 * @version $Id$
 * @since 4.5M1
 */
public abstract class AbstractResourceTranslationBundle extends AbstractCachedTranslationBundle implements
    TranslationBundle
{
    /**
     * The prefix to use in all resource based translations.
     */
    public static final String ID_PREFIX = "resource:";

    @Inject
    protected TranslationBundleContext bundleContext;

    protected TranslationMessageParser translationMessageParser;

    protected URL resourceURL;

    public AbstractResourceTranslationBundle(URL resourceURL, ComponentManager componentManager,
        TranslationMessageParser translationMessageParser) throws ComponentLookupException
    {
        this.bundleContext = componentManager.getInstance(TranslationBundleContext.class);

        this.translationMessageParser = translationMessageParser;

        this.logger = LoggerFactory.getLogger(getClass());

        setResourceURL(resourceURL);
    }

    protected void setResourceURL(URL resourceURL)
    {
        this.resourceURL = resourceURL;

        setId(ID_PREFIX + resourceURL);
    }

    protected LocalizedBundle loadResourceLocaleBundle(Locale locale) throws Exception
    {
        // Find resource

        String content = document.getContent();

        // Parse resource
        Properties properties = new Properties();
        properties.load(new StringReader(content));

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

    protected TranslationMessageParser getTranslationMessageParser()
    {
        return this.translationMessageParser;
    }

    @Override
    protected LocalizedBundle createBundle(Locale locale)
    {
        LocalizedBundle localeBundle;
        try {
            localeBundle = loadResourceLocaleBundle(locale);
        } catch (Exception e) {
            this.logger.error("Failed to get localization bundle", e);

            localeBundle = null;
        }

        return localeBundle;
    }
}
