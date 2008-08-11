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
package org.xwiki.rendering.macro;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BulletedListBlock;
import org.xwiki.rendering.block.ListBLock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.NumberedListBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.macro.TocMacroParameterManager.Scope;
import org.xwiki.rendering.macro.parameter.descriptor.MacroParameterDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class TocMacro extends AbstractMacro
{
    /**
     * The description of the TOC macro.
     */
    private static final String DESCRIPTION = "Generates a Table Of Contents.";

    /**
     * The TOC macro parameters manager.
     */
    private TocMacroParameterManager macroParameters = new TocMacroParameterManager();

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // TODO: Use an I8N service to translate the descriptions in several languages
    }

    /**
     * {@inheritDoc}
     * 
     * @see Macro#getDescription()
     */
    public String getDescription()
    {
        // TODO: Use an I8N service to translate the description in several languages
        return DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Macro#getAllowedParameters()
     */
    public Map<String, MacroParameterDescriptor< ? >> getAllowedParameters()
    {
        return this.macroParameters.getParametersDescriptorMap();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(java.util.Map, java.lang.String, org.xwiki.rendering.block.XDOM)
     */
    public List<Block> execute(Map<String, String> parameters, String content, MacroTransformationContext macroContexte)
        throws MacroExecutionException
    {
        // Example:
        // 1 Section1
        // 1 Section2
        // 1.1 Section3
        // 1 Section4
        // 1.1.1 Section5

        // Generates:
        // ListBlock
        // |_ ListItemBlock (TextBlock: Section1)
        // |_ ListItemBlock (TextBlock: Section2)
        // ...|_ ListBlock
        // ......|_ ListItemBlock (TextBlock: Section3)
        // |_ ListItemBlock (TextBlock: Section4)
        // ...|_ ListBlock
        // ......|_ ListBlock
        // .........|_ ListItemBlock (TextBlock: Section5)

        this.macroParameters.load(parameters);

        // Get the root block from scope parameter

        Block root;

        if (this.macroParameters.getScope() == Scope.LOCAL) {
            // FIXME: need to be able to know where is the macro block to support this option
            root = macroContexte.getCurrentMacroBlock().getParent();
        } else {
            root = macroContexte.getDom();
        }

        // Get the list of sections in the scope

        List<SectionBlock> sections = root.getChildrenByType(SectionBlock.class);

        // Construct table of content from sections list
        Block rootBlock =
            generateTree(sections, this.macroParameters.getStart(), this.macroParameters.getDepth(),
                this.macroParameters.numbered());

        return Arrays.asList(rootBlock);
    }

    /**
     * Convert sections into list block tree.
     * 
     * @param sections the sections to convert.
     * @param start the "start" parameter value.
     * @param depth the "depth" parameter value.
     * @param numbered the "numbered" parameter value.
     * @return the root block of generated block tree.
     */
    private Block generateTree(List<SectionBlock> sections, int start, int depth, boolean numbered)
    {
        int currentLevel = 0;
        Block currentBlock = null;
        for (SectionBlock section : sections) {
            int level = section.getLevel().getAsInt();

            if (level >= start && level <= depth) {
                // TODO: insert title in a local link pointing on section

                ListItemBlock itemBlock = new ListItemBlock(section.getChildren());

                if (currentLevel < level) {
                    while (currentLevel < level) {
                        ListBLock childListBlock =
                            numbered ? new NumberedListBlock(Collections.<Block> emptyList()) : new BulletedListBlock(
                                Collections.<Block> emptyList());

                        if (currentBlock != null) {
                            currentBlock.addChild(childListBlock);
                        }

                        currentBlock = childListBlock;
                        ++currentLevel;
                    }
                } else if (currentLevel > level) {
                    while (currentLevel > level) {
                        currentBlock = currentBlock.getParent();
                        --currentLevel;
                    }
                }

                currentBlock.addChild(itemBlock);
            }
        }

        return currentBlock.getRoot();
    }
}
