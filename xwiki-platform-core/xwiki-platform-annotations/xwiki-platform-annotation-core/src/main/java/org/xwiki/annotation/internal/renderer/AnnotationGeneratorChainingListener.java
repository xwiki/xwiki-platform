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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.content.AlteredContent;
import org.xwiki.annotation.content.ContentAlterer;
import org.xwiki.annotation.renderer.AnnotationEvent;
import org.xwiki.annotation.renderer.AnnotationEvent.AnnotationEventType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.chaining.ChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Chaining listener that maps the annotations on the events that it receives, holds the state of annotations on these
 * events and exposes it to the subsequent listeners in the chain through {@link #getAnnotationEvents()}. It operates by
 * buffering all events, creating the plain text representation of the listened content, mapping the annotations on this
 * content and identifying the events in the stream that hold the start and end of the annotations. <br>
 * FIXME: this should use the PlainTextNormalizingChaininngRenderer to generate the plain text version of the content,
 * and match with the space normalized selection.
 * 
 * @version $Id$
 * @since 2.3M1
 */
public class AnnotationGeneratorChainingListener extends QueueListener implements ChainingListener
{
    /**
     * Version number of this class.
     */
    private static final long serialVersionUID = -2790330640900288463L;

    /**
     * The chain listener from which this listener is part of.
     */
    private ListenerChain chain;

    /**
     * Buffer to store the plain text version of the content to be rendered, so that annotations are mapped on it.
     */
    private StringBuffer plainTextContent = new StringBuffer();

    /**
     * Map to store the ranges in the plainTextContent and their corresponding events. The ranges will be stored by
     * their end index (inclusive) and ordered from smallest to biggest.
     */
    private SortedMap<Integer, Event> eventsMapping = new TreeMap<Integer, Event>();

    /**
     * Map to store the events whose content has been altered upon append to the plain text representation, along with
     * the altered content objects to allow translation of offsets back to the original offsets.
     */
    private Map<Event, AlteredContent> alteredEventsContent = new HashMap<Event, AlteredContent>();

    /**
     * The collection of annotations to generate annotation events for, by default the empty list.
     */
    private Collection<Annotation> annotations = Collections.<Annotation> emptyList();

    /**
     * Cleaner for the annotation selection text, so that it can be mapped on the content.
     */
    private ContentAlterer selectionAlterer;

    /**
     * The list of bookmarks where annotation events take place. The map holds a correspondence between the event in the
     * stream of wiki events and the annotation events that take place inside it, at the specified offset.
     */
    private Map<Event, SortedMap<Integer, List<AnnotationEvent>>> bookmarks =
        new HashMap<Event, SortedMap<Integer, List<AnnotationEvent>>>();

    /**
     * Builds an annotation generator listener from the passed link generator in the passed chain.
     * 
     * @param selectionAlterer cleaner for the annotation selection text, so that it can be mapped on the content
     * @param listenerChain the chain in which this listener is part of
     */
    public AnnotationGeneratorChainingListener(ContentAlterer selectionAlterer, ListenerChain listenerChain)
    {
        this.chain = listenerChain;
        this.selectionAlterer = selectionAlterer;
    }

    @Override
    public void onWord(String word)
    {
        // queue this event
        super.onWord(word);
        // put it in the buffer
        plainTextContent.append(word);
        // store the mapping of the range to the just added event
        eventsMapping.put(plainTextContent.length() - 1, getLast());
    }

    @Override
    public void onSpecialSymbol(char symbol)
    {
        super.onSpecialSymbol(symbol);
        plainTextContent.append("" + symbol);
        eventsMapping.put(plainTextContent.length() - 1, getLast());
    }

    @Override
    public void onVerbatim(String content, boolean inline, Map<String, String> parameters)
    {
        super.onVerbatim(content, inline, parameters);
        handleRawText(content);
    }

    /**
     * Helper function to help handle raw text, such as the raw blocks or the verbatim blocks.
     * 
     * @param text the raw text to handle
     */
    private void handleRawText(String text)
    {
        // normalize the protected string before adding it to the plain text version
        AlteredContent cleanedContent = selectionAlterer.alter(text);
        // put this event in the mapping only if it has indeed generated something
        String cleanedContentString = cleanedContent.getContent().toString();
        if (!StringUtils.isEmpty(cleanedContentString)) {
            plainTextContent.append(cleanedContentString);
            eventsMapping.put(plainTextContent.length() - 1, getLast());
            // also store this event in the list of events with altered content
            alteredEventsContent.put(getLast(), cleanedContent);
        }
    }

    @Override
    public void onRawText(String text, Syntax syntax)
    {
        // Store the raw text as it is ftm. Should handle syntax in the future
        super.onRawText(text, syntax);
        // Similar approach to verbatim FTM. In the future, syntax specific cleaner could be used for various syntaxes
        // (which would do the great job for HTML, for example)
        handleRawText(text);
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.0M2
     */
    @Override
    public void endDocument(MetaData metadata)
    {
        super.endDocument(metadata);

        // create the bookmarks
        mapAnnotations();

        // now get the next listener in the chain and consume all events to it
        ChainingListener renderer = chain.getNextListener(getClass());

        // send the events forward to the next annotation listener
        consumeEvents(renderer);
    }

    /**
     * Helper method to map the annotations on the plainTextContent and identify the events where annotations start and
     * end.
     */
    private void mapAnnotations()
    {
        for (Annotation ann : annotations) {
            // clean it up and its context
            String annotationContext = ann.getSelectionInContext();
            if (StringUtils.isEmpty(annotationContext)) {
                // cannot find the context of the annotation or the annotation selection cannot be found in the
                // annotation context, ignore it
                // TODO: mark it somehow...
                continue;
            }
            // build the cleaned version of the annotation by cleaning its left context, selection and right context and
            // concatenating them together
            String alteredsLeftContext =
                StringUtils.isEmpty(ann.getSelectionLeftContext()) ? "" : selectionAlterer.alter(
                    ann.getSelectionLeftContext()).getContent().toString();
            String alteredRightContext =
                StringUtils.isEmpty(ann.getSelectionRightContext()) ? "" : selectionAlterer.alter(
                    ann.getSelectionRightContext()).getContent().toString();
            String alteredSelection =
                StringUtils.isEmpty(ann.getSelection()) ? "" : selectionAlterer.alter(ann.getSelection()).getContent()
                    .toString();
            String cleanedContext = alteredsLeftContext + alteredSelection + alteredRightContext;
            // find the annotation with its context in the plain text representation of the content
            int contextIndex = plainTextContent.indexOf(cleanedContext);
            if (contextIndex >= 0) {
                // find the indexes where annotation starts and ends inside the cleaned context
                int alteredSelectionStartIndex = alteredsLeftContext.length();
                int alteredSelectionEndIndex = alteredSelectionStartIndex + alteredSelection.length() - 1;
                // get the start and end events for the annotation
                // annotation starts before char at annotationIndex and ends after char at annotationIndex +
                // alteredSelection.length() - 1
                Object[] startEvt = getEventAndOffset(contextIndex + alteredSelectionStartIndex, false);
                Object[] endEvt = getEventAndOffset(contextIndex + alteredSelectionEndIndex, true);
                if (startEvt != null & endEvt != null) {
                    // store the bookmarks
                    addBookmark((Event) startEvt[0], new AnnotationEvent(AnnotationEventType.START, ann),
                        (Integer) startEvt[1]);
                    addBookmark((Event) endEvt[0], new AnnotationEvent(AnnotationEventType.END, ann),
                        (Integer) endEvt[1]);
                } else {
                    // cannot find the events for the start and / or end of annotation, ignore it
                    // TODO: mark it somehow...
                    continue;
                }
            } else {
                // cannot find the context of the annotation or the annotation selection cannot be found in the
                // annotation context, ignore it
                // TODO: mark it somehow...
                continue;
            }
        }
    }

    /**
     * Helper function to get the event where the passed index falls in, based on the isEnd setting to know if the
     * offset should be given before the character at the index position or after it.
     * 
     * @param index the index to get the event for
     * @param isEnd {@code true} if the index should be considered as an end index, {@code false} otherwise
     * @return an array of objects to hold the event reference, on the first position, and the offset inside this event
     *         on the second
     */
    private Object[] getEventAndOffset(int index, boolean isEnd)
    {
        Map.Entry<Integer, Event> previous = null;
        for (Map.Entry<Integer, Event> range : eventsMapping.entrySet()) {
            // <= because end index is included
            // if we have reached the first point where the end index is to the left of the index, it means we're in the
            // first event that contains the index
            if (index <= range.getKey()) {
                // get this event
                Event evt = range.getValue();
                // compute the start index wrt to the end index of the previous event
                int startIndex = 0;
                if (previous != null) {
                    startIndex = previous.getKey() + 1;
                }
                // compute the offset inside this event wrt the start index of this event
                int offset = index - startIndex;

                // adjust this offset if the content of this event was altered
                AlteredContent alteredEventContent = alteredEventsContent.get(evt);
                if (alteredEventContent != null) {
                    offset = alteredEventContent.getInitialOffset(offset);
                }

                // end indexes are specified after the character/position
                if (isEnd) {
                    offset += 1;
                }

                // return the result
                return new Object[] {evt, offset};
            }
            // else advance one more step, storing the previous event
            previous = range;
        }
        // nothing was found, return null. However this shouldn't happen :)
        return null;
    }

    /**
     * Adds an annotation bookmark in this list of bookmarks.
     * 
     * @param renderingEvent the rendering event where the annotation should be bookmarked
     * @param offset the offset of the annotation event inside this rendering event
     * @param annotationEvent the annotation event to bookmark
     */
    protected void addBookmark(Event renderingEvent, AnnotationEvent annotationEvent, int offset)
    {
        SortedMap<Integer, List<AnnotationEvent>> mappings = bookmarks.get(renderingEvent);
        if (mappings == null) {
            mappings = new TreeMap<Integer, List<AnnotationEvent>>();
            bookmarks.put(renderingEvent, mappings);
        }
        List<AnnotationEvent> events = mappings.get(offset);
        if (events == null) {
            events = new LinkedList<AnnotationEvent>();
            mappings.put(offset, events);
        }

        addAnnotationEvent(annotationEvent, events);
    }

    /**
     * Helper function to help add an annotation event to the list of events, and keep the restriction that end events
     * are stored before start events. Otherwise put, for the same offset, annotations end first and open after.
     * 
     * @param evt the annotation event to add to the list
     * @param list the annotation events list to add the event to
     */
    protected void addAnnotationEvent(AnnotationEvent evt, List<AnnotationEvent> list)
    {
        // if there is no event in the list, or the event is a start event or there is no start event in the list, just
        // append the event to the end of the list
        if (list.size() == 0 || evt.getType() == AnnotationEventType.START
            || list.get(list.size() - 1).getType() == AnnotationEventType.END) {
            list.add(evt);
        } else {
            // find the first start event and add before it
            int index = 0;
            for (index = 0; index < list.size() && list.get(index).getType() != AnnotationEventType.START; index++) {
                // nothing, it will stop at first start event
            }
            list.add(index, evt);
        }
    }

    @Override
    public void consumeEvents(Listener listener)
    {
        // same function basically, except that we need to leave the event at the top of the queue when firing so that
        // we can get the correct state
        while (!isEmpty()) {
            // peek the queue
            Event event = getFirst();
            // fire the event with the event as the top of the queue still so that we can give the correct bookmarks
            event.eventType.fireEvent(listener, event.eventParameters);
            // and remove the event so that we can go to next
            remove();
        }
    }

    @Override
    public ListenerChain getListenerChain()
    {
        return chain;
    }

    /**
     * Sets the collections of annotations to identify on the listened content and send notifications for.
     * 
     * @param annotations the collection of annotations to generate events for
     */
    public void setAnnotations(Collection<Annotation> annotations)
    {
        this.annotations = annotations;
    }

    /**
     * @return the bookmarks where annotation events take place
     */
    public SortedMap<Integer, List<AnnotationEvent>> getAnnotationEvents()
    {
        return bookmarks.get(getFirst());
    }
}
