package org.xwiki.platform.patchservice.api;

import com.xpn.xwiki.doc.XWikiDocument;

public interface PatchCreator
{
    Patch getPatch(XWikiDocument oldDoc, XWikiDocument newDoc);
}
