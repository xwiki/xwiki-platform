package org.xwiki.rendering.transformation;

import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;

/**
 * The context of the macro transformation process.
 * 
 * @version $Id$
 */
public class MacroTransformationContext
{
    /**
     * An empty macro context.
     */
    public static final MacroTransformationContext EMPTY = new MacroTransformationContext(null, XDOM.EMPTY);

    /**
     * The macro currently been processed.
     */
    private MacroBlock currentMacroBlock;

    /**
     * The complete {@link XDOM} of the page currently been transformed.
     */
    private XDOM dom;

    /**
     * Default constructor.
     */
    public MacroTransformationContext()
    {

    }

    /**
     * @param currentMacroBlock the macro currently processed.
     * @param dom the complete {@link XDOM} of the page currently been transformed.
     */
    public MacroTransformationContext(MacroBlock currentMacroBlock, XDOM dom)
    {
        setCurrentMacroBlock(currentMacroBlock);
        setDom(dom);
    }

    /**
     * @param currentMacroBlock the macro currently been processed.
     */
    void setCurrentMacroBlock(MacroBlock currentMacroBlock)
    {
        this.currentMacroBlock = currentMacroBlock;
    }

    /**
     * @return the macro currently been processed.
     */
    public MacroBlock getCurrentMacroBlock()
    {
        return currentMacroBlock;
    }

    /**
     * @param dom the complete {@link XDOM} of the page currently been transformed.
     */
    void setDom(XDOM dom)
    {
        this.dom = dom;
    }

    /**
     * @return the complete {@link XDOM} of the page currently been transformed.
     */
    public XDOM getDom()
    {
        return dom;
    }
}
