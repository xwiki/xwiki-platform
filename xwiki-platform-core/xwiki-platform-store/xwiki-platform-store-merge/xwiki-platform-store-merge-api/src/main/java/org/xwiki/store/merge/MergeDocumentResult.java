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
package org.xwiki.store.merge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.diff.Conflict;

/**
 * Store information about a merge operation of documents.
 * This object stores the three documents used for the merge, and all results of the different parts of the documents.
 * See {@link #putMergeResult(DocumentPart, MergeManagerResult)} for more information.
 *
 * @version $Id$
 * @since 11.5RC1
 */
public class MergeDocumentResult extends MergeManagerResult<DocumentModelBridge, Object>
{
    private final DocumentModelBridge currentDocument;

    private final DocumentModelBridge previousDocument;

    private final DocumentModelBridge nextDocument;

    private final Map<DocumentPart, MergeManagerResult> mergeResults;

    /**
     * Represents the different parts of a document that are merged.
     *
     * @version $Id$
     * @since 11.8RC1
     */
    public enum DocumentPart
    {
        /**
         * Merge of the title.
         */
        TITLE,

        /**
         * Merge of the content.
         */
        CONTENT,

        /**
         * Merge of the syntax.
         */
        SYNTAX,

        /**
         * Merge of the locale.
         * 
         * @since 11.10.11
         * @since 12.6.3
         * @since 12.8
         */
        DEFAULT_LOCALE,

        /**
         * Merge of the parent reference.
         */
        PARENT_REFERENCE,

        /**
         * Merge of the default template.
         */
        DEFAULT_TEMPLATE,

        /**
         * Merge of the hidden property.
         */
        HIDDEN,

        /**
         * Merge of the custom class property.
         */
        CUSTOM_CLASS,

        /**
         * Merge of the validation script.
         */
        VALIDATION_SCRIPT,

        /**
         * Merge of the xobjects.
         */
        XOBJECTS,

        /**
         * Merge of the xclass.
         */
        XCLASS,

        /**
         * Merge of the attachments.
         */
        ATTACHMENTS
    }

    /**
     * Default constructor.
     *
     * @param currentDocument the current document used for the merge.
     * @param previousDocument the previous document used for the merge.
     * @param nextDocument the next document used for the merge.
     * @since 11.8RC1
     */
    public MergeDocumentResult(DocumentModelBridge currentDocument, DocumentModelBridge previousDocument,
        DocumentModelBridge nextDocument)
    {
        this.currentDocument = currentDocument;
        this.previousDocument = previousDocument;
        this.nextDocument = nextDocument;
        this.mergeResults = new HashMap<>();
    }

    /**
     * @return the current document.
     */
    public DocumentModelBridge getCurrentDocument()
    {
        return this.currentDocument;
    }

    /**
     * @return the previous document.
     */
    public DocumentModelBridge getPreviousDocument()
    {
        return this.previousDocument;
    }

    /**
     * @return the next document.
     */
    public DocumentModelBridge getNextDocument()
    {
        return this.nextDocument;
    }

    /**
     * Stores the result of merging a part of the document.
     * This allows to easily retrieve the information of merging a specific portion of the document.
     * All logs and conflicts are automatically retrieved from the given {@link MergeManagerResult} and stored in the
     * current object.
     *
     * @param documentPart the part of the document that has been merged.
     * @param mergeManagerResult the result of the given merge operation.
     * @since 11.8RC1
     */
    public void putMergeResult(DocumentPart documentPart, MergeManagerResult mergeManagerResult)
    {
        if (!this.mergeResults.containsKey(documentPart)) {
            this.mergeResults.put(documentPart, mergeManagerResult);
            this.getLog().addAll(mergeManagerResult.getLog());
            this.getConflicts().addAll(mergeManagerResult.getConflicts());
            this.setModified(isModified() || mergeManagerResult.isModified());
        } else {
            throw new IllegalArgumentException(
                String.format("The merge result of document part [%s] has already been put.", documentPart.name()));
        }
    }

    /**
     * @param documentPart a document part that has been merged
     * @return the information result object of the part merge
     * @since 11.8RC1
     */
    public MergeManagerResult getMergeResult(DocumentPart documentPart)
    {
        return this.mergeResults.get(documentPart);
    }

    /**
     * Retrieve the conclicts that occurred during merging a specific part of the document.
     *
     * @param documentPart the part of the document that has been merged.
     * @return a list of conflicts or null. The parameter type of conflicts depends on the document part.
     * @since 11.8RC1
     */
    public List<Conflict<?>> getConflicts(DocumentPart documentPart)
    {
        if (this.mergeResults.containsKey(documentPart)) {
            return this.mergeResults.get(documentPart).getConflicts();
        }
        return null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MergeDocumentResult that = (MergeDocumentResult) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(currentDocument, that.currentDocument)
            .append(previousDocument, that.previousDocument)
            .append(nextDocument, that.nextDocument)
            .append(mergeResults, that.mergeResults)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(95, 23)
            .appendSuper(super.hashCode())
            .append(currentDocument)
            .append(previousDocument)
            .append(nextDocument)
            .append(mergeResults)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("currentDocument", currentDocument)
            .append("previousDocument", previousDocument)
            .append("nextDocument", nextDocument)
            .append("mergeResults", mergeResults)
            .toString();
    }
}
