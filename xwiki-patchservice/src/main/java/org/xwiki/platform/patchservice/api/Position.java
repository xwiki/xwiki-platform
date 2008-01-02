package org.xwiki.platform.patchservice.api;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xpn.xwiki.XWikiException;

public interface Position
{
    /**
     * Checks if the position is still valid for the given text.
     * 
     * @param text The text where this position is supposed to be defined.
     * @return True if the position is valid for the given text, false otherwise. Some
     *         implementations can return true even if the position does not match exactly, but can
     *         be identified in the text.
     */
    boolean checkPosition(String text);

    /**
     * Returns the text between the start of the text and this Position.
     * 
     * @param text The text where this position is supposed to be defined.
     * @return The portion of text before this position, or <tt>null</tt> if the position is not
     *         valid.
     */
    String getTextBeforePosition(String text);

    /**
     * Returns the text between this Position and the end of the text.
     * 
     * @param text The text where this position is supposed to be defined.
     * @return The portion of text after this position, or <tt>null</tt> if the position is not
     *         valid.
     */
    String getTextAfterPosition(String text);

    /**
     * Serialize this position as a DOM Element that can be inserted in the given DOM Document.
     * 
     * @param doc A DOM Document used for generating a compatible Element. The document is not
     *            changed, as the constructed Element is just returned, not inserted in the
     *            document.
     * @return The operation exported as a DOM Element.
     * @throws XWikiException If the Operation is not well defined.
     */
    Element toXml(Document doc) throws XWikiException;

    /**
     * Load the operation from an XML.
     * 
     * @param e A DOM Element defining the Operation.
     * @throws XWikiException If the provided element is not a valid (or compatible) exported
     *             Operation.
     */
    void fromXml(Element e) throws XWikiException;
}
