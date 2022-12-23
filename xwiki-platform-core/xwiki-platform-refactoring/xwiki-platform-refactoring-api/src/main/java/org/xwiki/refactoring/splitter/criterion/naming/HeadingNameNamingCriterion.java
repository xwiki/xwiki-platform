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
package org.xwiki.refactoring.splitter.criterion.naming;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.internal.RefactoringUtils;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BlockFilter;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.stability.Unstable;

/**
 * A {@link NamingCriterion} based on the opening heading (if present) of the document.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class HeadingNameNamingCriterion implements NamingCriterion
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadingNameNamingCriterion.class);

    /**
     * Used to render block to plain text.
     */
    private final BlockRenderer plainSyntaxRenderer;

    /**
     * {@link DocumentAccessBridge} used to lookup for existing wiki pages and avoid name clashes.
     */
    private final DocumentAccessBridge docBridge;

    /**
     * In case if we cannot find a heading name present in the document, we will revert back to
     * {@link PageIndexNamingCriterion}.
     */
    private final NamingCriterion mainPageNameAndNumberingNamingCriterion;

    /**
     * A list containing all the document references generated so far. This is used to avoid name clashes.
     */
    private final List<DocumentReference> documentReferences = new ArrayList<>();

    /**
     * The reference of the document being split.
     */
    private DocumentReference baseDocumentReference;

    /**
     * Flag indicating if each generated page name should be prepended with base page name.
     */
    private final boolean prependBasePageName;

    /**
     * Constructs a new {@link HeadingNameNamingCriterion}.
     * 
     * @param baseDocumentName name of the document that is being split.
     * @param docBridge {@link DocumentAccessBridge} used to lookup for documents.
     * @param plainSyntaxRenderer the renderer to convert to plain text
     * @param prependBasePageName a flag indicating if each generated page name should be prepended with base page name.
     * @deprecated since 14.10.2, 15.0RC1 use
     *             {@link #HeadingNameNamingCriterion(DocumentReference, DocumentAccessBridge, BlockRenderer, boolean)}
     *             instead
     */
    @Deprecated
    public HeadingNameNamingCriterion(String baseDocumentName, DocumentAccessBridge docBridge,
        BlockRenderer plainSyntaxRenderer, boolean prependBasePageName)
    {
        this(RefactoringUtils.resolveDocumentReference(baseDocumentName), docBridge, plainSyntaxRenderer,
            prependBasePageName);
    }

    /**
     * Constructs a new {@link HeadingNameNamingCriterion}.
     * 
     * @param baseDocumentReference reference of the document that is being split
     * @param docBridge {@link DocumentAccessBridge} used to lookup for documents
     * @param plainSyntaxRenderer the renderer to convert to plain text
     * @param prependBasePageName a flag indicating if each generated page name should be prepended with base page name
     * @since 14.10.2
     * @since 15.0RC1
     */
    @Unstable
    public HeadingNameNamingCriterion(DocumentReference baseDocumentReference, DocumentAccessBridge docBridge,
        BlockRenderer plainSyntaxRenderer, boolean prependBasePageName)
    {
        this.baseDocumentReference = baseDocumentReference;
        this.docBridge = docBridge;
        this.plainSyntaxRenderer = plainSyntaxRenderer;
        this.prependBasePageName = prependBasePageName;
        this.mainPageNameAndNumberingNamingCriterion = new PageIndexNamingCriterion(baseDocumentReference, docBridge);
    }

    @Override
    public DocumentReference getDocumentReference(XDOM xdom)
    {
        DocumentReference documentReference = truncate(computeDocumentReference(xdom));

        // Resolve any name clashes.
        DocumentReference newDocumentReference = documentReference;
        int localIndex = 0;
        while (this.documentReferences.contains(newDocumentReference) || exists(newDocumentReference)) {
            // Append a trailing local index if the page already exists.
            newDocumentReference = new DocumentReference(documentReference.getName() + INDEX_SEPERATOR + (++localIndex),
                documentReference.getLastSpaceReference());
        }

        // Add the newly generated document reference into the pool of generated document references.
        this.documentReferences.add(newDocumentReference);

        return newDocumentReference;
    }

    private Optional<String> getFirstHeadingName(XDOM xdom)
    {
        if (xdom.getChildren().size() > 0) {
            Block firstChild = xdom.getChildren().get(0);
            if (firstChild instanceof HeaderBlock) {
                // Clone the header block and remove any unwanted stuff.
                Block clonedHeaderBlock = firstChild.clone(new BlockFilter()
                {
                    public List<Block> filter(Block block)
                    {
                        List<Block> blocks = new ArrayList<Block>();
                        if (block instanceof WordBlock || block instanceof SpaceBlock
                            || block instanceof SpecialSymbolBlock) {
                            blocks.add(block);
                        }
                        return blocks;
                    }
                });
                XDOM heading = new XDOM(clonedHeaderBlock.getChildren());

                WikiPrinter printer = new DefaultWikiPrinter();
                this.plainSyntaxRenderer.render(heading, printer);

                String headingName = cleanPageName(printer.toString());
                return StringUtils.isEmpty(headingName) ? Optional.empty() : Optional.of(headingName);
            }
        }
        return Optional.empty();
    }

    private DocumentReference computeDocumentReference(XDOM xdom)
    {
        Optional<String> documentName = getFirstHeadingName(xdom);

        // Fall back if necessary.
        if (documentName.isEmpty()) {
            return this.mainPageNameAndNumberingNamingCriterion.getDocumentReference(xdom);
        } else if (this.prependBasePageName) {
            return new DocumentReference(this.baseDocumentReference.getName() + INDEX_SEPERATOR + documentName,
                this.baseDocumentReference.getLastSpaceReference());
        } else {
            return new DocumentReference(documentName.get(), this.baseDocumentReference.getLastSpaceReference());
        }
    }

    private DocumentReference truncate(DocumentReference documentReference)
    {
        // TODO: Implement the truncate!
        // TODO: the value should be asked to the store API instead of being hard-coded
        // int maxWidth = (this.documentReferences.contains(documentReference) || exists(documentReference)) ? 765 :
        // 768;
        // if (documentName.length() > maxWidth) {
        // documentName = documentName.substring(0, maxWidth);
        // }
        return documentReference;
    }

    private boolean exists(DocumentReference documentReference)
    {
        try {
            return this.docBridge.exists(documentReference);
        } catch (Exception e) {
            LOGGER.error("Failed to check the existence of the document with reference [{}]", documentReference, e);
        }

        return false;
    }

    /**
     * Utility method for cleaning out invalid / dangerous characters from page names.
     * 
     * @param originalName the page name to be cleaned
     * @return the cleaned page name
     */
    private String cleanPageName(String originalName)
    {
        // These characters are reserved for xwiki internal use.
        String replaced = originalName.trim().replaceAll("[\\.:]", "-");
        // Links to documents containing these characters are not rendered correctly at the moment.
        replaced = replaced.replaceAll("[@?#~/]", "");
        return replaced;

    }
}
