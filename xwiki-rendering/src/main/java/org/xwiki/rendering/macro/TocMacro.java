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
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ListBLock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.NumberedListBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.macro.TocMacroParameterManager.Scope;
import org.xwiki.rendering.macro.parameter.descriptor.MacroParameterDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.util.IdGenerator;

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
     * The id generator.
     */
    private IdGenerator idGenerator;

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
     * @see org.xwiki.rendering.macro.Macro#execute(java.util.Map, java.lang.String,
     *      org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    public List<Block> execute(Map<String, String> parameters, String content, MacroTransformationContext context)
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
        SectionBlock rootSectionBlock = null;

        if (this.macroParameters.getScope() == Scope.LOCAL && context.getCurrentMacroBlock() != null) {
            root = context.getCurrentMacroBlock().getParent();
            rootSectionBlock = context.getCurrentMacroBlock().getPreviousBlockByType(SectionBlock.class, true);
        } else {
            root = context.getXDOM();
        }

        // Get the list of sections in the scope

        List<SectionBlock> sections = root.getChildrenByType(SectionBlock.class, true);

        // Construct table of content from sections list
        Block rootBlock =
            generateTree(sections, this.macroParameters.getStart(), this.macroParameters.getDepth(),
                this.macroParameters.numbered(), rootSectionBlock);

        return Arrays.asList(rootBlock);
    }

    /**
     * @return e new {@link IdBlock} with a unique name.
     */
    private IdBlock newUniqueIdBlock()
    {
        return new IdBlock(this.idGenerator.generateRandomUniqueId());
    }

    /**
     * @param blocks the block to convert.
     * @return a merge of the provided list of blocks as String.
     */
    // TODO: remove this when LinkBlock will support children blocks as label
    private String getLabelFromChildren(List<Block> blocks)
    {
        StringBuffer label = new StringBuffer();
        for (Block block : blocks) {
            if (block instanceof WordBlock) {
                label.append(((WordBlock) block).getWord());
            } else if (block instanceof SpaceBlock) {
                label.append(' ');
            } else if (block instanceof SpecialSymbolBlock) {
                label.append(((SpecialSymbolBlock) block).getSymbol());
            }
        }

        return label.toString();
    }

    /**
     * Convert sections into list block tree.
     * 
     * @param sections the sections to convert.
     * @param start the "start" parameter value.
     * @param depth the "depth" parameter value.
     * @param numbered the "numbered" parameter value.
     * @param rootSectionBlock the section where the toc macro search for children sections.
     * @return the root block of generated block tree.
     */
    private Block generateTree(List<SectionBlock> sections, int start, int depth, boolean numbered,
        SectionBlock rootSectionBlock)
    {
        int rootSectionLevel = rootSectionBlock != null ? rootSectionBlock.getLevel().getAsInt() : 0;
        boolean rootSectionFound = false;

        int currentLevel = 0;
        Block currentBlock = null;
        for (SectionBlock sectionBlock : sections) {
            int sectionLevel = sectionBlock.getLevel().getAsInt();

            if (rootSectionBlock != null) {
                if (rootSectionBlock == sectionBlock) {
                    rootSectionFound = true;
                    continue;
                } else if (rootSectionBlock.getParent() == sectionBlock.getParent()
                    && sectionLevel <= rootSectionLevel) {
                    break;
                }
            } else {
                rootSectionFound = true;
            }

            if (rootSectionFound && sectionLevel >= start && sectionLevel <= depth) {
                ListItemBlock itemBlock = createTocEntry(sectionBlock);

                // Move to next section in toc tree

                while (currentLevel < sectionLevel) {
                    currentBlock = createChildListBlock(numbered, currentBlock);
                    ++currentLevel;
                }
                while (currentLevel > sectionLevel) {
                    currentBlock = currentBlock.getParent();
                    --currentLevel;
                }

                currentBlock.addChild(itemBlock);
            }
        }

        return currentBlock.getRoot();
    }

    /**
     * Create a new toc list itam based on section title.
     * 
     * @param sectionBlock the {@link SectionBlock}.
     * @return the new list item block.
     */
    private ListItemBlock createTocEntry(SectionBlock sectionBlock)
    {
        IdBlock idBlock = newUniqueIdBlock();
        sectionBlock.getParent().insertChildBefore(idBlock, sectionBlock);

        Link link = new Link();
        link.setAnchor(idBlock.getName());
        LinkBlock linkBlock = new LinkBlock(link);

        linkBlock.addChildren(sectionBlock.getChildren());
        // TODO: remove this when LinkBlock will support children blocks as label
        link.setLabel(getLabelFromChildren(sectionBlock.getChildren()));

        return new ListItemBlock(linkBlock);
    }

    /**
     * Create a new ListBlock and add it in the provided parent block.
     * 
     * @param numbered indicate if the list has to be numbered or with bullets
     * @param parentBlock the block where to add the new list block.
     * @return the new list block.
     */
    private ListBLock createChildListBlock(boolean numbered, Block parentBlock)
    {
        ListBLock childListBlock =
            numbered ? new NumberedListBlock(Collections.<Block> emptyList()) : new BulletedListBlock(Collections
                .<Block> emptyList());

        if (parentBlock != null) {
            parentBlock.addChild(childListBlock);
        }

        return childListBlock;
    }
}
