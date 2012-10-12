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
