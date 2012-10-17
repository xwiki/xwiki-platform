package org.xwiki.localization.internal.message;

import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.rendering.block.Block;

public class BlockTranslationMessage extends BlockTranslationMessageElement implements TranslationMessage
{
    private String rawSource;

    public BlockTranslationMessage(String rawSource, Block block)
    {
        super(block);

        this.rawSource = rawSource;
    }

    @Override
    public String getRawSource()
    {
        return this.rawSource;
    }
}
