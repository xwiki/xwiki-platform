package org.xwiki.localization.internal.message;

import java.util.Collection;
import java.util.Locale;

import org.xwiki.localization.Bundle;
import org.xwiki.rendering.block.Block;

public class BlockTranslationMessageElement implements TranslationMessageElement
{
    private Block block;

    public BlockTranslationMessageElement(Block block)
    {
        this.block = block;
    }

    @Override
    public Block render(Locale locale, Collection<Bundle> bundles, Object... parameters)
    {
        return this.block;
    }
}
