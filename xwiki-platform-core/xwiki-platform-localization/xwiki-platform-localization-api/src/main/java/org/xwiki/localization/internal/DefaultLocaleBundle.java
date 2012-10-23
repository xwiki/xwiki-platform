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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.xwiki.localization.Bundle;
import org.xwiki.localization.Translation;

public class DefaultLocaleBundle implements LocaleBundle
{
    private Bundle bundle;

    private Locale locale;

    private Map<String, Translation> translations = new HashMap<String, Translation>();

    public DefaultLocaleBundle(Bundle bundle, Locale locale)
    {
        this.locale = locale;
    }

    @Override
    public Bundle getBundle()
    {
        return this.bundle;
    }

    @Override
    public Locale getLocale()
    {
        return this.locale;
    }

    @Override
    public Translation getTranslation(String key)
    {
        return translations.get(key);
    }

    public void addTranslation(Translation translation)
    {
        this.translations.put(translation.getKey(), translation);
    }
}
