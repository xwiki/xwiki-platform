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
package org.xwiki.rendering.internal.macro.container;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroPreparationException;
import org.xwiki.rendering.macro.container.AbstractContainerMacro;
import org.xwiki.rendering.macro.container.ContainerMacroParameters;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Macro to hold a list groups and style them together, for example laying them out as indicated by the styleLayout
 * parameter. For the moment this macro handles only the layouting, and only the columns layout. When it will be
 * enhanced with other layout styles, it should be split in multiple classes, one to handle each.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component
@Named(ContainerMacro.MACRO_NAME)
@Singleton
public class ContainerMacro extends AbstractContainerMacro<ContainerMacroParameters>
{
    /**
     * The name of this macro.
     */
    public static final String MACRO_NAME = "container";

    /**
     * The description of this macro.
     */
    private static final String DESCRIPTION = "A macro to enclose multiple groups and add decoration, such as layout.";

    /**
     * The description of the content of this macro.
     */
    private static final String CONTENT_DESCRIPTION =
        "The content to enclose in this container (wiki syntax). "
            + "For the \"columns\" layout, a group should be added for each column.";

    /**
     * Used to parse the macro content.
     */
    @Inject
    private MacroContentParser contentParser;

    /**
     * Creates a container macro.
     */
    public ContainerMacro()
    {
        super("Container", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION, false, Block.LIST_BLOCK_TYPE),
            ContainerMacroParameters.class);
        setDefaultCategories(Set.of(DEFAULT_CATEGORY_LAYOUT));
    }

    @Override
    protected List<Block> getContent(ContainerMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        return this.contentParser.parse(content, context, false, false).getChildren();
    }

    @Override
    public void prepare(MacroBlock macroBlock) throws MacroPreparationException
    {
        this.contentParser.prepareContentWiki(macroBlock);
    }

    @Override
    public int getPriority()
    {
        return 750;
    }
}
