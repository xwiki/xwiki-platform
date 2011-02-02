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
package org.xwiki.rendering.internal.parser.doxia;

import java.util.Map;

import org.apache.maven.doxia.sink.Sink;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.syntax.Syntax;

public class DoxiaGeneratorListener implements Listener
{
    private Sink sink;

    public DoxiaGeneratorListener(Sink sink)
    {
        this.sink = sink;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDocument(org.xwiki.rendering.listener.MetaData)
     * @since 3.0M2
     */
    public void beginDocument(MetaData metaData)
    {
        this.sink.body();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDocument(org.xwiki.rendering.listener.MetaData)
     * @since 3.0M2
     */
    public void endDocument(MetaData metaData)
    {
        this.sink.body_();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#beginGroup(Map)
     */
    public void beginGroup(Map<String, String> parameters)
    {
        // Do nothing since Doxia doesn't support groups
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#endGroup(Map)
     */
    public void endGroup(Map<String, String> parameters)
    {
        // Do nothing since Doxia doesn't support groups
    }

    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#beginFormat(Format, Map)
     */
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        // TODO: Handle parameters
        switch (format) {
            case BOLD:
                this.sink.bold();
                break;
            case ITALIC:
                this.sink.italic();
                break;
            case STRIKEDOUT:
                // TODO: Implement when we move to Doxia 1.0 beta 1.
                // See http://jira.codehaus.org/browse/DOXIA-204
                break;
            case UNDERLINED:
                // TODO: Implement when we move to Doxia 1.0 beta 1.
                // See http://jira.codehaus.org/browse/DOXIA-204
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#endFormat(Format, Map)
     */
    public void endFormat(Format format, Map<String, String> parameters)
    {
        // TODO: Handle parameters
        switch (format) {
            case BOLD:
                this.sink.bold_();
                break;
            case ITALIC:
                this.sink.italic_();
                break;
            case STRIKEDOUT:
                // TODO: Implement when we move to Doxia 1.0 beta 1.
                // See http://jira.codehaus.org/browse/DOXIA-204
                break;
            case UNDERLINED:
                // TODO: Implement when we move to Doxia 1.0 beta 1.
                // See http://jira.codehaus.org/browse/DOXIA-204
                break;
        }
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        if (listType == ListType.BULLETED) {
            this.sink.list();
        } else {
            // TODO: Handle other numerotations (Roman, etc)
            this.sink.numberedList(Sink.NUMBERING_DECIMAL);
        }
    }

    public void beginListItem()
    {
        this.sink.listItem();
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        // Don't do anything since Doxia doesn't have macro markers and anyway we shouldn't
        // do anything.
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        this.sink.paragraph();
    }

    public void beginSection(Map<String, String> parameters)
    {

    }

    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        if (level == HeaderLevel.LEVEL1) {
            this.sink.section1();
        } else if (level == HeaderLevel.LEVEL2) {
            this.sink.section2();
        } else if (level == HeaderLevel.LEVEL3) {
            this.sink.section3();
        } else if (level == HeaderLevel.LEVEL4) {
            this.sink.section4();
        } else if (level == HeaderLevel.LEVEL5) {
            this.sink.section5();
        } else if (level == HeaderLevel.LEVEL6) {
            // There's no level 6 in Doxia!
            this.sink.section5();
        }
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        if (listType == ListType.BULLETED) {
            this.sink.list_();
        } else {
            this.sink.numberedList_();
        }
    }

    public void endListItem()
    {
        this.sink.listItem_();
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        // Don't do anything since Doxia doesn't have macro markers and anyway we shouldn't
        // do anything.
    }

    public void endParagraph(Map<String, String> parameters)
    {
        this.sink.paragraph_();
    }

    public void endSection(Map<String, String> parameters)
    {

    }

    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        if (level == HeaderLevel.LEVEL1) {
            this.sink.section1_();
        } else if (level == HeaderLevel.LEVEL2) {
            this.sink.section2_();
        } else if (level == HeaderLevel.LEVEL3) {
            this.sink.section3_();
        } else if (level == HeaderLevel.LEVEL4) {
            this.sink.section4_();
        } else if (level == HeaderLevel.LEVEL5) {
            this.sink.section5_();
        } else if (level == HeaderLevel.LEVEL6) {
            // There's no level 6 in Doxia!
            this.sink.section5_();
        }
    }

    public void onMacro(String id, Map<String, String> parameters, String content, boolean isInline)
    {
        // Don't do anything since macros have already been transformed so this method
        // should not be called.
    }

    public void onNewLine()
    {
        // TODO: Decide when to generate a line break and when to generate a new line

        // Since there's no On NewLine event in Doxia we simply generate text
        this.sink.text("\n");
    }

    public void onSpace()
    {
        // Since there's no On Space event in Doxia we simply generate text
        this.sink.text(" ");
    }

    public void onSpecialSymbol(char symbol)
    {
        // Since there's no On Special Symbol event in Doxia we simply generate text
        this.sink.text("" + symbol);
    }

    public void onWord(String word)
    {
        this.sink.text(word);
    }

    public void onId(String name)
    {
        // TODO: Find out what to do...
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onRawText(String, Syntax)
     */
    public void onRawText(String text, Syntax syntax)
    {
        // TODO: Ensure this is correct. The problem is that Doxia doesn't seem to have a syntax
        // associated with the raw text so I'm not sure how the renderers (sink in Doxia language)
        // can decide whether to print it or not.
        this.sink.rawText(text);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onHorizontalLine(Map)
     */
    public void onHorizontalLine(Map<String, String> parameters)
    {
        // TODO: Handle parameters
        this.sink.horizontalRule();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        // TODO: Find what to do...
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatim(String, boolean, java.util.Map)
     */
    public void onVerbatim(String protectedString, Map<String, String> parameters, boolean isInline)
    {
        // TODO: Handle parameters
        this.sink.verbatim(false);
        this.sink.rawText(protectedString);
        this.sink.verbatim_();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList(java.util.Map)
     * @since 2.0RC1
     */
    public void beginDefinitionList(Map<String, String> parameters)
    {
        // TODO: Handle parameters
        this.sink.definitionList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList(java.util.Map)
     * @since 2.0RC1
     */
    public void endDefinitionList(Map<String, String> parameters)
    {
        // TODO: Handle parameters
        this.sink.definitionList_();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     * @since 1.6M2
     */
    public void beginDefinitionTerm()
    {
        this.sink.definedTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     * @since 1.6M2
     */
    public void beginDefinitionDescription()
    {
        this.sink.definition();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     * @since 1.6M2
     */
    public void endDefinitionTerm()
    {
        this.sink.definedTerm_();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     * @since 1.6M2
     */
    public void endDefinitionDescription()
    {
        this.sink.definition_();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        // TODO: Doxia doesn't seem to have support for quotation... Find out what to do...
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void endQuotation(Map<String, String> parameters)
    {
        // TODO: Doxia doesn't seem to have support for quotation... Find out what to do...
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     * @since 1.6M2
     */
    public void beginQuotationLine()
    {
        // TODO: Doxia doesn't seem to have support for quotation... Find out what to do...
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     * @since 1.6M2
     */
    public void endQuotationLine()
    {
        // TODO: Doxia doesn't seem to have support for quotation... Find out what to do...
    }

    public void beginTable(Map<String, String> parameters)
    {
        this.sink.table();
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        this.sink.tableCell();
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.sink.tableHeaderCell();
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        this.sink.tableRow();
    }

    public void endTable(Map<String, String> parameters)
    {
        this.sink.table_();
    }

    public void endTableCell(Map<String, String> parameters)
    {
        this.sink.tableCell_();
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.sink.tableHeaderCell_();
    }

    public void endTableRow(Map<String, String> parameters)
    {
        this.sink.tableRow_();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginLink(org.xwiki.rendering.listener.reference.ResourceReference , boolean,
     *      Map)
     * @since 2.5RC1
     */
    public void beginLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.sink.link(reference.getReference());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endLink(org.xwiki.rendering.listener.reference.ResourceReference , boolean, Map)
     * @since 2.5RC1
     */
    public void endLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.sink.link_();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onImage(org.xwiki.rendering.listener.reference.ResourceReference , boolean,
     *      java.util.Map)
     * @since 2.5RC1
     */
    public void onImage(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.sink.figure();
        // TODO: handle special XWiki format for image locations. How do we pass image bits to Doxia?
        // TODO: Handle parameters
        // TODO: Handle free standing URI (if supported by Doxia)
        this.sink.figureGraphics(reference.getReference());
        this.sink.figure_();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.Listener#beginMetaData(org.xwiki.rendering.listener.MetaData)
     * @since 3.0M2
     */
    public void beginMetaData(MetaData metadata)
    {
        // Doxia doesn't support the notion of metadata
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.Listener#endMetaData(org.xwiki.rendering.listener.MetaData)
     * @since 3.0M2
     */
    public void endMetaData(MetaData metadata)
    {
        // Doxia doesn't support the notion of metadata
    }
}
