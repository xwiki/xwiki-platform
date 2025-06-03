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

import java.util.List;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.diff.Conflict;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.util.Programming;

/**
 * Represents the result of a 3-way merge on documents performed in the {@link MergeScriptService}.
 *
 * @version $Id$
 * @since 11.8RC1
 */
public class MergeDocumentResultScript
{
    private final MergeDocumentResult mergeDocumentResult;

    private final Document previousDocument;

    private final Document currentDocument;

    private final Document nextDocument;

    private final Document mergedDocument;

    private boolean hasProgramming;

    /**
     * Default constructor.
     *
     * @param mergeDocumentResult the actual result obtained through
     *          {@link MergeManager#mergeDocument(DocumentModelBridge, DocumentModelBridge, DocumentModelBridge,
     *              MergeConfiguration)}.
     * @param context the current context.
     */
    public MergeDocumentResultScript(MergeDocumentResult mergeDocumentResult, XWikiContext context)
    {
        this(mergeDocumentResult, context, false);
    }

    /**
     * Default constructor.
     *
     * @param mergeDocumentResult the actual result obtained through
     *          {@link MergeManager#mergeDocument(DocumentModelBridge, DocumentModelBridge, DocumentModelBridge,
     *              MergeConfiguration)}.
     * @param context the current context.
     * @param hasProgramming {@code true} if the user has programming right (See {@link #getMergeDocumentResult()}).
     * @since 15.5RC1
     * @since 14.10.12
     */
    public MergeDocumentResultScript(MergeDocumentResult mergeDocumentResult, XWikiContext context,
        boolean hasProgramming)
    {
        this.mergeDocumentResult = mergeDocumentResult;

        this.previousDocument = ((XWikiDocument) mergeDocumentResult.getPreviousDocument()).newDocument(context);
        this.currentDocument = ((XWikiDocument) mergeDocumentResult.getCurrentDocument()).newDocument(context);
        this.nextDocument = ((XWikiDocument) mergeDocumentResult.getNextDocument()).newDocument(context);
        this.mergedDocument = ((XWikiDocument) mergeDocumentResult.getMergeResult()).newDocument(context);
        this.hasProgramming = hasProgramming;
    }

    /**
     * @return the previous document used for the merge.
     */
    public Document getPreviousDocument()
    {
        return previousDocument;
    }

    /**
     * @return the current document used for the merge.
     */
    public Document getCurrentDocument()
    {
        return currentDocument;
    }

    /**
     * @return the new document used for the merge.
     */
    public Document getNextDocument()
    {
        return nextDocument;
    }

    /**
     * @return the merged document obtained after the merge.
     */
    public Document getMergedDocument()
    {
        return mergedDocument;
    }

    /**
     * @return the conflicts that occurred when merging the content of document.
     */
    public List<Conflict<?>> getContentConflicts()
    {
        return this.mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.CONTENT);
    }

    /**
     * @return all the conflicts that occurred when merging the whole document.
     */
    public List<Conflict<Object>> getAllConflicts()
    {
        return this.mergeDocumentResult.getConflicts();
    }

    /**
     * @return the actual {@link MergeDocumentResult} only if the author of the script has programming rights, else
     * return {@code null}.
     * @since 15.5RC1
     * @since 14.10.12
     */
    @Programming
    public MergeDocumentResult getMergeDocumentResult()
    {
        if (this.hasProgramming) {
            return this.mergeDocumentResult;
        } else {
            return null;
        }
    }

    /**
     * @return {@code true} if the merge contains at least one content conflict but only contains conflicts related to
     * the content.
     * @since 14.10.12
     * @since 15.5RC1
     */
    public boolean hasOnlyContentConflicts()
    {
        return !getContentConflicts().isEmpty()
            && getContentConflicts().size() == this.mergeDocumentResult.getConflictsNumber();
    }
}
