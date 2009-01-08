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
package org.xwiki.rendering.macro.box;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.XMLBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.xml.XMLElement;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Draw a box around provided content.
 * 
 * @param <P> the type of macro parameters bean.
 * @version $Id$
 * @since 1.7
 */
public abstract class AbstractBoxMacro<P extends BoxMacroParameters> extends AbstractMacro<P> implements Composable
{
    /**
     * A new line based on <code>\n</code>.
     */
    private static final char NEWLINE_N = '\n';

    /**
     * A new line based on <code>\r</code>.
     */
    private static final char NEWLINE_R = '\r';

    /**
     * A new line based on <code>\r\n</code>.
     */
    private static final String NEWLINE_RN = "\r\n";

    /**
     * Used to get the current syntax parser.
     */
    private ComponentManager componentManager;

    /**
     * A macro should overwrite this method.
     * 
     * @param macroDescriptor the macro descriptor.
     */
    protected AbstractBoxMacro(MacroDescriptor macroDescriptor)
    {
        super(macroDescriptor);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * @return the component manager.
     */
    public ComponentManager getComponentManager()
    {
        return this.componentManager;
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
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        Map<String, String> classParameter = Collections.singletonMap("class", getClassProperty());

        Block boxBlock;
        if (context.isInlined()) {
            List<Block> result = parseContent(parameters, content, context);
            FormatBlock spanBlock = new FormatBlock(result, Format.NONE);
            spanBlock.setParameters(classParameter);

            boxBlock = spanBlock;
        } else {
            // We remove the first leading and trailing new line in non inline mode to have a better readability
            // {{box}}
            // some content
            // {{box}}
            // is the same than
            // {{box}}some content{{box}}
            List<Block> result = parseContent(parameters, stripSingleNewLine(content), context);
            boxBlock = new XMLBlock(result, new XMLElement("div", classParameter));
        }

        return Collections.singletonList(boxBlock);
    }

    /**
     * Remove the first and last new line of provided content.
     * 
     * @param content the content to trim.
     * @return the trimed content.
     */
    private String stripSingleNewLine(String content)
    {
        int beginIndex = 0;
        int endIndex = content.length();
        if (endIndex > 0) {
            if (content.charAt(0) == NEWLINE_N) {
                beginIndex = 1;
            } else if (content.startsWith(NEWLINE_RN)) {
                endIndex = 2;
            } else if (content.charAt(0) == NEWLINE_R) {
                beginIndex = 1;
            }

            if (endIndex - beginIndex > 0) {
                if (content.charAt(endIndex - 1) == NEWLINE_R) {
                    endIndex -= 1;
                } else if (content.endsWith(NEWLINE_RN)) {
                    endIndex -= 2;
                } else if (content.charAt(endIndex - 1) == NEWLINE_N) {
                    endIndex -= 1;
                }
            }
        }

        return content.substring(beginIndex, endIndex);
    }

    /**
     * Execute macro content and return the result. This methods is separated form
     * {@link #execute(BoxMacroParameters, String, MacroTransformationContext)} to be able to overwrite it in macro
     * which need boxes.
     * 
     * @param parameters the parameters of the macro.
     * @param content the content of the macro.
     * @param context the context if the macros transformation.
     * @return the result of the macro execution.
     * @throws MacroExecutionException error when executing the macro.
     */
    protected abstract List<Block> parseContent(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException;

    /**
     * @return the name of the box class to use when on renderer.
     */
    protected String getClassProperty()
    {
        return "box";
    }
}
