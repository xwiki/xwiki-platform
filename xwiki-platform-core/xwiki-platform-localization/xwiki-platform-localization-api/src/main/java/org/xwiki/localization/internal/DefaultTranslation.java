package org.xwiki.localization.internal;

import org.xwiki.localization.BundleContext;
import org.xwiki.localization.message.TranslationMessage;

public class DefaultTranslation extends AbstractTranslation
{
    public DefaultTranslation(BundleContext context, LocaleBundle localeBundle, String key, TranslationMessage message)
    {
        super(context, localeBundle, key, message);
    }
}
