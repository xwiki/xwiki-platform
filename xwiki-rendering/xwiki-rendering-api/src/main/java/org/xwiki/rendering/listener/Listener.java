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

import org.xwiki.rendering.listener.xml.XMLNode;

/**
 * Contains callback events called when a document has been parsed and when it needs to be modified or rendered. More
 * specifically when a document is parsed it generates an {@link org.xwiki.rendering.block.XDOM} object. That object has
 * a {@link org.xwiki.rendering.block.XDOM#traverse(Listener)} method that accepts a {@link Listener} object. For each
 * {@link org.xwiki.rendering.block.Block} element found in the document its
 * {@link org.xwiki.rendering.block.Block#traverse} method is called leading to the generation of events from this
 * interface.
 * <p>
 * Here's an example of usage:
 * </p>
 * 
 * <pre>
 * &lt;code&gt;
 *   XDOM dom = parser.parse(source);
 *   MyListener listener = new MyListener(...);
 *   dom.traverse(listener);
 *   // At this stage all events have been sent to MyListener. 
 * &lt;/code&gt;
 * </pre>
 * 
 * @version $Id$
 * @since 1.5M2
 */
public interface Listener extends LinkListener, ImageListener
{
    /**
     * Start of the document.
     * 
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     */
    void beginDocument(Map<String, String> parameters);

    /**
     * End of the document.
     * 
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     */
    void endDocument(Map<String, String> parameters);

    /**
     * Start a group of elements. Groups are used to allow using standalone elements in 
     * list items, table cells, etc. They can also be used to set parameters on a group of 
     * standalone elements.
     * 
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     * @since 1.8.3
     */
    void beginGroup(Map<String, String> parameters);

    /**
     * End of the group.
     * 
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     * @since 1.8.3
     */
    void endGroup(Map<String, String> parameters);

    /**
     * End of a text formatting block.
     * 
     * @param format the formatting type (bold, italic, etc)
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     * @see Format
     */
    void beginFormat(Format format, Map<String, String> parameters);

    /**
     * End of a text formatting block.
     * 
     * @param format the formatting type (bold, italic, etc)
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     * @see Format
     */
    void endFormat(Format format, Map<String, String> parameters);

    /**
     * Start of a paragraph.
     * 
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     */
    void beginParagraph(Map<String, String> parameters);

    /**
     * End of a paragraph.
     * 
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     */
    void endParagraph(Map<String, String> parameters);

    /**
     * Start of a list.
     * 
     * @param listType the type of list (bulleted, numbered, etc)
     * @param parameters a generic list of parameters for the list. Example: "style"/"background-color: blue"
     * @see ListType
     */
    void beginList(ListType listType, Map<String, String> parameters);

    /**
     * Start of a definition list. For example in HTML this is the equivalent of &lt;dl&gt;.
     * 
     * @since 1.6M2
     */
    void beginDefinitionList();

    /**
     * End of a list.
     * 
     * @param listType the type of list (bulleted, numbered, etc)
     * @param parameters a generic list of parameters for the list. Example: "style"/"background-color: blue"
     * @see ListType
     */
    void endList(ListType listType, Map<String, String> parameters);

    /**
     * End of a definition list. For example in HTML this is the equivalent of &lt;/dl&gt;.
     * 
     * @since 1.6M2
     */
    void endDefinitionList();

    /**
     * Start of a list item.
     */
    void beginListItem();

    /**
     * Start of a definition list term. For example in HTML this is the equivalent of &lt;dt&gt;.
     * 
     * @since 1.6M2
     */
    void beginDefinitionTerm();

    /**
     * Start of a definition list description. For example in HTML this is the equivalent of &lt;dd&gt;.
     * 
     * @since 1.6M2
     */
    void beginDefinitionDescription();

    /**
     * End of a list item.
     */
    void endListItem();

    /**
     * End of a definition list term. For example in HTML this is the equivalent of &lt;/dt&gt;.
     * 
     * @since 1.6M2
     */
    void endDefinitionTerm();

    /**
     * End of a definition list description. For example in HTML this is the equivalent of &lt;/dd&gt;.
     * 
     * @since 1.6M2
     */
    void endDefinitionDescription();

    /**
     * Start of a table.
     * 
     * @param parameters a generic list of parameters for the table.
     * @since 1.6M2
     */
    void beginTable(Map<String, String> parameters);

    /**
     * Start of a table row.
     * 
     * @param parameters a generic list of parameters for the table row.
     * @since 1.6M2
     */
    void beginTableRow(Map<String, String> parameters);

    /**
     * Start of a table cell.
     * 
     * @param parameters a generic list of parameters for the table cell.
     * @since 1.6M2
     */
    void beginTableCell(Map<String, String> parameters);

    /**
     * Start of a table head cell.
     * 
     * @param parameters a generic list of parameters for the table head cell.
     * @since 1.6M2
     */
    void beginTableHeadCell(Map<String, String> parameters);

    /**
     * End of a table.
     * 
     * @param parameters a generic list of parameters for the table.
     * @since 1.6M2
     */
    void endTable(Map<String, String> parameters);

    /**
     * End of a table row.
     * 
     * @param parameters a generic list of parameters for the table row.
     * @since 1.6M2
     */
    void endTableRow(Map<String, String> parameters);

    /**
     * End of a table cell.
     * 
     * @param parameters a generic list of parameters for the table cell.
     * @since 1.6M2
     */
    void endTableCell(Map<String, String> parameters);

    /**
     * End of a table head cell.
     * 
     * @param parameters a generic list of parameters for the table head cell.
     * @since 1.6M2
     */
    void endTableHeadCell(Map<String, String> parameters);

    /**
     * Start of a section.
     * 
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     * @see org.xwiki.rendering.listener.HeaderLevel
     */
    void beginSection(Map<String, String> parameters);

    /**
     * End of a section.
     * 
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     * @see org.xwiki.rendering.listener.HeaderLevel
     */
    void endSection(Map<String, String> parameters);

    /**
     * Start of a header.
     * 
     * @param level the header level (1, 2, 3, etc)
     * @param id the header unique identifier
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     * @see org.xwiki.rendering.listener.HeaderLevel
     * @since 1.9M1
     */
    void beginHeader(HeaderLevel level, String id, Map<String, String> parameters);

    /**
     * End of a header.
     * 
     * @param level the header level (1, 2, 3, etc)
     * @param id the header unique identifier
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     * @see org.xwiki.rendering.listener.HeaderLevel
     * @since 1.9M1
     */
    void endHeader(HeaderLevel level, String id, Map<String, String> parameters);

    /**
     * Start of an XML node. We use this type of event whenever there's no other equivalent event. For example for a
     * bold element we would use the {@link #beginFormat(Format)} instead of this event. However for example for an HTML
     * FORM there's no equivalent event. Note that these events are usually generated by a macro such as the HTML or
     * XHTML macros.
     * 
     * @param node the XML node (can be an {@link org.xwiki.rendering.listener.xml.XMLElement},
     *            {@link org.xwiki.rendering.listener.xml.XMLCData} and
     *            {@link org.xwiki.rendering.listener.xml.XMLComment})
     */
    void beginXMLNode(XMLNode node);

    /**
     * End of an XML node.
     * 
     * @param node the XML node (can be an {@link org.xwiki.rendering.listener.xml.XMLElement}, {@link XMLCData} and
     *            {@link org.xwiki.rendering.listener.xml.XMLComment})
     * @see #beginXMLElement(String, java.util.Map)
     */
    void endXMLNode(XMLNode node);

    /**
     * Start of marker containing a macro definition. This is a special that Macro Blocks emits when they are executed
     * so that it's possible to reconstruct the initial macro syntax even after Macros have been executed. This is used
     * for exemple by the WYSIWYG editor to let use see the result of executing a macro and still let them modify the
     * macro definition.
     * 
     * @param name the macro name
     * @param macroParameters the macro parameters
     * @param content the macro content
     * @param isInline if true the macro is located in a inline content (like paragraph, etc.)
     * @see #onMacro(String, java.util.Map, String, boolean)
     * @see #onInlineMacro(String, java.util.Map, String)
     */
    void beginMacroMarker(String name, Map<String, String> macroParameters, String content, boolean isInline);

    /**
     * End of marker containing a macro definition.
     * 
     * @param name the macro name
     * @param macroParameters the macro parameters
     * @param content the macro content
     * @param isInline if true the macro is located in a inline content (like paragraph, etc.)
     * @see #beginMacroMarker(String, java.util.Map, String, boolean)
     */
    void endMacroMarker(String name, Map<String, String> macroParameters, String content, boolean isInline);

    /**
     * Start of a quotation. There are one or several quotation lines inside a quotation block.
     * 
     * @param parameters a generic list of parameters for the quotation. Example: "style"/"background-color: blue"
     */
    void beginQuotation(Map<String, String> parameters);

    /**
     * End of a quotation.
     * 
     * @param parameters a generic list of parameters for the quotation. Example: "style"/"background-color: blue"
     */
    void endQuotation(Map<String, String> parameters);

    /**
     * Start of a quotation line. There can be several quotation lines in a quotation block.
     */
    void beginQuotationLine();

    /**
     * End of a quotation line.
     */
    void endQuotationLine();

    /**
     * A new line or line break (it's up to the renderers to decide if it should be outputted as a new line or as a line
     * break in the given syntax).
     */
    void onNewLine();

    /**
     * A {@link org.xwiki.rendering.macro.Macro} by itself on a line (ie not inside another Block).
     * 
     * @param name the macro name
     * @param macroParameters the macro parameters
     * @param content the macro content
     * @param isInline if true the macro is located in a inline content (like paragraph, etc.)
     * @since 1.6M2
     */
    void onMacro(String name, Map<String, String> macroParameters, String content, boolean isInline);

    /**
     * A word. Note that sentences ar broken into different events: word events, special symbols events, space events,
     * etc. This allows fine-grained actions for listeners.
     * 
     * @param word the word encountered
     */
    void onWord(String word);

    /**
     * A space.
     */
    void onSpace();

    /**
     * A special symbol ("*", "<", ">", "=", quote, etc). Any non alpha numeric character is a special symbol.
     * 
     * @param symbol the symbol encountered
     */
    void onSpecialSymbol(char symbol);

    /**
     * A reference/location in a page. In HTML for example this is called an Anchor. It allows pointing to that
     * location, for example in links. Note that there is no wiki syntax for this in general and it's often generated by
     * Macros (such as the TOC Macro).
     * 
     * @param name the location name.
     * @since 1.6M1
     */
    void onId(String name);

    /**
     * Represents an horizontal line.
     * 
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     * @since 1.6M1
     */
    void onHorizontalLine(Map<String, String> parameters);

    /**
     * Represents an empty line between 2 standalone Blocks. A standalone block is block that is not included in another
     * block. Standalone blocks are Paragraph, Standalone Macro, Lists, Table, etc.
     * 
     * @param count the number of empty lines between 2 standalone Blocks
     */
    void onEmptyLines(int count);

    /**
     * A portion of text.
     * 
     * @param protectedString the string to protected from rendering
     * @param isInline if true the text content is located in a inline content (like paragraph, etc.)
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     */
    void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters);
}
