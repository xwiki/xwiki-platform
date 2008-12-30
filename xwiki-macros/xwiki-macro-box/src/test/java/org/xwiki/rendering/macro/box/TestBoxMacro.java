package org.xwiki.rendering.macro.box;

import java.util.Collections;
import java.util.List;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.VerbatimInlineBlock;
import org.xwiki.rendering.block.VerbatimStandaloneBlock;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

public class TestBoxMacro extends AbstractBoxMacro<BoxMacroParameters>
{
    public TestBoxMacro()
    {
        super(new DefaultMacroDescriptor("TestBoxMacro", BoxMacroParameters.class));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.box.AbstractBoxMacro#parseContent(org.xwiki.rendering.macro.box.BoxMacroParameters,
     *      java.lang.String, org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    @Override
    protected List<Block> parseContent(BoxMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (context.isInlined()) {
            return Collections.<Block> singletonList(new VerbatimInlineBlock(content));
        } else {
            return Collections.<Block> singletonList(new VerbatimStandaloneBlock(content));
        }
    }
}
