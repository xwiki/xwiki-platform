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

import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import javax.inject.Inject;

import org.xwiki.localization.BundleContext;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.localization.message.TranslationMessageParser;

public abstract class AbstractResourceBundle extends AbstractCachedBundle
{
    public final static String ID_PREFIX = "resource:";

    @Inject
    protected BundleContext bundleContext;

    protected String baseName;

    protected ClassLoader classloader;

    public AbstractResourceBundle(String baseName)
    {
        super(ID_PREFIX + baseName);

        this.baseName = baseName;
    }

    public AbstractResourceBundle(String baseName, ClassLoader classloader)
    {
        this(baseName);

        this.classloader = classloader;
    }

    protected abstract TranslationMessageParser getTranslationMessageParser();

    @Override
    protected LocaleBundle createBundle(Locale locale)
    {
        // Get corresponding ResourceBundle

        ResourceBundle bundle;

        try {
            bundle =
                ResourceBundle.getBundle(this.baseName, locale, this.classloader,
                    Control.getNoFallbackControl(Control.FORMAT_DEFAULT));
        } catch (MissingResourceException e) {
            bundle = null;
        }

        // Convert to LocalBundle
        DefaultLocaleBundle localeBundle;

        if (bundle != null) {
            localeBundle = new DefaultLocaleBundle(this, locale);

            TranslationMessageParser parser = getTranslationMessageParser();

            Enumeration<String> keys = bundle.getKeys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String message = bundle.getString(key);

                TranslationMessage translationMessage = parser.parse(message);

                localeBundle.addTranslation(new DefaultTranslation(this.bundleContext, localeBundle, key,
                    translationMessage));
            }
        } else {
            localeBundle = null;
        }

        return localeBundle;
    }
}
