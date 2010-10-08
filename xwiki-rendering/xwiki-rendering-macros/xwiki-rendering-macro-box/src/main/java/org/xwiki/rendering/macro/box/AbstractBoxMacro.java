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
import java.util.LinkedHashMap;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
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
public abstract class AbstractBoxMacro<P extends BoxMacroParameters> extends AbstractMacro<P>
{
    /**
     * Used to get the current syntax parser.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Creates a new box macro.
     * 
     * @param name the name of the macro
     * @param description string describing this macro.
     * @param contentDescriptor the {@link ContentDescriptor} describing the content of this macro.
     * @param parametersBeanClass class of the parameters bean.
     */
    protected AbstractBoxMacro(String name, String description, ContentDescriptor contentDescriptor,
        Class< ? > parametersBeanClass)
    {
        super(name, description, contentDescriptor, parametersBeanClass);
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
        List< ? extends Block> titleBlockList = parameters.getBlockTitle();

        // Use a linked hashmap to keep the parameters in the same order as we create them when they are retrieved
        // by renderers. This is useful for example in the Event renderer to control the order in which the params
        // are displayed.
        Map<String, String> boxParameters = new LinkedHashMap<String, String>();
        String classParameter = parameters.getCssClass();
        String cssClass =
            StringUtils.isEmpty(classParameter) ? getClassProperty() : getClassProperty() + " " + classParameter;
        boxParameters.put("class", cssClass);

        if (!StringUtils.isEmpty(parameters.getWidth())) {
            boxParameters.put("style", "width:" + parameters.getWidth());
        }

        Block boxBlock;

        if (content != null) {
            if (context.isInline()) {
                List<Block> contentBlocks = parseContent(parameters, content, context);
                FormatBlock spanBlock = new FormatBlock(contentBlocks, Format.NONE);
                spanBlock.setParameters(boxParameters);
                boxBlock = spanBlock;
            } else {
                boxBlock = new GroupBlock(boxParameters);

                // we add the image, if there is one
                if (!StringUtils.isEmpty(imageParameter)) {
                    ResourceReference imageReference = new ResourceReference(imageParameter, ResourceType.URL);
                    Block imageBlock = new ImageBlock(imageReference, true);
                    boxBlock.addChild(imageBlock);
                    boxBlock.addChild(NewLineBlock.NEW_LINE_BLOCK);
                }
                // we add the title, if there is one
                if (!StringUtils.isEmpty(titleParameter)) {
                    Parser parser = getSyntaxParser(context);
                    List<Block> titleBlocks = parseTitle(parser, titleParameter);
                    boxBlock.addChildren(titleBlocks);
                }
                if (titleBlockList != null) {
                    boxBlock.addChildren(titleBlockList);
                }
                List<Block> contentBlocks = parseContent(parameters, content, context);
                boxBlock.addChildren(contentBlocks);
            }

            return Collections.singletonList(boxBlock);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Renders the box's title (which can contain content in the current syntax).
     * 
     * @param parser the appropriate syntax parser
     * @param titleParameter the title which is going to be parsed
     * @return the parsing result
     * @throws MacroExecutionException if the parsing fails
     */
    private List<Block> parseTitle(Parser parser, String titleParameter) throws MacroExecutionException
    {
        try {
            List<Block> titleBlocks = parser.parse(new StringReader(titleParameter)).getChildren();

            // we try to simplify a bit the generated XDOM tree
            if (titleBlocks.size() == 1 && titleBlocks.get(0) instanceof ParagraphBlock) {
                List<Block> children = titleBlocks.get(0).getChildren();
                if (children.size() > 0) {
                    titleBlocks = children;
                }
            }

            return titleBlocks;
        } catch (ParseException e) {
            throw new MacroExecutionException("Failed to parse the box's title [" + titleParameter + "]", e);
        }
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
     * Get the parser for the current syntax.
     * 
     * @param context the context of the macro transformation (from which to get the current syntax)
     * @return the parser for the current syntax
     * @throws MacroExecutionException Failed to find source parser.
     */
    protected Parser getSyntaxParser(MacroTransformationContext context) throws MacroExecutionException
    {
        try {
            return getComponentManager().lookup(Parser.class, context.getSyntax().toIdString());
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException("Failed to find source parser for syntax [" + context.getSyntax() + "]",
                e);
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
