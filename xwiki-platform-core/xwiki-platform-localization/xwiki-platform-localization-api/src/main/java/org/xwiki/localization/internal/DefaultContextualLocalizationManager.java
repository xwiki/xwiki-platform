package org.xwiki.localization.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;

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

    /**
     * The plain text renderer.
     */
    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainRenderer;

    @Override
    public Translation getTranslation(String key)
    {
        return this.localizationManager.getTranslation(key, this.localizationContext.getCurrentLocale());
    }

    @Override
    public String getTranslationPlain(String key, Object... parameters)
    {
        Translation translation = getTranslation(key);

        if (translation == null) {
            return null;
        }

        Block block = translation.render(parameters);

        DefaultWikiPrinter wikiPrinter = new DefaultWikiPrinter();
        this.plainRenderer.render(block, wikiPrinter);

        return wikiPrinter.toString();
    }
}
