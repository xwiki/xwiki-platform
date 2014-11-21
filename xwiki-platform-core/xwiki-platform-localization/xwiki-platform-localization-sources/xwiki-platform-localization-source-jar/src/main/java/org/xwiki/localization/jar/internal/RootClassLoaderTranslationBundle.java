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
package org.xwiki.localization.jar.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections.EnumerationUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.TranslationBundleContext;
import org.xwiki.localization.internal.AbstractCachedTranslationBundle;
import org.xwiki.localization.internal.DefaultLocalizedTranslationBundle;
import org.xwiki.localization.internal.DefaultTranslation;
import org.xwiki.localization.internal.LocalizedTranslationBundle;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.localization.message.TranslationMessageParser;

/**
 * Provide translations coming from the root {@link ClassLoader}.
 * 
 * @version $Id$
 * @since 4.5M1
 */
@Component
@Named("rootclassloader")
@Singleton
public class RootClassLoaderTranslationBundle extends AbstractCachedTranslationBundle
{
    /**
     * The parser to use for each message.
     */
    @Inject
    @Named("messagetool/1.0")
    private TranslationMessageParser parser;

    /**
     * Used to access the current bundles.
     */
    @Inject
    private TranslationBundleContext bundleContext;

    @Override
    protected LocalizedTranslationBundle createBundle(Locale locale)
    {
        Properties properties = getResourceProperties(locale);

        if (properties == null) {
            return LocalizedTranslationBundle.EMPTY;
        }

        // Convert to LocalBundle
        DefaultLocalizedTranslationBundle localeBundle = new DefaultLocalizedTranslationBundle(this, locale);

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                String key = (String) entry.getKey();
                String message = (String) entry.getValue();

                TranslationMessage translationMessage = this.parser.parse(message);

                localeBundle.addTranslation(new DefaultTranslation(this.bundleContext, localeBundle, key,
                    translationMessage));
            }
        }

        return localeBundle;
    }

    /**
     * @param locale the locale to search for
     * @return the content of all the resources files associated to the provided locale
     */
    private Properties getResourceProperties(Locale locale)
    {
        String resourceName = "ApplicationResources";
        if (!locale.equals(Locale.ROOT)) {
            resourceName += "_" + locale;
        }
        resourceName += ".properties";

        Enumeration<URL> urls;
        try {
            urls = getClass().getClassLoader().getResources(resourceName);
        } catch (IOException e) {
            this.logger.error("Failed to get resource URLs from class loader for name [{}]", resourceName, e);

            return null;
        }

        if (!urls.hasMoreElements()) {
            return null;
        }

        List<URL> urlList = EnumerationUtils.toList(urls);

        Properties properties = new Properties();

        // Load resources in reverse order to give priority to first found resources (follow ClassLoader#getResource
        // behavior)
        for (ListIterator<URL> it = urlList.listIterator(urlList.size()); it.hasPrevious();) {
            URL url = it.previous();

            try {
                InputStream componentListStream = url.openStream();

                try {
                    properties.load(componentListStream);
                } finally {
                    componentListStream.close();
                }
            } catch (IOException e) {
                this.logger.error("Failed to parse resource [{}] as translation budle", url, e);
            }
        }

        return properties;
    }
}
