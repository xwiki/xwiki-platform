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

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BulletedListBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ListBLock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.NumberedListBlock;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.internal.util.EnumConverter;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.toc.TocMacroParameters.Scope;
import org.xwiki.rendering.macro.toc.TocMacroParameters;
import org.xwiki.rendering.renderer.LinkLabelGenerator;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Generate a Table Of Contents based on the document sections.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class TocMacro extends AbstractMacro<TocMacroParameters> implements Initializable
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Generates a Table Of Contents.";

    /**
     * Used to filter the {@link SectionBlock} title to generate the toc anchor.
     */
    private TocBlockFilter tocBlockFilter;

    /**
     * Generate link label.
     */
    private LinkLabelGenerator linkLabelGenerator;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public TocMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, null, TocMacroParameters.class));

        registerConverter(new EnumConverter(Scope.class), Scope.class);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.tocBlockFilter = new TocBlockFilter(linkLabelGenerator);
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
        List<Block> result;

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

        if (parameters.getScope() == Scope.LOCAL) {
            root = context.getCurrentMacroBlock().getParent();
        } else {
            root = context.getXDOM();
        }

        // Get the list of sections in the scope

        List<HeaderBlock> headers = root.getChildrenByType(HeaderBlock.class, true);

        // If the root block is a section, remove it's header block for the list of header blocks
        if (root instanceof SectionBlock) {
            Block block = root.getChildren().get(0);

            if (block instanceof HeaderBlock) {
                headers.remove(block);
            }
        }

        // Construct table of content from sections list
        Block tocBlock = generateTree(headers, parameters.getStart(), parameters.getDepth(), parameters.isNumbered());
        if (tocBlock != null) {
            result = Arrays.asList(tocBlock);
        } else {
            result = Collections.emptyList();
        }

        return result;
    }

    /**
     * Convert headers into list block tree.
     * 
     * @param headers the headers to convert.
     * @param start the "start" parameter value.
     * @param depth the "depth" parameter value.
     * @param numbered the "numbered" parameter value.
     * @return the root block of generated block tree or null if no header was matching the specified parameters
     */
    private Block generateTree(List<HeaderBlock> headers, int start, int depth, boolean numbered)
    {
        Block tocBlock = null;
        int currentLevel = start - 1;
        Block currentBlock = null;
        for (HeaderBlock headerBlock : headers) {
            int headerLevel = headerBlock.getLevel().getAsInt();

            if (headerLevel >= start && headerLevel <= depth) {
                // Move to next header in toc tree

                while (currentLevel < headerLevel) {
                    if (currentBlock instanceof ListBLock) {
                        currentBlock = addItemBlock(currentBlock, null);
                    }

                    currentBlock = createChildListBlock(numbered, currentBlock);
                    ++currentLevel;
                }
                while (currentLevel > headerLevel) {
                    currentBlock = currentBlock.getParent().getParent();
                    --currentLevel;
                }

                currentBlock = addItemBlock(currentBlock, headerBlock);
            }
        }

        if (currentBlock != null) {
            tocBlock = currentBlock.getRoot();
        }

        return tocBlock;
    }

    /**
     * Add a {@link ListItemBlock} in the current toc tree block and return the new {@link ListItemBlock}.
     * 
     * @param currentBlock the current block in the toc tree.
     * @param headerBlock the {@link HeaderBlock} to use to generate toc anchor label.
     * @return the new {@link ListItemBlock}.
     */
    private Block addItemBlock(Block currentBlock, HeaderBlock headerBlock)
    {
        ListItemBlock itemBlock = headerBlock == null ? createEmptyTocEntry() : createTocEntry(headerBlock);

        currentBlock.addChild(itemBlock);

        return itemBlock;
    }

    /**
     * @return a new empty list item.
     * @since 1.8RC2
     */
    private ListItemBlock createEmptyTocEntry()
    {
        return new ListItemBlock(Collections.<Block> emptyList());
    }

    /**
     * Create a new toc list item based on section title.
     * 
     * @param headerBlock the {@link HeaderBlock}.
     * @return the new list item block.
     */
    private ListItemBlock createTocEntry(HeaderBlock headerBlock)
    {
        // Create the link to target the header anchor
        Link link = new Link();
        link.setAnchor(headerBlock.getId());
        LinkBlock linkBlock = new LinkBlock(this.tocBlockFilter.generateLabel(headerBlock), link, false);

        return new ListItemBlock(Collections.<Block> singletonList(linkBlock));
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
