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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
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
@Component
@Named("headingNames")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class HeadingNameNamingCriterion extends AbstractNamingCriterion
{
    public static final String PARAM_PREPEND_BASE_PAGE_NAME = "prependBasePageName";

    /**
     * Plain text renderer used for rendering heading names.
     */
    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainTextRenderer;

    /**
     * In case if we cannot find a heading name present in the document, we will revert back to
     * {@link PageIndexNamingCriterion}.
     */
    @Inject
    @Named("mainPageNameAndNumbering")
    private NamingCriterion mainPageNameAndNumberingNamingCriterion;

    /**
     * A list containing all the document references generated so far. This is used to avoid name clashes.
     */
    private final List<DocumentReference> documentReferences = new ArrayList<>();

    /**
     * Constructs a new {@link HeadingNameNamingCriterion}.
     * 
     * @param baseDocumentName name of the document that is being split.
     * @param docBridge {@link DocumentAccessBridge} used to lookup for documents.
     * @param plainTextRenderer the renderer to convert to plain text
     * @param prependBasePageName a flag indicating if each generated page name should be prepended with base page name.
     * @deprecated since 14.10.2, 15.0RC1 inject this as a component instead and set the base reference through
     *             {@link #getParameters()}
     */
    @Deprecated
    public HeadingNameNamingCriterion(String baseDocumentName, DocumentAccessBridge docBridge,
        BlockRenderer plainTextRenderer, boolean prependBasePageName)
    {
        getParameters().setBaseDocumentReference(RefactoringUtils.resolveDocumentReference(baseDocumentName));
        getParameters().setParameter(PARAM_PREPEND_BASE_PAGE_NAME, prependBasePageName);

        this.docBridge = docBridge;
        this.plainTextRenderer = plainTextRenderer;
        this.mainPageNameAndNumberingNamingCriterion = new PageIndexNamingCriterion(baseDocumentName, docBridge);
    }

    /**
     * Implicit constructor. Don't use it directly. Use the component manager instead (e.g. through injection).
     * 
     * @since 14.10.2
     * @since 15.0RC1
     */
    @Unstable
    public HeadingNameNamingCriterion()
    {
    }

    @Override
    public DocumentReference getDocumentReference(XDOM xdom)
    {
        DocumentReference documentReference = maybeTruncate(computeDocumentReference(xdom));

        // Resolve any name clashes.
        DocumentReference newDocumentReference = documentReference;
        int localIndex = 0;
        while (this.documentReferences.contains(newDocumentReference) || exists(newDocumentReference)) {
            // Append a trailing local index if the page already exists.
            newDocumentReference =
                newDocumentReference(getPageName(documentReference) + INDEX_SEPERATOR + (++localIndex));
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
                this.plainTextRenderer.render(heading, printer);

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
            this.mainPageNameAndNumberingNamingCriterion.getParameters()
                .setBaseDocumentReference(getParameters().getBaseDocumentReference());
            this.mainPageNameAndNumberingNamingCriterion.getParameters()
                .setUseTerminalPages(getParameters().isUseTerminalPages());
            return this.mainPageNameAndNumberingNamingCriterion.getDocumentReference(xdom);
        } else if (getParameters().getParameter(PARAM_PREPEND_BASE_PAGE_NAME, false)) {
            return newDocumentReference(getBasePageName() + INDEX_SEPERATOR + documentName.get());
        } else {
            return newDocumentReference(documentName.get());
        }
    }

    private DocumentReference maybeTruncate(DocumentReference documentReference)
    {
        // Reserve 3 characters for the suffix needed to avoid name clashes in case the document reference was used
        // previously or it exists already.
        // TODO: The max length should be taken from the store API instead of being hard-coded.
        int maxLength = (this.documentReferences.contains(documentReference) || exists(documentReference)) ? 765 : 768;

        // We can only truncate the document name, so we can't do much if the base space reference is already too large.
        // The document name can contain special characters that are escaped when serialized, requiring more length, so
        // we can't simply subtract the document name length from the reference length to get the space reference
        // length. Instead we have to compute another document reference without special characters in the name.
        // Note: We're using toString() instead of a proper serializer in order to preserve backwards compatibility with
        // old code that might still instantiate this class using the constructor.
        boolean isNotTerminal = "WebHome".equals(documentReference.getName());
        int lengthWithoutPageName = (isNotTerminal
            ? new DocumentReference(documentReference.getName(),
                new SpaceReference("n", documentReference.getLastSpaceReference().getParent()))
            : new DocumentReference("t", documentReference.getLastSpaceReference())).toString().length() - 1;
        if (lengthWithoutPageName < maxLength) {
            int maxPageNameLength = maxLength - lengthWithoutPageName;
            String pageName = getPageName(documentReference);
            if (pageName.length() > maxPageNameLength) {
                pageName = pageName.substring(0, maxPageNameLength);
                if (isNotTerminal) {
                    return new DocumentReference(documentReference.getName(),
                        new SpaceReference(pageName, documentReference.getLastSpaceReference().getParent()));
                } else {
                    return new DocumentReference(pageName, documentReference.getLastSpaceReference());
                }
            }
        }

        return documentReference;
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
