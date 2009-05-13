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
package com.xpn.xwiki.store;

import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.ComponentRole;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeContent;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

/**
 * Interface for manipulate document history.
 * 
 * @version $Id$
 */
@ComponentRole
public interface XWikiVersioningStoreInterface
{
    void loadXWikiDocArchive(XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context)
        throws XWikiException;

    void saveXWikiDocArchive(XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context)
        throws XWikiException;

    void updateXWikiDocArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException;

    Version[] getXWikiDocVersions(XWikiDocument doc, XWikiContext context) throws XWikiException;

    XWikiDocument loadXWikiDoc(XWikiDocument doc, String version, XWikiContext context) throws XWikiException;

    void resetRCSArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException;

    XWikiDocumentArchive getXWikiDocumentArchive(XWikiDocument doc, XWikiContext context) throws XWikiException;

    /**
     * Delete all document history.
     * 
     * @param doc - deleted document
     */
    void deleteArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException;

    /**
     * Load {@link XWikiRCSNodeContent} by demand. Used in {@link XWikiRCSNodeInfo#getContent(XWikiContext)}
     * 
     * @return loaded rcs node content
     * @param id = {@link XWikiRCSNodeContent#getId()}
     */
    XWikiRCSNodeContent loadRCSNodeContent(XWikiRCSNodeId id, boolean bTransaction, XWikiContext context)
        throws XWikiException;
}
