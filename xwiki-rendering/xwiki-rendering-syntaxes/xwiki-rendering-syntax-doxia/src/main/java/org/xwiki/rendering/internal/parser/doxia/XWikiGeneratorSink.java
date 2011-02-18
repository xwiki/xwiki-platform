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

import java.io.StringReader;
import java.util.Map;
import java.util.Stack;

import org.apache.maven.doxia.logging.Log;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.xwiki.rendering.listener.CompositeListener;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.InlineFilterListener;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.util.IdGenerator;

/**
 * Transforms Doxia events into XWiki Rendering events.
 * 
 * @version $Id$
 * @since 2.1RC1
 */
public class XWikiGeneratorSink implements Sink
{
    private Stack<Listener> listener = new Stack<Listener>();

    private Stack<Object> parameters = new Stack<Object>();

    private ResourceReferenceParser linkReferenceParser;

    private IdGenerator idGenerator;

    private PrintRendererFactory plainRendererFactory;

    private StreamParser plainParser;

    private int lineBreaks = 0;

    private int inlineDepth = 0;

    private Syntax syntax;

    private MetaData documentMetadata;

    /**
     * @since 3.0M3
     */
    public XWikiGeneratorSink(Listener listener, ResourceReferenceParser linkReferenceParser,
        PrintRendererFactory plainRendererFactory, IdGenerator idGenerator, StreamParser plainParser, Syntax syntax)
    {
        pushListener(listener);

        this.linkReferenceParser = linkReferenceParser;
        this.idGenerator = idGenerator != null ? idGenerator : new IdGenerator();
        this.plainRendererFactory = plainRendererFactory;
        this.plainParser = plainParser;
        this.syntax = syntax;
        this.documentMetadata = new MetaData();
        this.documentMetadata.addMetaData(MetaData.SYNTAX, this.syntax);
    }

    public Listener getListener()
    {
        return this.listener.peek();
    }

    private Listener pushListener(Listener listener)
    {
        return this.listener.push(listener);
    }

    private Listener popListener()
    {
        return this.listener.pop();
    }

    private boolean isInline()
    {
        return this.inlineDepth > 0;
    }

    private void flushEmptyLines()
    {
        if (this.lineBreaks > 0) {
            if (isInline()) {
                for (int i = 0; i < this.lineBreaks; ++i) {
                    getListener().onNewLine();
                }
            } else {
                if (this.lineBreaks >= 2) {
                    getListener().onEmptyLines(lineBreaks - 1);
                } else {
                    getListener().onNewLine();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#flush()
     */
    public void flush()
    {
        flushEmptyLines();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#enableLogging(Log)
     */
    public void enableLogging(Log arg0)
    {
        // Not used.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#anchor(String, SinkEventAttributes)
     */
    public void anchor(String name, SinkEventAttributes attributes)
    {
        flushEmptyLines();

        getListener().onId(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#anchor(String)
     */
    public void anchor(String name)
    {
        anchor(name, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#anchor_()
     */
    public void anchor_()
    {
        // Nothing to do since for XWiki anchors don't have children and thus the XWiki Block is generated in the Sink
        // anchor start event
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#author(SinkEventAttributes)
     */
    public void author(SinkEventAttributes attributes)
    {
        // XWiki's Listener model doesn't support authors. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#author()
     */
    public void author()
    {
        // XWiki's Listener model doesn't support authors. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#author_()
     */
    public void author_()
    {
        // XWiki's Listener model doesn't support authors. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#body(SinkEventAttributes)
     */
    public void body(SinkEventAttributes attributes)
    {
        body();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#body()
     */
    public void body()
    {
        getListener().beginDocument(this.documentMetadata);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#body_()
     */
    public void body_()
    {
        getListener().endDocument(this.documentMetadata);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#bold()
     */
    public void bold()
    {
        flushEmptyLines();

        getListener().beginFormat(Format.BOLD, Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#bold_()
     */
    public void bold_()
    {
        flushEmptyLines();

        getListener().endFormat(Format.BOLD, Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#close()
     */
    public void close()
    {
        // Not used.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#comment(String)
     */
    public void comment(String comment)
    {
        // TODO: Not supported yet by the XDOM.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#date(SinkEventAttributes)
     */
    public void date(SinkEventAttributes attributes)
    {
        // XWiki's Listener model doesn't support dates. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#date()
     */
    public void date()
    {
        // XWiki's Listener model doesn't support dates. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#date_()
     */
    public void date_()
    {
        // XWiki's Listener model doesn't support dates. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#definedTerm(SinkEventAttributes)
     */
    public void definedTerm(SinkEventAttributes attributes)
    {
        getListener().beginDefinitionTerm();

        ++this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#definedTerm()
     */
    public void definedTerm()
    {
        definedTerm(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#definedTerm_()
     */
    public void definedTerm_()
    {
        flushEmptyLines();

        // Limitation: XWiki doesn't use parameters on this Block.
        getListener().endDefinitionTerm();

        --this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#definition(SinkEventAttributes)
     */
    public void definition(SinkEventAttributes attributes)
    {
        getListener().beginDefinitionDescription();

        ++this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#definition()
     */
    public void definition()
    {
        definition(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#definition()
     */
    public void definition_()
    {
        flushEmptyLines();

        // Limitation: XWiki doesn't use parameters on this Block.
        getListener().endDefinitionDescription();

        --this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#definitionList(SinkEventAttributes)
     */
    public void definitionList(SinkEventAttributes attributes)
    {
        flushEmptyLines();

        getListener().beginDefinitionList(Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#definitionList()
     */
    public void definitionList()
    {
        definitionList(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#definitionList_()
     */
    public void definitionList_()
    {
        // TODO: Handle parameters
        getListener().endDefinitionList(Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#definitionListItem(SinkEventAttributes)
     */
    public void definitionListItem(SinkEventAttributes attributes)
    {
        // Nothing to do since for XWiki the definition list items are the definition term/descriptions.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#definitionListItem()
     */
    public void definitionListItem()
    {
        // Nothing to do since for XWiki the definition list items are the definition term/descriptions.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#definitionListItem_()
     */
    public void definitionListItem_()
    {
        // Nothing to do since for XWiki the definition list items are the definition term/descriptions.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#figure(SinkEventAttributes)
     */
    public void figure(SinkEventAttributes attributes)
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#figure()
     */
    public void figure()
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#figure_()
     */
    public void figure_()
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#figureCaption(SinkEventAttributes)
     */
    public void figureCaption(SinkEventAttributes attributes)
    {
        // TODO: Handle caption as parameters in the future
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#figureCaption()
     */
    public void figureCaption()
    {
        figureCaption(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#figureCaption_()
     */
    public void figureCaption_()
    {
        // TODO: Handle caption as parameters in the future
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#figureGraphics(String, SinkEventAttributes)
     */
    public void figureGraphics(String source, SinkEventAttributes attributes)
    {
        flushEmptyLines();

        // TODO: Handle image to attachments. For now we only handle URLs.
        getListener().onImage(new ResourceReference(source, ResourceType.URL), false, Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#figureGraphics(String)
     */
    public void figureGraphics(String source)
    {
        figureGraphics(source, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#head(SinkEventAttributes)
     */
    public void head(SinkEventAttributes sinkEventAttributes)
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#head()
     */
    public void head()
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#head_()
     */
    public void head_()
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#horizontalRule(SinkEventAttributes)
     */
    public void horizontalRule(SinkEventAttributes attributes)
    {
        flushEmptyLines();

        // TODO: Handle parameters
        getListener().onHorizontalLine(Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#horizontalRule()
     */
    public void horizontalRule()
    {
        horizontalRule(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#italic()
     */
    public void italic()
    {
        flushEmptyLines();

        getListener().beginFormat(Format.ITALIC, Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#italic_()
     */
    public void italic_()
    {
        flushEmptyLines();

        getListener().endFormat(Format.ITALIC, Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#lineBreak(SinkEventAttributes)
     */
    public void lineBreak(SinkEventAttributes attributes)
    {
        ++this.lineBreaks;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#lineBreak()
     */
    public void lineBreak()
    {
        lineBreak(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#link(String, SinkEventAttributes)
     */
    public void link(String name, SinkEventAttributes attributes)
    {
        flushEmptyLines();

        ResourceReference resourceReference = this.linkReferenceParser.parse(name);

        getListener().beginLink(resourceReference, false, Listener.EMPTY_PARAMETERS);

        this.parameters.push(resourceReference);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#link(String)
     */
    public void link(String name)
    {
        link(name, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#link_()
     */
    public void link_()
    {
        flushEmptyLines();

        getListener().endLink((ResourceReference) this.parameters.pop(), false, Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#list()
     */
    public void list(SinkEventAttributes attributes)
    {
        flushEmptyLines();

        // TODO: Handle parameters
        getListener().beginList(ListType.BULLETED, Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#list()
     */
    public void list()
    {
        list(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#list_()
     */
    public void list_()
    {
        getListener().endList(ListType.BULLETED, Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#listItem(SinkEventAttributes)
     */
    public void listItem(SinkEventAttributes attributes)
    {
        // TODO: Handle parameters
        getListener().beginListItem();

        ++this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#listItem()
     */
    public void listItem()
    {
        listItem(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#listItem_()
     */
    public void listItem_()
    {
        flushEmptyLines();

        getListener().endListItem();

        --this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#monospaced()
     */
    public void monospaced()
    {
        flushEmptyLines();

        getListener().beginFormat(Format.MONOSPACE, Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#monospaced_()
     */
    public void monospaced_()
    {
        flushEmptyLines();

        getListener().endFormat(Format.MONOSPACE, Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#nonBreakingSpace()
     */
    public void nonBreakingSpace()
    {
        flushEmptyLines();

        getListener().onSpace();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#numberedList(int, SinkEventAttributes)
     */
    public void numberedList(int numbering, SinkEventAttributes sinkEventAttributes)
    {
        flushEmptyLines();

        getListener().beginList(ListType.NUMBERED, Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#numberedList(int)
     */
    public void numberedList(int numbering)
    {
        numberedList(numbering, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#numberedList_()
     */
    public void numberedList_()
    {
        getListener().endList(ListType.NUMBERED, Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#numberedListItem(SinkEventAttributes)
     */
    public void numberedListItem(SinkEventAttributes attributes)
    {
        getListener().beginListItem();

        ++this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#numberedListItem()
     */
    public void numberedListItem()
    {
        numberedListItem(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#numberedListItem_()
     */
    public void numberedListItem_()
    {
        flushEmptyLines();

        getListener().endListItem();

        --this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#pageBreak()
     */
    public void pageBreak()
    {
        // Not supported in XWiki.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#paragraph(SinkEventAttributes)
     */
    public void paragraph(SinkEventAttributes attributes)
    {
        flushEmptyLines();

        // TODO: handle parameters
        getListener().beginParagraph(Listener.EMPTY_PARAMETERS);

        ++this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#paragraph()
     */
    public void paragraph()
    {
        paragraph(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#paragraph_()
     */
    public void paragraph_()
    {
        flushEmptyLines();

        getListener().endParagraph(Listener.EMPTY_PARAMETERS);

        --this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#rawText(String)
     */
    public void rawText(String text)
    {
        flushEmptyLines();

        getListener().onVerbatim(text, isInline(), Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#section(int, SinkEventAttributes)
     */
    public void section(int level, SinkEventAttributes attributes)
    {
        flushEmptyLines();

        getListener().beginSection(Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#section_(int)
     */
    public void section_(int level)
    {
        flushEmptyLines();

        getListener().endSection(Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#section1()
     */
    public void section1()
    {
        section(1, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#section1_()
     */
    public void section1_()
    {
        section_(1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#section2()
     */
    public void section2()
    {
        section(2, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#section2_()
     */
    public void section2_()
    {
        section_(2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#section3()
     */
    public void section3()
    {
        section(3, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#section3_()
     */
    public void section3_()
    {
        section_(3);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#section4()
     */
    public void section4()
    {
        section(4, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#section4_()
     */
    public void section4_()
    {
        section_(4);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#section5()
     */
    public void section5()
    {
        section(5, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#section5_()
     */
    public void section5_()
    {
        section_(5);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle(int, SinkEventAttributes)
     */
    public void sectionTitle(int level, SinkEventAttributes attributes)
    {
        flushEmptyLines();

        CompositeListener composite = new CompositeListener();

        composite.addListener(new QueueListener());
        composite.addListener(this.plainRendererFactory.createRenderer(new DefaultWikiPrinter()));

        pushListener(composite);

        ++this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle()
     */
    public void sectionTitle()
    {
        // Should be deprecated in Doxia
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle_(int)
     */
    public void sectionTitle_(int level)
    {
        flushEmptyLines();

        CompositeListener composite = (CompositeListener) getListener();

        QueueListener queue = (QueueListener) composite.getListener(0);
        PrintRenderer renderer = (PrintRenderer) composite.getListener(1);

        popListener();

        HeaderLevel headerLevel = HeaderLevel.parseInt(level);
        String id = this.idGenerator.generateUniqueId("H", renderer.getPrinter().toString());

        getListener().beginHeader(headerLevel, id, Listener.EMPTY_PARAMETERS);
        queue.consumeEvents(getListener());
        getListener().endHeader(headerLevel, id, Listener.EMPTY_PARAMETERS);

        --this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle_()
     */
    public void sectionTitle_()
    {
        // Should be deprecated in Doxia
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle1()
     */
    public void sectionTitle1()
    {
        sectionTitle(1, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle1_()
     */
    public void sectionTitle1_()
    {
        sectionTitle_(1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle2()
     */
    public void sectionTitle2()
    {
        sectionTitle(2, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle2_()
     */
    public void sectionTitle2_()
    {
        sectionTitle_(2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle3()
     */
    public void sectionTitle3()
    {
        sectionTitle(3, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle3_()
     */
    public void sectionTitle3_()
    {
        sectionTitle_(3);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle4()
     */
    public void sectionTitle4()
    {
        sectionTitle(4, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle4_()
     */
    public void sectionTitle4_()
    {
        sectionTitle_(4);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle5()
     */
    public void sectionTitle5()
    {
        sectionTitle(5, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#sectionTitle5_()
     */
    public void sectionTitle5_()
    {
        sectionTitle_(5);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#table(SinkEventAttributes)
     */
    public void table(SinkEventAttributes attributes)
    {
        flushEmptyLines();

        // TODO: Handle parameters
        getListener().beginTable(Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#table()
     */
    public void table()
    {
        table(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#table_()
     */
    public void table_()
    {
        getListener().endTable(Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableCaption(SinkEventAttributes)
     */
    public void tableCaption(SinkEventAttributes attributes)
    {
        // TODO: Handle this
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableCaption()
     */
    public void tableCaption()
    {
        tableCaption(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableCaption_()
     */
    public void tableCaption_()
    {
        // TODO: Handle this
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableCell(SinkEventAttributes)
     */
    public void tableCell(SinkEventAttributes attributes)
    {
        // TODO: Handle parameters
        getListener().beginTableCell(Listener.EMPTY_PARAMETERS);

        ++this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableCell()
     */
    public void tableCell()
    {
        tableCell((SinkEventAttributes) null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableCell(String)
     */
    public void tableCell(String width)
    {
        // TODO: Handle width
        tableCell((SinkEventAttributes) null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableCell_()
     */
    public void tableCell_()
    {
        flushEmptyLines();

        getListener().endTableCell(Listener.EMPTY_PARAMETERS);

        --this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableHeaderCell(SinkEventAttributes)
     */
    public void tableHeaderCell(SinkEventAttributes attributes)
    {
        // TODO: Handle parameters
        getListener().beginTableHeadCell(Listener.EMPTY_PARAMETERS);

        ++this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableHeaderCell()
     */
    public void tableHeaderCell()
    {
        tableHeaderCell((SinkEventAttributes) null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableHeaderCell(String)
     */
    public void tableHeaderCell(String width)
    {
        // TODO: Handle width
        tableHeaderCell((SinkEventAttributes) null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableHeaderCell_()
     */
    public void tableHeaderCell_()
    {
        flushEmptyLines();

        getListener().endTableHeadCell(Listener.EMPTY_PARAMETERS);

        --this.inlineDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableRow(SinkEventAttributes)
     */
    public void tableRow(SinkEventAttributes attributes)
    {
        // TODO: Handle parameters
        getListener().beginTableRow(Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableRow()
     */
    public void tableRow()
    {
        tableRow(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableRow_()
     */
    public void tableRow_()
    {
        getListener().endTableRow(Listener.EMPTY_PARAMETERS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableRows(int[], boolean)
     */
    public void tableRows(int[] arg0, boolean arg1)
    {
        // Not supported by XWiki.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#tableRows_()
     */
    public void tableRows_()
    {
        // Not supported by XWiki.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#text(String, SinkEventAttributes)
     */
    public void text(String text, SinkEventAttributes attributes)
    {
        flushEmptyLines();

        // TODO Handle parameters
        // Since Doxia doesn't generate events at the word level we need to reparse the
        // text to extract spaces, special symbols and words.

        // TODO: Use an inline parser. See http://jira.xwiki.org/jira/browse/XWIKI-2748
        WrappingListener inlineFilterListener = new InlineFilterListener();
        inlineFilterListener.setWrappedListener(getListener());

        // Parse the text using the plain text parser
        try {
            this.plainParser.parse(new StringReader(text), inlineFilterListener);
        } catch (ParseException e) {
            // Shouldn't happen since we use a StringReader which shouldn't generate any IO.
            throw new RuntimeException("Failed to parse raw text [" + text + "]", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#text(String)
     */
    public void text(String text)
    {
        text(text, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#title(SinkEventAttributes)
     */
    public void title(SinkEventAttributes attributes)
    {
        // XWiki's Listener model doesn't support titles. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#title()
     */
    public void title()
    {
        // XWiki's Listener model doesn't support titles. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#title_()
     */
    public void title_()
    {
        // XWiki's Listener model doesn't support titles. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#verbatim(SinkEventAttributes)
     */
    public void verbatim(SinkEventAttributes attributes)
    {
        // Nothing to do since whitespaces are significant in the XDOM.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#verbatim(boolean)
     */
    public void verbatim(boolean boxed)
    {
        // Nothing to do since whitespaces are significant in the XDOM.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#verbatim_()
     */
    public void verbatim_()
    {
        // Nothing to do since whitespaces are significant in the XDOM.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Sink#unknown(String, Object[], SinkEventAttributes)
     */
    public void unknown(String arg0, Object[] arg1, SinkEventAttributes arg2)
    {
        // TODO: Not supported yet by the XDOM.
    }
}
