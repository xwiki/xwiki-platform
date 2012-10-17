package org.xwiki.localization.internal.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.xwiki.localization.Bundle;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;

public class DefaultTranslationMessage implements TranslationMessage
{
    private String rawSource;

    private List<TranslationMessageElement> elements;

    public DefaultTranslationMessage(String rawSource, List<TranslationMessageElement> elements)
    {
        this.rawSource = rawSource;
        this.elements = new ArrayList<TranslationMessageElement>(elements);
    }

    @Override
    public Block render(Locale locale, Collection<Bundle> bundles, Object... parameters)
    {
        Block block = new CompositeBlock();

        for (TranslationMessageElement element : this.elements) {
            block.addChild(element.render(locale, bundles, parameters));
        }

        return null;
    }

    @Override
    public String getRawSource()
    {
        return this.rawSource;
    }
}
