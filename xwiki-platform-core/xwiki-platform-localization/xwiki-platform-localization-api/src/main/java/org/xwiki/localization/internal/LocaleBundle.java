package org.xwiki.localization.internal;

import java.util.Locale;

import org.xwiki.localization.Translation;

public interface LocaleBundle
{
    LocaleBundle EMPTY = new LocaleBundle()
    {
        @Override
        public Translation getTranslation(String key)
        {
            return null;
        }

        @Override
        public Locale getLocale()
        {
            return null;
        }
    };

    Locale getLocale();

    Translation getTranslation(String key);
}
