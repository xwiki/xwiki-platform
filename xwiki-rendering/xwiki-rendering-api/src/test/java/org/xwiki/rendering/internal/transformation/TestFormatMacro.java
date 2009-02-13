package org.xwiki.rendering.internal.transformation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.macro.AbstractNoParameterMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

public class TestFormatMacro extends AbstractNoParameterMacro
{
    public TestFormatMacro()
    {
        super("Format Macro");
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    public List<Block> execute(Object parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        int wordCount = context.getXDOM().getChildrenByType(WordBlock.class, true).size();
        return Arrays.asList(new Block[] { new FormatBlock(Arrays.<Block>asList(
            new WordBlock("formatmacro" + wordCount)), Format.NONE, Collections.singletonMap("param", "value"))});
    }
}
