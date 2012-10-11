package org.xwiki.localization.internal;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.xwiki.localization.Translation;

public class DefaultLocaleBundle implements LocaleBundle
{
    private Locale locale;

    private Map<String, Translation> translations = new HashedMap();

    public DefaultLocaleBundle(Locale locale)
    {
        this.locale = locale;
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

}
