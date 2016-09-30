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
package org.xwiki.annotation.internal.renderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.xwiki.annotation.renderer.AnnotationEvent;
import org.xwiki.annotation.renderer.ChainingPrintRenderer;
import org.xwiki.annotation.renderer.AnnotationEvent.AnnotationEventType;
import org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.image.XHTMLImageRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.XHTMLLinkRenderer;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Extends the default XHTML renderer to add handling of annotations.<br>
 * 
 * @version $Id$
 * @since 2.3M1
 */
public class AnnotationXHTMLChainingRenderer extends XHTMLChainingRenderer implements ChainingPrintRenderer
{
    /**
     * Map to store the events count to be able to identify an event in the emitted events.
     */
    private Map<EventType, Integer> eventsCount = new HashMap<EventType, Integer>();

    /**
     * The annotations XHTML markers printer, used to handle annotations markers rendering and nesting.
     */
    private AnnotationMarkersXHTMLPrinter annotationsMarkerPrinter;

    /**
     * Constructor from super class.
     * 
     * @param linkRenderer the renderer for links
     * @param imageRenderer the renderer for images
     * @param listenerChain the listener chain in which to add this listener
     */
    public AnnotationXHTMLChainingRenderer(XHTMLLinkRenderer linkRenderer, XHTMLImageRenderer imageRenderer,
        ListenerChain listenerChain)
    {
        super(linkRenderer, imageRenderer, listenerChain);
    }

    /**
     * @return the annotations printer for this print renderer, used to handle annotations markers rendering and nesting
     */
    public AnnotationMarkersXHTMLPrinter getAnnotationsMarkerPrinter()
    {
        if (annotationsMarkerPrinter == null) {
            annotationsMarkerPrinter = new AnnotationMarkersXHTMLPrinter(getPrinter());
        }
        return annotationsMarkerPrinter;
    }

    @Override
    protected XHTMLWikiPrinter getXHTMLWikiPrinter()
    {
        return this.getAnnotationsMarkerPrinter();
    }

    /**
     * @return the annotation generator listener in this chain, holding the annotations state in the current rendering
     */
    protected AnnotationGeneratorChainingListener getAnnotationGenerator()
    {
        return (AnnotationGeneratorChainingListener) getListenerChain().getListener(
            AnnotationGeneratorChainingListener.class);
    }

    @Override
    public void onWord(String word)
    {
        // open all annotation markers which are closed and need to be opened
        getAnnotationsMarkerPrinter().openAllAnnotationMarkers();
        // get the current annotation events
        SortedMap<Integer, List<AnnotationEvent>> annEvts = getAnnotationGenerator().getAnnotationEvents();
        if (annEvts != null && !annEvts.isEmpty()) {
            getAnnotationsMarkerPrinter().printXMLWithAnnotations(word, annEvts);
        } else {
            getXHTMLWikiPrinter().printXML(word);
        }
    }

    @Override
    public void onSpace()
    {
        getAnnotationsMarkerPrinter().openAllAnnotationMarkers();
        super.onSpace();
    }

    @Override
    public void onSpecialSymbol(char symbol)
    {
        // open all annotation markers which are closed and need to be opened
        getAnnotationsMarkerPrinter().openAllAnnotationMarkers();
        // get the annotations state at this point
        SortedMap<Integer, List<AnnotationEvent>> annEvts = getAnnotationGenerator().getAnnotationEvents();
        if (annEvts != null && !annEvts.isEmpty()) {
            getAnnotationsMarkerPrinter().printXMLWithAnnotations("" + symbol, annEvts);
        } else {
            getXHTMLWikiPrinter().printXML("" + symbol);
        }
    }

    @Override
    public void onVerbatim(String content, boolean inline, Map<String, String> parameters)
    {
        SortedMap<Integer, List<AnnotationEvent>> annEvts = getAnnotationGenerator().getAnnotationEvents();
        if (inline) {
            String ttEltName = "tt";
            getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
            getXHTMLWikiPrinter().printXMLStartElement(ttEltName, new String[][] {{"class", "wikimodel-verbatim"}});
            getAnnotationsMarkerPrinter().openAllAnnotationMarkers();
            if (annEvts != null && annEvts.size() > 0) {
                getAnnotationsMarkerPrinter().printXMLWithAnnotations(content, annEvts);
            } else {
                getXHTMLWikiPrinter().printXML(content);
            }
            getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
            getXHTMLWikiPrinter().printXMLEndElement(ttEltName);
        } else {
            String preEltName = "pre";
            getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
            getXHTMLWikiPrinter().printXMLStartElement(preEltName, parameters);
            getAnnotationsMarkerPrinter().openAllAnnotationMarkers();
            if (annEvts != null && annEvts.size() > 0) {
                getAnnotationsMarkerPrinter().printXMLWithAnnotations(content, annEvts);
            } else {
                getXHTMLWikiPrinter().printXML(content);
            }
            getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
            getXHTMLWikiPrinter().printXMLEndElement(preEltName);
        }
    }

    @Override
    public void onRawText(String text, Syntax syntax)
    {
        // FIXME: this is going to be messy, messy because of the raw block syntax which can be HTML and produce very
        // invalid html.

        SortedMap<Integer, List<AnnotationEvent>> currentBookmarks = getAnnotationGenerator().getAnnotationEvents();

        if (currentBookmarks != null) {
            // open all annotations that start in this event
            for (Map.Entry<Integer, List<AnnotationEvent>> bookmark : currentBookmarks.entrySet()) {
                for (AnnotationEvent annEvt : bookmark.getValue()) {
                    if (annEvt.getType() == AnnotationEventType.START) {
                        getAnnotationsMarkerPrinter().beginAnnotation(annEvt.getAnnotation());
                    }
                }
            }
        }

        // open all annotation markers in case there was any annotation enclosing this block
        getAnnotationsMarkerPrinter().openAllAnnotationMarkers();
        // Store the raw text as it is ftm. Should handle syntax in the future
        super.onRawText(text, syntax);

        if (currentBookmarks != null) {
            // close all annotations that start in this event.
            for (Map.Entry<Integer, List<AnnotationEvent>> bookmark : currentBookmarks.entrySet()) {
                for (AnnotationEvent annEvt : bookmark.getValue()) {
                    if (annEvt.getType() == AnnotationEventType.END) {
                        getAnnotationsMarkerPrinter().endAnnotation(annEvt.getAnnotation());
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @since 2.5RC1
     */
    @Override
    public void endLink(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endLink(reference, freestanding, parameters);
    }

    @Override
    public void endDefinitionDescription()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endDefinitionDescription();
    }

    @Override
    public void endDefinitionList(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endDefinitionList(parameters);
    }

    @Override
    public void endDefinitionTerm()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.0M2
     */
    @Override
    public void endDocument(MetaData metadata)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endDocument(metadata);
    }

    @Override
    public void endFormat(Format format, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endFormat(format, parameters);
    }

    @Override
    public void endGroup(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endGroup(parameters);
    }

    @Override
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endHeader(level, id, parameters);
    }

    @Override
    public void endList(ListType type, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endList(type, parameters);
    }

    @Override
    public void endListItem()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endListItem();
    }

    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endParagraph(parameters);
    }

    @Override
    public void endQuotation(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endQuotation(parameters);
    }

    @Override
    public void endQuotationLine()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endQuotationLine();
    }

    @Override
    public void endSection(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endSection(parameters);
    }

    @Override
    public void endTable(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endTable(parameters);
    }

    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endTableCell(parameters);
    }

    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endTableHeadCell(parameters);
    }

    @Override
    public void endTableRow(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endTableRow(parameters);
    }

    @Override
    public void beginDefinitionDescription()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginDefinitionDescription();
    }

    @Override
    public void beginDefinitionList(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginDefinitionList(parameters);
    }

    @Override
    public void beginDefinitionTerm()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginDefinitionTerm();
    }

    @Override
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginFormat(format, parameters);
    }

    @Override
    public void beginGroup(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginGroup(parameters);
    }

    @Override
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginHeader(level, id, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 2.5RC1
     */
    @Override
    public void beginLink(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginLink(reference, freestanding, parameters);
    }

    @Override
    public void beginList(ListType type, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginList(type, parameters);
    }

    @Override
    public void beginListItem()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginListItem();
    }

    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginParagraph(parameters);
    }

    @Override
    public void beginQuotation(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginQuotation(parameters);
    }

    @Override
    public void beginQuotationLine()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginQuotationLine();
    }

    @Override
    public void beginSection(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginSection(parameters);
    }

    @Override
    public void beginTable(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginTable(parameters);
    }

    @Override
    public void beginTableCell(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginTableCell(parameters);
    }

    @Override
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginTableHeadCell(parameters);
    }

    @Override
    public void beginTableRow(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginTableRow(parameters);
    }

    /**
     * Helper function to get the current event count of the specified type, and increment it. Similar to a ++ operation
     * on the Integer mapped to the passed event type.
     * 
     * @param type the event type
     * @return the current event count for the passed type.
     */
    protected int getAndIncrement(EventType type)
    {
        Integer currentCount = eventsCount.get(type);
        if (currentCount == null) {
            currentCount = 0;
        }
        eventsCount.put(type, currentCount + 1);
        return currentCount;
    }
}
