package org.xwiki.localization.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;

/**
 * Default implementation of {@link ContextualLocalizationManager}.
 * 
 * @version $Id$
 * @since 5.0M1
 */
@Component
@Singleton
public class DefaultContextualLocalizationManager implements ContextualLocalizationManager
{
    /**
     * The actual localization manager.
     */
    @Inject
    private LocalizationManager localizationManager;

    /**
     * Used to get the current {@link java.util.Locale}.
     */
    @Inject
    private LocalizationContext localizationContext;

    @Override
    public Translation getTranslation(String key)
    {
        return this.localizationManager.getTranslation(key, this.localizationContext.getCurrentLocale());
    }
}
