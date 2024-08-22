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
package org.xwiki.localization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Extends {@link org.apache.commons.lang3.LocaleUtils} with more tools.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
public class LocaleUtils extends org.apache.commons.lang3.LocaleUtils
{
    // class to avoid synchronization (Init on demand)
    static class SyncAvoid
    {
        /**
         * Unmodifiable list of available locales.
         */
        private static final List<Locale> AVAILABLE_LOCALE_LIST;

        /**
         * Unmodifiable set of available locales.
         */
        private static final Set<Locale> AVAILABLE_LOCALE_SET;

        static {
            Locale[] jvmLocales = Locale.getAvailableLocales();
            final List<Locale> list = new ArrayList<>(jvmLocales.length);
            for (Locale jvmLocale : jvmLocales) {
                // Make sure the Locale has a valid format from XWiki point of view
                if (isValid(jvmLocale)) {
                    list.add(jvmLocale);
                }
            }
            AVAILABLE_LOCALE_LIST = Collections.unmodifiableList(list);
            AVAILABLE_LOCALE_SET = Collections.unmodifiableSet(new HashSet<>(list));
        }
    }

    /**
     * @param locale the locale
     * @return the parent locale
     */
    public static Locale getParentLocale(Locale locale)
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
     * Extends {@link org.apache.commons.lang3.LocaleUtils} which return {@link Locale#ROOT} for an empty string.
     * 
     * @param str the locale String to convert, null returns null
     * @return a Locale, null if null input
     * @throws IllegalArgumentException if the string is an invalid format
     * @since 5.2M1
     */
    public static Locale toLocale(String str)
    {
        if (str != null && str.isEmpty()) {
            return Locale.ROOT;
        }

        return org.apache.commons.lang3.LocaleUtils.toLocale(str);
    }

    /**
     * Same as {@link #toLocale(String)} but it never throws an exception. It returns the passed fallback locale in case
     * the given string locale has an invalid format.
     * 
     * @param str the locale string to convert, null returns null
     * @param fallback the locale to return as fallback in case the given locale string has an invalid format
     * @return a Locale, null if null input
     * @see #toLocale(String)
     * @since 6.0M1
     */
    public static Locale toLocale(String str, Locale fallback)
    {
        try {
            return toLocale(str);
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * @param locale the {@link Locale} to validate
     * @return true is the locale is a valid {@link Locale} from XWiki point of view
     * @since 14.10.22
     * @since 15.10.12
     * @since 16.4.2
     * @since 16.7.0
     */
    public static boolean isValid(Locale locale)
    {
        if (locale != null) {
            try {
                // Make sure we can parse the locale String representation
                LocaleUtils.toLocale(locale.toString());

                return true;
            } catch (Exception e) {
                // There is no reason to log the error since it's expected that the JVM is giving us unsupported locales
                // by default
            }
        }

        return false;
    }

    /**
     * Obtains an unmodifiable list of installed valid locales.
     * <p>
     * This methods adds to {@link org.apache.commons.lang3.LocaleUtils#availableLocaleList()} the validation of the
     * locales format
     * </p>
     *
     * @return the unmodifiable list of available and valid locales
     * @since 14.10.22
     * @since 15.10.12
     * @since 16.4.2
     * @since 16.7.0
     */
    public static List<Locale> availableLocaleList()
    {
        return SyncAvoid.AVAILABLE_LOCALE_LIST;
    }

    /**
     * Obtains an unmodifiable set of installed valid locales.
     * <p>
     * This methods adds to {@link org.apache.commons.lang3.LocaleUtils#availableLocaleSet()} the validation of the
     * locales format
     * </p>
     *
     * @return the unmodifiable set of available and valid locales
     * @since 14.10.22
     * @since 15.10.12
     * @since 16.4.2
     * @since 16.7.0
     */
    public static Set<Locale> availableLocaleSet()
    {
        return SyncAvoid.AVAILABLE_LOCALE_SET;
    }
}
