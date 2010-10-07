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
import org.xwiki.rendering.listener.ResourceReference;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Extends the default XHTML renderer to add handling of annotations.<br />
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#getXHTMLWikiPrinter()
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.QueueListener#onWord(java.lang.String)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#onSpace()
     */
    @Override
    public void onSpace()
    {
        getAnnotationsMarkerPrinter().openAllAnnotationMarkers();
        super.onSpace();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.QueueListener#onSpecialSymbol(char)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.QueueListener#onVerbatim(java.lang.String, boolean, java.util.Map)
     */
    @Override
    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        SortedMap<Integer, List<AnnotationEvent>> annEvts = getAnnotationGenerator().getAnnotationEvents();
        if (isInline) {
            String ttEltName = "tt";
            getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
            getXHTMLWikiPrinter().printXMLStartElement(ttEltName, new String[][] {{"class", "wikimodel-verbatim"}});
            getAnnotationsMarkerPrinter().openAllAnnotationMarkers();
            if (annEvts != null && annEvts.size() > 0) {
                getAnnotationsMarkerPrinter().printXMLWithAnnotations(protectedString, annEvts);
            } else {
                getXHTMLWikiPrinter().printXML(protectedString);
            }
            getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
            getXHTMLWikiPrinter().printXMLEndElement(ttEltName);
        } else {
            String preEltName = "pre";
            getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
            getXHTMLWikiPrinter().printXMLStartElement(preEltName, parameters);
            getAnnotationsMarkerPrinter().openAllAnnotationMarkers();
            if (annEvts != null && annEvts.size() > 0) {
                getAnnotationsMarkerPrinter().printXMLWithAnnotations(protectedString, annEvts);
            } else {
                getXHTMLWikiPrinter().printXML(protectedString);
            }
            getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
            getXHTMLWikiPrinter().printXMLEndElement(preEltName);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.QueueListener#onRawText(java.lang.String, org.xwiki.rendering.syntax.Syntax)
     */
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
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endLink(
     *      org.xwiki.rendering.listener.ResourceReference, boolean, java.util.Map)
     * @since 2.5RC1
     */
    @Override
    public void endLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endLink(reference, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#endDefinitionDescription()
     */
    @Override
    public void endDefinitionDescription()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#endDefinitionList(java.util.Map)
     */
    @Override
    public void endDefinitionList(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endDefinitionList(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#endDefinitionTerm()
     */
    @Override
    public void endDefinitionTerm()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDocument(java.util.Map)
     */
    @Override
    public void endDocument(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endDocument(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer
     *      #endFormat(org.xwiki.rendering.listener.Format, java.util.Map)
     */
    @Override
    public void endFormat(Format format, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endFormat(format, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#endGroup(java.util.Map)
     */
    @Override
    public void endGroup(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endGroup(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer
     *      #endHeader(org.xwiki.rendering.listener.HeaderLevel, java.lang.String, java.util.Map)
     */
    @Override
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endHeader(level, id, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer
     *      #endList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    @Override
    public void endList(ListType listType, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endList(listType, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#endListItem()
     */
    @Override
    public void endListItem()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endListItem();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#endParagraph(java.util.Map)
     */
    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endParagraph(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#endQuotation(java.util.Map)
     */
    @Override
    public void endQuotation(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endQuotation(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endQuotationLine()
     */
    @Override
    public void endQuotationLine()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endQuotationLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endSection(java.util.Map)
     */
    @Override
    public void endSection(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endSection(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#endTable(java.util.Map)
     */
    @Override
    public void endTable(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endTable(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#endTableCell(java.util.Map)
     */
    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endTableCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#endTableHeadCell(java.util.Map)
     */
    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endTableHeadCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#endTableRow(java.util.Map)
     */
    @Override
    public void endTableRow(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.endTableRow(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#beginDefinitionDescription()
     */
    @Override
    public void beginDefinitionDescription()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#beginDefinitionList(java.util.Map)
     */
    @Override
    public void beginDefinitionList(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginDefinitionList(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#beginDefinitionTerm()
     */
    @Override
    public void beginDefinitionTerm()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer
     *      #beginFormat(org.xwiki.rendering.listener.Format, java.util.Map)
     */
    @Override
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginFormat(format, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#beginGroup(java.util.Map)
     */
    @Override
    public void beginGroup(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginGroup(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer
     *      #beginHeader(org.xwiki.rendering.listener.HeaderLevel, java.lang.String, java.util.Map)
     */
    @Override
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginHeader(level, id, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer
     *      #beginLink(org.xwiki.rendering.listener.Link, boolean, java.util.Map)
     * @since 2.5RC1
     */
    @Override
    public void beginLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginLink(reference, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer
     *      #beginList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    @Override
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginList(listType, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#beginListItem()
     */
    @Override
    public void beginListItem()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginListItem();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#beginParagraph(java.util.Map)
     */
    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginParagraph(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#beginQuotation(java.util.Map)
     */
    @Override
    public void beginQuotation(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginQuotation(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#beginQuotationLine()
     */
    @Override
    public void beginQuotationLine()
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginQuotationLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginSection(java.util.Map)
     */
    @Override
    public void beginSection(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginSection(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#beginTable(java.util.Map)
     */
    @Override
    public void beginTable(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginTable(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#beginTableCell(java.util.Map)
     */
    @Override
    public void beginTableCell(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginTableCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#beginTableHeadCell(java.util.Map)
     */
    @Override
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        getAnnotationsMarkerPrinter().closeAllAnnotationMarkers();
        super.beginTableHeadCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.renderer.xhtml.XHTMLChainingRenderer#beginTableRow(java.util.Map)
     */
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
