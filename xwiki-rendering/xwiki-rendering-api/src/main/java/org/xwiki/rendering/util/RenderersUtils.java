package org.xwiki.rendering.util;

import java.util.Collection;
import java.util.Collections;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.PlainTextRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

public class RenderersUtils
{
    public String renderPlainText(Block block)
    {
        return renderPlainText(Collections.singleton(block));
    }

    public String renderPlainText(Collection<Block> blocks)
    {
        WikiPrinter wikiPrinter = new DefaultWikiPrinter();
        PlainTextRenderer plainTextRenderer = new PlainTextRenderer(wikiPrinter, null);

        for (Block block : blocks) {
            block.traverse(plainTextRenderer);
        }

        return wikiPrinter.toString();
    }
}
