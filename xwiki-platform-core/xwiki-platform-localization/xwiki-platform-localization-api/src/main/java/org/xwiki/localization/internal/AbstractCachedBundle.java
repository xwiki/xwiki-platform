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
 * @since 4.3M2
 */
public abstract class AbstractCachedBundle extends AbstractBundle
{
    /**
     * The bundle cache.
     */
    protected Map<String, LocalizedBundle> bundleCache = new ConcurrentHashMap<String, LocalizedBundle>();

    /**
     * Default constructor.
     */
    protected AbstractCachedBundle()
    {
    }

    /**
     * @param id the identifier of the bundle
     */
    public AbstractCachedBundle(String id)
    {
        super(id);
    }

    /**
     * @param id the identifier of the bundle
     * @param priority the priority of the bundle
     */
    public AbstractCachedBundle(String id, int priority)
    {
        super(id, priority);
    }

    /**
     * @param locale the locale
     * @return the parent locale
     */
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

    /**
     * @param locale the Locale
     * @return the bundle containing translation for the passed Locale
     */
    private LocalizedBundle getLocalizedBundle(Locale locale)
    {
        String localeString = locale.toString();

        LocalizedBundle bundle = this.bundleCache.get(localeString);
        if (bundle == null) {
            bundle = getSynchLocalizedBundle(locale);
        }

        return bundle;
    }

    /**
     * @param locale the Locale
     * @return the bundle containing translation for the passed Locale
     */
    private synchronized LocalizedBundle getSynchLocalizedBundle(Locale locale)
    {
        String localeString = locale.toString();

        LocalizedBundle bundle = this.bundleCache.get(localeString);

        if (bundle == null) {
            bundle = createBundle(locale);
            if (bundle == null) {
                bundle = LocalizedBundle.EMPTY;
            }

            this.bundleCache.put(localeString, bundle);
        }

        return bundle;
    }

    @Override
    public Translation getTranslation(String key, Locale locale)
    {
        Translation translation;

        LocalizedBundle bundle = getLocalizedBundle(locale);
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

    /**
     * @param locale the locale
     * @return the bundle containing translation for the passed Locale
     */
    protected abstract LocalizedBundle createBundle(Locale locale);
}
