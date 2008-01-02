package org.xwiki.platform.patchservice.api;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public interface Patch
{
    String getSpecVersion();

    PatchId getId();

    Originator getOriginator();

    String getDescription();

    List getOperations();

    /*
     * We shouldn't make our own hash/sign methods, but instead use Standard W3C XML signatures. See
     * http://santuario.apache.org/
     */
    // String getHash();
    //
    // boolean checkHash();
    //
    // String getSignature();
    //
    // boolean checkSignature();
    Element toXml() throws XWikiException;

    Element toXml(Document doc) throws XWikiException;

    void fromXml(Element e) throws XWikiException;

    /**
     * Apply this patch on a document.
     * 
     * @param doc The document being patched.
     * @throws XWikiException if the patch cannot be applied on the document.
     */
    void apply(XWikiDocument doc) throws XWikiException;
}
