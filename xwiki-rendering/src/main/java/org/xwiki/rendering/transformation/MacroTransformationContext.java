/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.transformation;

import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;

/**
 * The context of the macro transformation process. Contains information such as the current XWiki DOM for the
 * parsed content and the current Macro block being processed by the Macro transformation. 
 * 
 * @version $Id$
 */
public class MacroTransformationContext
{
    /**
     * An empty macro context. Useful for example when calling a macro that doesn't use the context passed to
     * it.
     */
    public static final MacroTransformationContext EMPTY = new MacroTransformationContext(null, XDOM.EMPTY);

    /**
     * The macro currently being processed.
     */
    private MacroBlock currentMacroBlock;

    /**
     * The complete {@link XDOM} of the page currently being transformed.
     */
    private XDOM xdom;

    /**
     * Default constructor.
     */
    public MacroTransformationContext()
    {
        this(null, XDOM.EMPTY);
    }

    /**
     * @param currentMacroBlock the macro currently processed.
     * @param xdom the complete {@link XDOM} of the page currently being transformed.
     */
    public MacroTransformationContext(MacroBlock currentMacroBlock, XDOM xdom)
    {
        setCurrentMacroBlock(currentMacroBlock);
        setXDOM(xdom);
    }

    /**
     * @param currentMacroBlock the macro currently being processed.
     */
    void setCurrentMacroBlock(MacroBlock currentMacroBlock)
    {
        this.currentMacroBlock = currentMacroBlock;
    }

    /**
     * @return the macro currently being processed.
     */
    public MacroBlock getCurrentMacroBlock()
    {
        return this.currentMacroBlock;
    }

    /**
     * @param xdom the complete {@link XDOM} of the page currently being transformed.
     */
    void setXDOM(XDOM xdom)
    {
        this.xdom = xdom;
    }

    /**
     * @return the complete {@link XDOM} of the page currently being transformed.
     */
    public XDOM getXDOM()
    {
        return this.xdom;
    }
}
