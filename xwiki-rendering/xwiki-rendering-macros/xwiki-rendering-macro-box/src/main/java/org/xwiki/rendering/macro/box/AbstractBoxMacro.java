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
import java.util.LinkedHashMap;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.internal.macro.MacroContentParser;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.parser.ResourceReferenceParser;
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
     * Parses untyped image references.
     */
    @Requirement("image/untyped")
    private ResourceReferenceParser untypedImageReferenceParser;

    /**
     * The parser used to parse box content and box title parameter.
     */
    @Requirement
    private MacroContentParser contentParser;

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
        // TODO: Refactor this when it'll possible to have a specific converter associated to a macro parameter.
        ResourceReference imageReference = parameters.getImage();
        // If the image reference is unknown then resolve it with the untyped resource reference parser
        // (this happens when the user doesn't specify a type for the image reference).
        if (imageReference != null && imageReference.getType().equals(ResourceType.UNKNOWN)) {
            imageReference = this.untypedImageReferenceParser.parse(imageReference.getReference());
        }

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
                if (imageReference != null) {
                    Block imageBlock = new ImageBlock(imageReference, true);
                    boxBlock.addChild(imageBlock);
                    boxBlock.addChild(new NewLineBlock());
                }
                // we add the title, if there is one
                if (!StringUtils.isEmpty(titleParameter)) {
                    // Don't execute transformations explicitly. They'll be executed on the generated content later on.
                    boxBlock.addChildren(this.contentParser.parse(titleParameter, context, false, true));
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
     * @return the name of the CSS class to use when rendering, in case no cssClass parameter is specified.
     */
    protected String getClassProperty()
    {
        return "box";
    }

    /**
     * @return the macro content parser to use to parse content in wiki syntax
     */
    protected MacroContentParser getMacroContentParser()
    {
        return this.contentParser;
    }
}
