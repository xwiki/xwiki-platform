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
package org.xwiki.rendering.macro.container;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Abstract container macro to hold a list groups and style them together, for example laying them out as indicated by
 * the styleLayout parameter. For the moment this macro handles only the layouting, and only the columns layout. When it
 * will be enhanced with other layout styles, it should be split in multiple classes, one to handle each. This is
 * abstract to allow subclasses to provide the content of the macro.
 * 
 * @param <P> the macro parameters bean
 * @version $Id$
 * @since 3.0M1
 */
public abstract class AbstractContainerMacro<P extends ContainerMacroParameters> extends AbstractMacro<P>
{
    /**
     * The name of the parameter to convey style information to the HTML (html style attribute).
     */
    private static final String CLASS_ATTRIBUTE = "class";

    /**
     * The component manager used to dynamically fetch components (syntax parsers, in this case).
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Builds a container macro.
     * 
     * @param name the name of the macro
     * @param description the description of the macro
     * @param contentDescriptor the descriptor of the content of this macro
     * @param parametersBeanClass the type of parameters of this macro
     */
    protected AbstractContainerMacro(String name, String description, ContentDescriptor contentDescriptor,
        Class< ? > parametersBeanClass)
    {
        super(name, description, contentDescriptor, parametersBeanClass);
    }

    @Override
    public List<Block> execute(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // TODO: include here container CSS. FTM the only rule, the one about justified text, is in the columns.css, in
        // which case the justification makes the most sense.
        // transform the container in a group, with appropriate parameters
        Map<String, String> containerParameters = new HashMap<String, String>();
        if (parameters.isJustify()) {
            containerParameters.put(CLASS_ATTRIBUTE, "container-justified");
        }

        // create the root block for the container macro, as a group block, and add all the blocks resulted from parsing
        // its content
        GroupBlock containerRoot = new GroupBlock(containerParameters);
        containerRoot.addChildren(getContent(parameters, content, context));

        // grab the layout manager to layout this container
        LayoutManager layoutManager = getLayoutManager(parameters.getLayoutStyle());
        // if a suitable layout manager was found, layout this container
        if (layoutManager != null) {
            layoutManager.layoutContainer(containerRoot);
        }

        // add the css class, if any, to the container root
        if (StringUtils.isNotEmpty(parameters.getCssClass())) {
            containerRoot.setParameter(CLASS_ATTRIBUTE, parameters.getCssClass());
        }

        // and finally return the styled container root
        return Collections.<Block> singletonList(containerRoot);
    }

    /**
     * @param layoutStyle the style passed to the container component
     * @return the layout manager to do the layouting according to the specified layout style
     */
    protected LayoutManager getLayoutManager(String layoutStyle)
    {
        try {
            return getComponentManager().getInstance(LayoutManager.class, layoutStyle);
        } catch (ComponentLookupException e) {
            // TODO: maybe should log?
            return null;
        }
    }

    /**
     * Returns the content of this macro, as blocks, either after parsing the passed {@code content} or by other means.
     * 
     * @param parameters the parameters of this macro
     * @param content the content of the macro
     * @param context the macro transformation context
     * @return a list of blocks representing the content of this macro, as blocks
     * @throws MacroExecutionException in case anything wrong happens during parsing the content
     */
    protected abstract List<Block> getContent(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException;

    /**
     * @return the componentManager
     */
    protected ComponentManager getComponentManager()
    {
        return this.componentManager;
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }
}
