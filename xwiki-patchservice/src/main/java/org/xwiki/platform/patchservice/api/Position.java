package org.xwiki.platform.patchservice.api;

public interface Position extends XmlSerializable
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
}
