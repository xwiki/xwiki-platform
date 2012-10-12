package org.xwiki.localization.internal;

import java.util.Collection;
import java.util.Locale;

import org.xwiki.localization.Bundle;
import org.xwiki.localization.BundleContext;
import org.xwiki.localization.Translation;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.rendering.block.Block;

public class AbstractTranslation implements Translation
{
    private BundleContext context;

    private LocaleBundle localeBundle;

    private String key;

    private TranslationMessage message;

    public AbstractTranslation(BundleContext context, LocaleBundle localeBundle, String key, TranslationMessage message)
    {
        this.context = context;
        this.localeBundle = localeBundle;
        this.key = key;
        this.message = message;
    }

    @Override
    public Bundle getBundle()
    {
        return this.localeBundle.getBundle();
    }

    @Override
    public Locale getLocale()
    {
        return this.localeBundle.getLocale();
    }

    @Override
    public String getKey()
    {
        return this.key;
    }

    @Override
    public String getRawSource()
    {
        return this.message.getRawSource();
    }

    // Render

    @Override
    public Block render(Locale locale, Collection<Bundle> bundles, Object... parameters)
    {
        return this.message.render(locale != null ? locale : getLocale(), bundles, parameters);
    }

    private Collection<Bundle> getCurrentBundles()
    {
        return this.context != null ? this.context.getBundles() : null;
    }

    @Override
    public Block render(Object... parameters)
    {
        return render(getLocale(), getCurrentBundles(), parameters);
    }
}
