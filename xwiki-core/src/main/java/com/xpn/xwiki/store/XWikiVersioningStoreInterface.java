package com.xpn.xwiki.store;

import org.suigeneris.jrcs.rcs.Version;

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
