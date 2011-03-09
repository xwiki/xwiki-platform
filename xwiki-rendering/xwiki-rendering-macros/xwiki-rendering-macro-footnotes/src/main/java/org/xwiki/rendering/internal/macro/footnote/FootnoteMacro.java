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
package org.xwiki.rendering.internal.macro.footnote;

import java.util.Collections;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.footnote.FootnoteMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Generate footnotes, listed at the end of the page. A reference to the generated footnote is inserted at the location
 * where the macro is called.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component(FootnoteMacro.MACRO_NAME)
public class FootnoteMacro extends AbstractMacro<FootnoteMacroParameters>
{
    /** The name of this macro. */
    public static final String MACRO_NAME = "footnote";

    /** The description of the macro. */
    private static final String DESCRIPTION = "Generates a footnote to display at the end of the page.";

    /** The description of the macro content. */
    private static final String CONTENT_DESCRIPTION = "the text to place in the footnote";

    /**
     * Create and initialize the descriptor of the macro.
     */
    public FootnoteMacro()
    {
        super("Footnote", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION),
            FootnoteMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
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
     * {@inheritDoc} Decrease the priority of the footnote macro, so that the putFootnote macro executes first.
     * 
     * @see org.xwiki.rendering.macro.Macro#getPriority()
     */
    @Override
    public int getPriority()
    {
        return 500;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(FootnoteMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        Block root = context.getXDOM();

        Block matchingBlock = root.getFirstBlock(new MacroBlockMatcher(PutFootnotesMacro.MACRO_NAME),
            Block.Axes.DESCENDANT);
        if (matchingBlock != null) {
            return Collections.emptyList();
        }

        Block putFootnotesMacro =
            new MacroBlock(PutFootnotesMacro.MACRO_NAME, Collections.<String, String> emptyMap(), false);
        root.addChild(putFootnotesMacro);

        return Collections.emptyList();
    }
}
