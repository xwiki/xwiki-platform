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
package com.xpn.xwiki.api;

import java.util.List;

import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.stats.impl.DocumentStats;

/**
 * Add a backward compatibility layer to the {@link Document} class.
 * 
 * @version $Id$
 */
public privileged aspect DocumentCompatibilityAspect
{
    /**
     * @deprecated replaced by {@link Document#getCurrentMonthSpaceStats(String)} since 2.3M1
     */
    @Deprecated
    public DocumentStats Document.getCurrentMonthWebStats(String action)
    {
        return this.getCurrentMonthSpaceStats(action);
    }

    /**
     * Get the name of the space of the document for example if the fullName of a document is "MySpace.Mydoc", the name
     * is MySpace.
     * 
     * @return The name of the space of the document.
     * @deprecated use {@link #getSpace()} instead of this function.
     */
    @Deprecated
    public String Document.getWeb()
    {
        return this.doc.getSpace();
    }

    /**
     * @deprecated use {@link #rename(String)} instead
     */
    @Deprecated
    public void Document.renameDocument(String newDocumentName) throws XWikiException
    {
        rename(newDocumentName);
    }

    /**
     * @deprecated use {@link #rename(String, java.util.List)} instead
     */
    @Deprecated
    public void Document.renameDocument(String newDocumentName, List<String> backlinkDocumentNames) throws XWikiException
    {
        rename(newDocumentName, backlinkDocumentNames);
    }

    /**
     * Rename the current document and all the backlinks leading to it. Will also change parent field in all documents
     * which list the document we are renaming as their parent. See
     * {@link #rename(String, java.util.List, java.util.List)} for more details.
     *
     * @param newDocumentName the new document name. If the space is not specified then defaults to the current space.
     * @throws XWikiException in case of an error
     * @deprecated since 12.0RC1. Use {@link #rename(DocumentReference)} instead.
     */
    @Deprecated
    public void Document.rename(String newDocumentName) throws XWikiException
    {
        rename(this.getCurrentMixedDocumentReferenceResolver().resolve(newDocumentName));
    }
}
