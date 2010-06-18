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
     * The context of the transformation process.
     */
    private TransformationContext transformationContext;

    /**
     * The macro currently being processed.
     */
    private MacroBlock currentMacroBlock;

    /**
     * Whether the macro is called in inline mode or not.
     */
    private boolean isInline;

    /**
     * See {@link #getTransformation()}.
     */
    private Transformation transformation;

    /**
     * Constructor.
     */
    public MacroTransformationContext()
    {
        this.transformationContext = new TransformationContext();
    }

    /**
     * Constructor.
     * 
     * @param transformationContext the context of the transformation process.
     * @since 2.4M1
     */
    public MacroTransformationContext(TransformationContext transformationContext)
    {
        this.transformationContext = transformationContext;
    }

    /**
     * @return the context of the transformation process.
     * @since 2.4M1
     */
    public TransformationContext getTransformationContext()
    {
        return this.transformationContext;
    }

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
        this.transformationContext.setXDOM(xdom);
    }

    /**
     * @return the complete {@link XDOM} of the page currently being transformed.
     */
    public XDOM getXDOM()
    {
        return this.transformationContext.getXDOM();
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
        this.transformationContext.setSyntax(syntax);
    }

    /**
     * @return the current syntax.
     */
    public Syntax getSyntax()
    {
        return this.transformationContext.getSyntax();
    }

    /**
     * @return an id representing the transformation being evaluated. It's a free form name that Transformations can
     *         use, for example if they need to perform some caching based on a key. For example the Velocity Macro
     *         is using this id to pass it to the underlying Velocity Engine so that it caches macros using this key.
     * @since 2.3.2
     */
    public String getId()
    {
        return this.transformationContext.getId();
    }

    /**
     * @param id see {@link #getId()}
     * @since 2.3.2
     */
    public void setId(String id)
    {
        this.transformationContext.setId(id);
    }
}
