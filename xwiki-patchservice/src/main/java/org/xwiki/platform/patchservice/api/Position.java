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
 *
 */
package org.xwiki.platform.patchservice.api;

/**
 * A <tt>Position</tt> identifies a place inside a text field, where some changes occured.
 * 
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public interface Position extends XmlSerializable
{
    /**
     * Checks if the position is still valid for the given text.
     * 
     * @param text The text where this position is supposed to be defined.
     * @return True if the position is valid for the given text, false otherwise. Some implementations can return true
     *         even if the position does not match exactly, but can be identified in the text.
     */
    boolean checkPosition(String text);

    /**
     * Returns the text between the start of the text and this Position. Does not throw exceptions if the position is
     * not valid anymore, but gets the text between the start of the text and the position that best estimates the
     * original position.
     * 
     * @param text The text where this position is supposed to be defined.
     * @return The portion of text before this position, or <tt>null</tt> if the position is not valid.
     */
    String getTextBeforePosition(String text);

    /**
     * Returns the text between this Position and the end of the text. Does not throw exceptions if the position is not
     * valid anymore, but gets the text between the position that best estimates the original position, and the end of
     * the text.
     * 
     * @param text The text where this position is supposed to be defined.
     * @return The portion of text after this position, or <tt>null</tt> if the position is not valid.
     */
    String getTextAfterPosition(String text);
}
