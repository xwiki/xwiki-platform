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

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Store information about a merge operation of documents.
 *
 * @version $Id$
 * @since 11.5RC1
 */
public class MergeDocumentResult
{
    private final boolean withConflict;

    private final XWikiDocument currentDocument;

    private final XWikiDocument previousDocument;

    private final XWikiDocument nextDocument;

    private final XWikiDocument mergedDocument;

    /**
     * Default constructor.
     *
     * @param currentDocument the current document used for the merge.
     * @param previousDocument the previous document used for the merge.
     * @param nextDocument the next document used for the merge.
     * @param mergedDocument the merged document created.
     * @param withConflict true if some conflicts occured during the operation.
     */
    public MergeDocumentResult(XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, XWikiDocument mergedDocument, boolean withConflict)
    {
        this.currentDocument = currentDocument;
        this.previousDocument = previousDocument;
        this.nextDocument = nextDocument;
        this.mergedDocument = mergedDocument;
        this.withConflict = withConflict;
    }

    /**
     * @return true if the merge operation created conflicts.
     */
    public boolean isWithConflict()
    {
        return withConflict;
    }

    /**
     * @return the current document.
     */
    public XWikiDocument getCurrentDocument()
    {
        return this.currentDocument;
    }

    /**
     * @return the previous document.
     */
    public XWikiDocument getPreviousDocument()
    {
        return this.previousDocument;
    }

    /**
     * @return the next document.
     */
    public XWikiDocument getNextDocument()
    {
        return this.nextDocument;
    }

    /**
     * @return the merged document.
     */
    public XWikiDocument getMergedDocument()
    {
        return this.mergedDocument;
    }
}
