package org.xwiki.platform.patchservice.api;

import java.util.List;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public interface PatchSet extends XmlSerializable
{
    List getVersionRange();

    List getPatches();

    /**
     * Apply this patch set on a document.
     * 
     * @param doc The document being patched.
     * @return The document with all the patches in this patch set applied.
     */
    void apply(XWikiDocument doc) throws XWikiException;
}
