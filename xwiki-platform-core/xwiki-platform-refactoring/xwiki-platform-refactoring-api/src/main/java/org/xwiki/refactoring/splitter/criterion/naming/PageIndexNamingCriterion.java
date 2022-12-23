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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.internal.RefactoringUtils;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.stability.Unstable;

/**
 * A {@link NamingCriterion} based on the name of the main document being split.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class PageIndexNamingCriterion implements NamingCriterion
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PageIndexNamingCriterion.class);

    /**
     * {@link DocumentAccessBridge} used to lookup for existing wiki pages and avoid name clashes.
     */
    private final DocumentAccessBridge docBridge;

    /**
     * Base reference to be used for generating new document references.
     */
    private final DocumentReference baseDocumentReference;

    /**
     * Current value of the post-fix appended to new document names.
     */
    private int index = 0;

    /**
     * Constructs a new {@link PageIndexNamingCriterion}.
     * 
     * @param baseDocumentName base name to be used for generating new document names.
     * @param docBridge {@link DocumentAccessBridge} used to lookup for documents.
     * @deprecated since 14.10.2, 15.0RC1 use {@link #PageIndexNamingCriterion(DocumentReference, DocumentAccessBridge)}
     *             instead
     */
    @Deprecated
    public PageIndexNamingCriterion(String baseDocumentName, DocumentAccessBridge docBridge)
    {
        this(RefactoringUtils.resolveDocumentReference(baseDocumentName), docBridge);
    }

    /**
     * Constructs a new {@link PageIndexNamingCriterion}.
     * 
     * @param baseDocumentReference base reference to be used for generating new document references
     * @param docBridge {@link DocumentAccessBridge} used to lookup for documents
     * @since 14.10.2
     * @since 15.0RC1
     */
    @Unstable
    public PageIndexNamingCriterion(DocumentReference baseDocumentReference, DocumentAccessBridge docBridge)
    {
        this.baseDocumentReference = baseDocumentReference;
        this.docBridge = docBridge;
    }

    @Override
    public DocumentReference getDocumentReference(XDOM newDoc)
    {
        int newIndex = ++this.index;
        DocumentReference newDocumentReference =
            new DocumentReference(this.baseDocumentReference.getName() + INDEX_SEPERATOR + newIndex,
                this.baseDocumentReference.getLastSpaceReference());
        // Resolve any name clashes.
        int localIndex = 0;
        while (exists(newDocumentReference)) {
            // Append a trailing local index if the page already exists
            newDocumentReference = new DocumentReference(
                this.baseDocumentReference.getName() + INDEX_SEPERATOR + newIndex + INDEX_SEPERATOR + (++localIndex),
                this.baseDocumentReference.getLastSpaceReference());
        }
        return newDocumentReference;
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
}
