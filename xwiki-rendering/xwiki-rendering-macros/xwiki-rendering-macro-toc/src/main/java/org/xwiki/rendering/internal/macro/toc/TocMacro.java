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
package org.xwiki.rendering.internal.macro.toc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BulletedListBlock;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ListBLock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.NumberedListBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.internal.util.EnumConverter;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.toc.TocMacroParameters.Scope;
import org.xwiki.rendering.macro.toc.TocMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.util.IdGenerator;

/**
 * Generate a Table Of Contents based on the document sections.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class TocMacro extends AbstractMacro<TocMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Generates a Table Of Contents.";

    /**
     * The id generator.
     */
    private IdGenerator idGenerator;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public TocMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, TocMacroParameters.class));

        registerConverter(new EnumConverter(Scope.class), Scope.class);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(TocMacroParameters parameters, String content, MacroTransformationContext context)
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

        // Get the root block from scope parameter

        Block root;
        SectionBlock rootSectionBlock = null;

        if (parameters.getScope() == Scope.LOCAL && context.getCurrentMacroBlock() != null) {
            root = context.getCurrentMacroBlock().getParent();
            rootSectionBlock = context.getCurrentMacroBlock().getPreviousBlockByType(SectionBlock.class, true);
        } else {
            root = context.getXDOM();
        }

        // Get the list of sections in the scope

        List<SectionBlock> sections = root.getChildrenByType(SectionBlock.class, true);

        if (!sections.isEmpty()) {
            // Construct table of content from sections list
            Block rootBlock =
                generateTree(sections, parameters.getStart(), parameters.getDepth(), parameters.isNumbered(),
                    rootSectionBlock);

            return Arrays.asList(rootBlock);
        }

        return Collections.emptyList();
    }

    /**
     * @return e new {@link IdBlock} with a unique name.
     */
    private IdBlock newUniqueIdBlock()
    {
        return new IdBlock(this.idGenerator.generateRandomUniqueId());
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
                } else if (rootSectionBlock.getParent() == sectionBlock.getParent() && sectionLevel <= rootSectionLevel) {
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
     * Create a new toc list item based on section title.
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
        LinkBlock linkBlock = new LinkBlock(sectionBlock.getChildren(), link, false);

        return new ListItemBlock(Arrays.<Block> asList(linkBlock));
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
