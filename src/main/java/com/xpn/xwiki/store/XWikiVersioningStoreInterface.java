package com.xpn.xwiki.store;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.rcs.Archive;

public interface XWikiVersioningStoreInterface {
    public void loadXWikiDocArchive(XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context) throws XWikiException;
    public void saveXWikiDocArchive(XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context) throws XWikiException;
    public void updateXWikiDocArchive(XWikiDocument doc, String text, boolean bTransaction, XWikiContext context) throws XWikiException;
    public Version[] getXWikiDocVersions(XWikiDocument doc, XWikiContext context) throws XWikiException;
    public Archive getXWikiDocRCSArchive(XWikiDocument doc, XWikiContext context) throws XWikiException;
    public String getXWikiDocArchive(XWikiDocument doc, XWikiContext context) throws XWikiException;
    public XWikiDocument loadXWikiDoc(XWikiDocument doc, String version, XWikiContext context) throws XWikiException;
    public void resetRCSArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException;
}
