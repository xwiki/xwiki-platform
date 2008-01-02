package org.xwiki.platform.patchservice.api;

import java.util.List;

import org.w3c.dom.Element;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public interface PatchSet
{
    List getVersionRange();

    List getPatches();

    Element toXml() throws XWikiException;

    void fromXml(Element e) throws XWikiException;

    /**
     * Apply this patch set on a document.
     * 
     * @param doc The document being patched.
     * @return The document with all the patches in this patch set applied.
     */
    void apply(XWikiDocument doc) throws XWikiException;
}
