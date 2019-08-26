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
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;

/**
 * Represents the result of a 3-way merge on documents performed in the {@link MergeScriptService}.
 *
 * @version $Id$
 * @since 11.8RC1
 */
@Unstable
public class MergeDocumentResultScript
{
    private final MergeDocumentResult mergeDocumentResult;

    private final Document previousDocument;

    private final Document currentDocument;

    private final Document nextDocument;

    private final Document mergedDocument;

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
        this.mergeDocumentResult = mergeDocumentResult;

        this.previousDocument = ((XWikiDocument) mergeDocumentResult.getPreviousDocument()).newDocument(context);
        this.currentDocument = ((XWikiDocument) mergeDocumentResult.getCurrentDocument()).newDocument(context);
        this.nextDocument = ((XWikiDocument) mergeDocumentResult.getNextDocument()).newDocument(context);
        this.mergedDocument = ((XWikiDocument) mergeDocumentResult.getMergeResult()).newDocument(context);
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
}
