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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.localization.Translation;

/**
 * Extends {@link AbstractBundle} and add {@link Locale} based cache management.
 * 
 * @version $Id$
 * @since 4.3M1
 */
public abstract class AbstractCachedBundle extends AbstractBundle
{
    /**
     * The bundle cache.
     */
    protected Map<String, LocaleBundle> bundleCache = new ConcurrentHashMap<String, LocaleBundle>();

    public AbstractCachedBundle(String id)
    {
        super(id);
    }

    public AbstractCachedBundle(String id, int priority)
    {
        super(id, priority);
    }

    private Locale getParentLocale(Locale locale)
    {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        if (StringUtils.isEmpty(language)) {
            return null;
        }

        if (StringUtils.isEmpty(country)) {
            return Locale.ROOT;
        }

        if (StringUtils.isEmpty(variant)) {
            return new Locale(language);
        }

        return new Locale(language, country);
    }

    private LocaleBundle getLocaleBundle(Locale locale)
    {
        LocaleBundle bundle = this.bundleCache.get(locale.toString());
        if (bundle != null) {
            return bundle;
        }

        bundle = createBundle(locale);

        if (bundle == null) {
            Locale parentLocale = getParentLocale(locale);
            if (parentLocale != null) {
                bundle = getLocaleBundle(parentLocale);
            }
        }

        return bundle;
    }

    @Override
    public Translation getTranslation(String key, Locale locale)
    {
        Translation translation;

        LocaleBundle bundle = getLocaleBundle(locale);
        if (bundle != null) {
            translation = bundle.getTranslation(key);
            if (translation == null) {
                Locale parentLocale = getParentLocale(locale);
                if (parentLocale != null) {
                    translation = getTranslation(key, parentLocale);
                }
            }
        } else {
            translation = null;
        }

        return translation;
    }

    protected abstract LocaleBundle createBundle(Locale locale);
}
