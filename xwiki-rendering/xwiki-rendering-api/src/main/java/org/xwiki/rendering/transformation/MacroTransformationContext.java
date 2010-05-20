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

import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.syntax.Syntax;

/**
 * The context of the macro transformation process. Contains information such as the current XWiki DOM for the parsed
 * content and the current Macro block being processed by the Macro transformation.
 * 
 * @version $Id$
 */
public class MacroTransformationContext
{
    /**
     * The macro currently being processed.
     */
    private MacroBlock currentMacroBlock;

    /**
     * The complete {@link XDOM} of the page currently being transformed.
     */
    private XDOM xdom;

    /**
     * Whether the macro is called in inline mode or not.
     */
    private boolean isInline;

    /**
     * See {@link #getTransformation()}.
     */
    private Transformation transformation;

    /**
     * The current syntax.
     */
    private Syntax syntax;

    /**
     * @param currentMacroBlock the macro currently being processed.
     */
    public void setCurrentMacroBlock(MacroBlock currentMacroBlock)
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
    public void setXDOM(XDOM xdom)
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

    /**
     * @param isInline if true then the macro is called in inline mode
     */
    public void setInline(boolean isInline)
    {
        this.isInline = isInline;
    }

    /**
     * @return true if the macro is called in inline mode (ie inside a paragraph, a list item, etc)
     */
    public boolean isInline()
    {
        return this.isInline;
    }

    /**
     * @param transformation the Transformation being used
     * @see #getTransformation()
     * @since 2.4M1
     */
    public void setTransformation(Transformation transformation)
    {
        this.transformation = transformation;
    }

    /**
     * @return the current Transformation instance being executed. Useful for Macros which need to perform other
     *         transformations in turn such as the Include macro which needs to execute the transformation if the
     *         included page should be executed in its own context.
     * @since 2.4M1
     */
    public Transformation getTransformation()
    {
        return this.transformation;
    }

    /**
     * @param syntax the current syntax.
     */
    public void setSyntax(Syntax syntax)
    {
        this.syntax = syntax;
    }

    /**
     * @return the current syntax.
     */
    public Syntax getSyntax()
    {
        return syntax;
    }
}
