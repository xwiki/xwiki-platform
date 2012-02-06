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
package org.xwiki.wikistream.internal.output.xml;

import java.io.StringWriter;
import java.util.Map;

import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.wikistream.listener.Listener;
import org.xwiki.wikistream.structure.User;

import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

/**
 * A Basic XML Serializer implementation.
 * 
 * @version $Id$
 */
public class WikiXMLListener implements Listener
{

    private StringWriter stringWriter;

    private PrettyPrintWriter writer;

    public WikiXMLListener()
    {
        stringWriter = new StringWriter();
        writer = new PrettyPrintWriter(stringWriter);
    }

    public String getXMLString()
    {
        return stringWriter.toString();
    }

    private void writeSimpleNode(String tag, String value)
    {
        writer.startNode(tag);
        writer.setValue(value);
        writer.endNode();
    }

    private void writeNodeWithMetadata(String tag, MetaData metadata)
    {
        writeBeginNodeWithMetadata(tag, metadata);
        writer.endNode();
    }

    private void writeBeginNodeWithMetadata(String tag, MetaData metadata)
    {
        writer.startNode(tag);
        // TODO Print MetaData
    }

    private void writeNodeWithParameters(String tag, Map<String, String> parameters)
    {
        writeBeginNodeWithParameters(tag, parameters);
        writer.endNode();
    }

    private void writeBeginNodeWithParameters(String tag, Map<String, String> parameters)
    {
        writer.startNode(tag);
        // TODO Print Parameters
    }

    @Override
    public void onAuthor(User author)
    {
        // FIXME add more author metadata
        writeSimpleNode("author", author.toString());
    }

    @Override
    public void onComment(MetaData metadata)
    {
        writeNodeWithMetadata("comment", metadata);
    }

    @Override
    public void onTitle(String title)
    {
        writeSimpleNode("title", title);
    }

    @Override
    public void beginRevision(MetaData metadata)
    {
        writeBeginNodeWithMetadata("revision", metadata);
    }

    @Override
    public void endRevision(MetaData metadata)
    {
        writer.endNode();
    }

    @Override
    public void beginDocument(MetaData metadata)
    {
        writeBeginNodeWithMetadata("document", metadata);
    }

    @Override
    public void endDocument(MetaData metaData)
    {
        writer.endNode();

    }

    @Override
    public void beginMetaData(MetaData metadata)
    {
        writeBeginNodeWithMetadata("metadata", metadata);
    }

    @Override
    public void endMetaData(MetaData metadata)
    {
        writer.endNode();
    }

    @Override
    public void beginGroup(Map<String, String> parameters)
    {
        writeBeginNodeWithParameters("group", parameters);
    }

    @Override
    public void endGroup(Map<String, String> parameters)
    {
        writer.endNode();
    }

    @Override
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        // TODO Handle format tag
    }

    @Override
    public void endFormat(Format format, Map<String, String> parameters)
    {
        writer.endNode();
    }

    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        writer.endNode();
    }

    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        writer.endNode();
    }

    @Override
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        // TODO Handle List
    }

    @Override
    public void beginDefinitionList(Map<String, String> parameters)
    {
        // TODO Handle Definition list
    }

    @Override
    public void endList(ListType listType, Map<String, String> parameters)
    {
        writer.endNode();
    }

    @Override
    public void endDefinitionList(Map<String, String> parameters)
    {
        writer.endNode();
    }

    @Override
    public void beginListItem()
    {
        writer.startNode("li");
    }

    @Override
    public void beginDefinitionTerm()
    {
        writer.startNode("def");
    }

    @Override
    public void beginDefinitionDescription()
    {
        writer.startNode("def-description");
    }

    @Override
    public void endListItem()
    {
        writer.endNode();
    }

    @Override
    public void endDefinitionTerm()
    {
        writer.endNode();
    }

    @Override
    public void endDefinitionDescription()
    {
        writer.endNode();
    }

    @Override
    public void beginTable(Map<String, String> parameters)
    {
        writeBeginNodeWithParameters("table", parameters);
    }

    @Override
    public void beginTableRow(Map<String, String> parameters)
    {
        writeBeginNodeWithParameters("tr", parameters);
    }

    @Override
    public void beginTableCell(Map<String, String> parameters)
    {
        writeBeginNodeWithParameters("tr", parameters);
    }

    @Override
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        // TODO Auto-generated method stub
        writer.endNode();
    }

    @Override
    public void endTable(Map<String, String> parameters)
    {
        // TODO Auto-generated method stub
        writer.endNode();
    }

    @Override
    public void endTableRow(Map<String, String> parameters)
    {
        // TODO Auto-generated method stub
        writer.endNode();
    }

    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        // TODO Auto-generated method stub
        writer.endNode();
    }

    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        // TODO Auto-generated method stub
        writer.endNode();
    }

    @Override
    public void beginSection(Map<String, String> parameters)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endSection(Map<String, String> parameters)
    {
        // TODO Auto-generated method stub
        writer.endNode();
    }

    @Override
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        // TODO Auto-generated method stub
        writer.endNode();
    }

    @Override
    public void beginMacroMarker(String name, Map<String, String> macroParameters, String content, boolean isInline)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endMacroMarker(String name, Map<String, String> macroParameters, String content, boolean isInline)
    {
        // TODO Auto-generated method stub
        writer.endNode();
    }

    @Override
    public void beginQuotation(Map<String, String> parameters)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endQuotation(Map<String, String> parameters)
    {
        // TODO Auto-generated method stub
        writer.endNode();
    }

    @Override
    public void beginQuotationLine()
    {
        // TODO Auto-generated method stub
        writer.startNode("quotation");
    }

    @Override
    public void endQuotationLine()
    {
        // TODO Auto-generated method stub
        writer.endNode();
    }

    @Override
    public void onNewLine()
    {
        stringWriter.append('\n');
    }

    @Override
    public void onMacro(String id, Map<String, String> macroParameters, String content, boolean isInline)
    {
        writer.startNode("macro");
        writer.addAttribute("id", id);
        writer.addAttribute("inline", isInline ? "true" : "false");
        writer.setValue(content);
        // TODO Add macro parameters.
        writer.endNode();
    }

    @Override
    public void onWord(String word)
    {
        // TODO Auto-generated method stub
        writeSimpleNode("newline", "");
    }

    @Override
    public void onSpace()
    {
        // TODO Auto-generated method stub
        // writeSimpleNode("space", "");
        stringWriter.append(" ");
    }

    @Override
    public void onSpecialSymbol(char symbol)
    {
        stringWriter.append(symbol);
    }

    @Override
    public void onId(String name)
    {
        writeSimpleNode("id", name);
    }

    @Override
    public void onHorizontalLine(Map<String, String> parameters)
    {
        stringWriter.append("<hr/>");
        // TODO Add parameters
    }

    @Override
    public void onEmptyLines(int count)
    {
        // TODO Auto-generated method stub
        for (int i = 0; i < count; i++) {
            stringWriter.append('\n');
        }
    }

    @Override
    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        // TODO Handle verbatim

    }

    @Override
    public void onRawText(String rawContent, Syntax syntax)
    {
        // TODO Check for the syntax
        stringWriter.append(rawContent);

    }

    @Override
    public void beginLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // TODO Refactor it to log the attributes better.
        writer.startNode("image");
        writer.addAttribute("isFreeStandingURI", isFreeStandingURI ? "true" : "false");

        // TODO Add parameters

    }

    @Override
    public void endLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        writer.endNode();
    }

    @Override
    public void onImage(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // TODO Refactor it to log the attributes better.
        writer.startNode("image");
        writer.addAttribute("isFreeStandingURI", isFreeStandingURI ? "true" : "false");

        // TODO Add parameters

        writer.endNode();

    }

    @Override
    public void beginSpace(String spaceName)
    {
        writeSimpleNode("space", spaceName);
    }

    @Override
    public void endSpace(String spaceName)
    {
        // TODO Auto-generated method stub
        writer.endNode();
    }

    @Override
    public void beginMetadata(MetaData metadata)
    {
        writeBeginNodeWithMetadata("metadata", metadata);
    }

    @Override
    public void endMetadata(MetaData metadata)
    {
        // TODO Auto-generated method stub
        writer.endNode();
    }

    @Override
    public void beginWiki(String wikiName)
    {
        writeSimpleNode("wiki", wikiName);

    }

    @Override
    public void endWiki(String wikiName)
    {
        writer.endNode();
    }

}
