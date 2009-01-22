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

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.XMLBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.URLImage;
import org.xwiki.rendering.listener.xml.XMLElement;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
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
        String imageParameter = parameters.getImage();
        String titleParameter = parameters.getTitle();
        
        String classParameter = parameters.getCssClass();
        String cssClass = StringUtils.isEmpty(classParameter)
            ? getClassProperty()
            : getClassProperty() + " " + classParameter;
        Map<String, String> classParameterMap = Collections.singletonMap("class", cssClass);

        Block boxBlock;
        if (context.isInlined()) {
            List<Block> contentBlocks = parseContent(parameters, content, context);
            FormatBlock spanBlock = new FormatBlock(contentBlocks, Format.NONE);
            spanBlock.setParameters(classParameterMap);
            boxBlock = spanBlock;
        } else {
            boxBlock = new XMLBlock(new XMLElement("div", classParameterMap));
            
            //we add the image, if there is one
            if (!StringUtils.isEmpty(imageParameter)) {
                Image image = new URLImage(imageParameter);
                Block imageBlock = new ImageBlock(image, true);
                boxBlock.addChild(imageBlock);
                boxBlock.addChild(NewLineBlock.NEW_LINE_BLOCK);
            }
            //we add the title, if there is one
            if (!StringUtils.isEmpty(titleParameter)) {
                Parser parser = getSyntaxParser(context);
                List<Block> titleBlocks = parseTitle(parser, titleParameter);
                boxBlock.addChildren(titleBlocks);
            }
            // We remove the first leading and trailing new line in non inline mode to have a better readability
            // {{box}}
            // some content
            // {{box}}
            // is the same than
            // {{box}}some content{{box}}
            List<Block> contentBlocks = parseContent(parameters, stripSingleNewLine(content), context);
            boxBlock.addChildren(contentBlocks);
        }

        return Collections.singletonList(boxBlock);
    }

    /**
     * Renders the box's title.
     * @param parser the appropriate syntax parser
     * @param titleParameter the title which is going to be parsed
     * @return the parsing result
     * @throws MacroExecutionException if the parsing fails
     */
    private static List<Block> parseTitle(Parser parser, String titleParameter) throws MacroExecutionException {
        try {
            List<Block> titleBlocks = parser.parse(new StringReader(titleParameter)).getChildren();

            //we try to simplify a bit the generated XDOM tree
            if (titleBlocks.size() == 1) {
                List<Block> children = titleBlocks.get(0).getChildren();
                if (children.size() > 0) {
                    titleBlocks = children;
                }
            }
            
            return titleBlocks;
        } catch (ParseException e) {
            throw new MacroExecutionException(e.getMessage(), e);
        }
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
                beginIndex = 2;
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
     * Get the parser of the current wiki syntax.
     * 
     * @param context the context of the macro transformation.
     * @return the parser of the current wiki syntax.
     * @throws MacroExecutionException Failed to find source parser.
     */
    protected Parser getSyntaxParser(MacroTransformationContext context) throws MacroExecutionException
    {
        try {
            return (Parser) getComponentManager().lookup(Parser.ROLE, context.getSyntax().toIdString());
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException("Failed to find source parser", e);
        }
    }

    
    /**
     * @return the name of the CSS class to use when rendering, in case no cssClass parameter is specified.
     */
    protected String getClassProperty()
    {
        return "box";
    }
}
