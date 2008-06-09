package org.xwiki.platform.patchservice.api;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xpn.xwiki.XWikiException;

/**
 * Patch components can be serialized into XML documents, using the W3C DOM specification.
 * 
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public interface XmlSerializable
{
    /**
     * Serialize this object as a DOM Element that can be inserted in the given DOM Document.
     * 
     * @param doc A DOM Document used for generating a compatible Element. The document is not
     *            changed, as the constructed Element is just returned, not inserted in the
     *            document.
     * @return The object exported as a DOM Element.
     * @throws XWikiException If the object is not well defined.
     */
    Element toXml(Document doc) throws XWikiException;

    /**
     * Load the object from an XML.
     * 
     * @param e A DOM Element defining the object.
     * @throws XWikiException If the provided element is not a valid (or compatible) exported
     *             object.
     */
    void fromXml(Element e) throws XWikiException;
}
