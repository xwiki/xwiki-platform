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
 */
package org.xwiki.rendering.listener;

import java.util.Map;

/**
 * @version $Id$
 * @since 1.5M2
 */
public interface Listener
{
    /**
     * First event generated representing the start of the document.
     */
    void beginDocument();

    /**
     * Last event generated representing the end of the document.
     */
    void endDocument();

    /**
     * Represents the start of a text formatting block (bold, italic, etc).
     * @param format the formatting type
     * @see Format
     */
    void beginFormat(Format format);

    /**
     * Represents the end of a text formatting block (bold, italic, etc).
     * @param format the formatting type
     * @see Format
     */
    void endFormat(Format format);

    void beginParagraph();

    void endParagraph();

    void beginList(ListType listType);

    void endList(ListType listType);

    void beginListItem();

    void endListItem();

    void beginSection(SectionLevel level);

    void endSection(SectionLevel level);

    void beginXMLElement(String name, Map<String, String> attributes);

    void endXMLElement(String name, Map<String, String> attributes);

    /**
     * A special event that Macro Blocks emits when they are executed so that it's possible to reconstruct the initial
     * syntax even after Macros have been executed.
     */
    void beginMacroMarker(String name, Map<String, String> parameters, String content);

    /**
     * A special event that Macro Blocks emits when they are executed so that it's possible to reconstruct the initial
     * syntax even after Macros have been executed.
     */
    void endMacroMarker(String name, Map<String, String> parameters, String content);

    /**
     * Represents an explicit line break specified in the wiki syntax. For example for XWiki this would be "\\". Note
     * that this is different from a new line which is triggered when the new line character is found ("\n") and which
     * generates an onNewLine() event.
     */
    void onLineBreak();

    /**
     * Represents an implicit new line triggered when the new line character is found ("\n"). Note that this is
     * different from a line break which is explicitely specified in wiki syntax, and which generates a onLineBreak()
     * event.
     */
    void onNewLine();

    void onLink(Link link);

    void onMacro(String name, Map<String, String> parameters, String content);

    void onWord(String word);

    void onSpace();

    void onSpecialSymbol(String symbol);

    /**
     * Represents a string that is escaped, ie which contains special characters that should not be transformed.
     * 
     * @param escapedString the string to keep as is
     * @since 1.5RC1
     */
    void onEscape(String escapedString);

    /**
     * Represents a reference/location in a page. In HTML for example this is called an Anchor. It allows pointing
     * to that location, for example in links.
     *
     * @param name the location name.
     */
    void onId(String name);

    /**
     * Represents an horizontal line.
     */
    void onHorizontalLine();

}
