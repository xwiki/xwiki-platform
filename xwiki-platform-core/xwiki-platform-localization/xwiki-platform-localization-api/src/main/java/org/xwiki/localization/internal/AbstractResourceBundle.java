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

import org.apache.commons.collections.EnumerationUtils;

public class AbstractResourceBundle extends AbstractBundle
{
    public final static String ID_PREFIX = "resource:";
    
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

            Enumeration<String> keys = bundle.getKeys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();

                localeBundle.addTranslation(new DefaultTranslation(context, localeBundle, key, message));
            }
        } else {
            localeBundle = null;
        }

        return localeBundle;
    }
}
