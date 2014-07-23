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

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
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
}
