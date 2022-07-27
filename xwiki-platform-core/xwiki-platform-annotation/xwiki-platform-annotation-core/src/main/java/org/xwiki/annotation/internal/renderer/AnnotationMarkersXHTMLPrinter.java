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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.renderer.AnnotationEvent;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;
import org.xwiki.xml.html.HTMLElementSanitizer;

/**
 * XHTML Printer to handle printing annotations markers in the rendered XHTML. It is able to generate the annotation
 * start marker and end marker and to store information about the annotations state: which ones are being currently
 * rendered, which markers are opened, etc. To be used by the XHTML renderers to render annotations on the XHTML. <br>
 * Note that, although this class could aggregate a XHTMLWikiPrinter, and be used to wrap the default XHTMLWikiPrinter
 * in a XHTML Renderer, it extends it so that it can replace. TODO: in a future improved implementation, this could
 * handle annotations rendering alone, just using state information from the renderer.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class AnnotationMarkersXHTMLPrinter extends XHTMLWikiPrinter
{

    /**
     * The annotation marker element in HTML.
     */
    private static final String ANNOTATION_MARKER = "span";

    /**
     * Flag to signal if the annotations in the openAnnotations stack are actually opened in the printed XHTML. Namely
     * this will become true when a text event will occur (like a word or space or ...) and will become false when and
     * end event will occur.
     */
    private boolean open;

    /**
     * The list of annotations being currently rendered, in the order in which they were opened (i.e. for which
     * beginAnnotation was signaled but not endAnnotation). Used for correctly nesting the annotations markers with
     * other XHTML elements. <br>
     */
    private List<Annotation> renderedAnnotations = new LinkedList<Annotation>();

    /**
     * Builds an XHTML markers annotations printer which uses the underlying wiki printer.
     *
     * @param printer the wiki printer used by this XHTML printer
     * @param htmlElementSanitizer the HTML element sanitizer to use by this XHTML printer
     */
    public AnnotationMarkersXHTMLPrinter(WikiPrinter printer, HTMLElementSanitizer htmlElementSanitizer)
    {
        super(printer, htmlElementSanitizer);
    }

    /**
     * Handles the beginning of a new annotation.
     *
     * @param annotation the annotation that begins
     */
    public void beginAnnotation(Annotation annotation)
    {
        // and put it in the stack of open annotations
        renderedAnnotations.add(annotation);
        // if all other annotations are opened, open this one too. Otherwise it will be opened whenever all the others
        // are opened.
        if (open) {
            printAnnotationStartMarker(annotation);
        }
    }

    /**
     * Handles the end of an annotation.
     *
     * @param annotation the annotation that ends
     */
    public void endAnnotation(Annotation annotation)
    {
        // all annotations which are opened after this one must be closed before this close and reopened after
        int annIndex = renderedAnnotations.indexOf(annotation);
        // close all annotations opened after this one, in reverse order
        for (int i = renderedAnnotations.size() - 1; i > annIndex; i--) {
            printAnnotationEndMarker(renderedAnnotations.get(i));
        }
        // close this annotation
        printAnnotationEndMarker(annotation);
        // open all previously closed annotations in the order they were initially opened
        for (int i = annIndex + 1; i < renderedAnnotations.size(); i++) {
            printAnnotationStartMarker(renderedAnnotations.get(i));
        }
        // and remove it from the list of open annotations
        renderedAnnotations.remove(annotation);
    }

    /**
     * Prints the start marker for the passed annotation.
     *
     * @param annotation the annotation to print the start marker for
     */
    private void printAnnotationStartMarker(Annotation annotation)
    {
        Map<String, String> attributes = new LinkedHashMap<String, String>();

        attributes.put("class", "annotation ID" + annotation.getId());
        printXMLStartElement(ANNOTATION_MARKER, attributes);
    }

    /**
     * Prints the end marker for the passed annotation.
     *
     * @param annotation the annotation to print end marker for
     */
    private void printAnnotationEndMarker(Annotation annotation)
    {
        printXMLEndElement(ANNOTATION_MARKER);
    }

    /**
     * Helper function to handle closing all annotations. To be called either when elements close or when an element
     * opens (all annotation spans will only wrap text, not inner elements). It will close all opened annotations
     * markers and set the flag to specify that annotations are closed and they should be opened at next text element.
     */
    public void closeAllAnnotationMarkers()
    {
        // if the annotations are opened
        if (open) {
            // for each annotation from the last opened to the first opened
            for (int i = renderedAnnotations.size() - 1; i >= 0; i--) {
                // close it
                printAnnotationEndMarker(renderedAnnotations.get(i));
            }
            // set the flag so that next end event doesn't close them as well
            open = false;
        }
    }

    /**
     * Helper function to handle opening all annotations. If the annotations are not already opened, it should open them
     * all and set the flag to opened so that next text event doesn't do the same.
     */
    public void openAllAnnotationMarkers()
    {
        // if annotations are not opened
        if (!open) {
            // for each annotation in the order in which they were opened
            for (int i = 0; i < renderedAnnotations.size(); i++) {
                // re-open it
                printAnnotationStartMarker(renderedAnnotations.get(i));
            }
            // and mark the annotations as opened
            open = true;
        }
    }

    /**
     * Helper function to help render a piece of text with annotation events inside it, at the specified offsets. To be
     * used for the events which generate text and need to generate annotation markers inside them.
     *
     * @param text the text to render
     * @param annotations the map of indexes and annotation events to render in this text
     */
    public void printXMLWithAnnotations(String text, SortedMap<Integer, List<AnnotationEvent>> annotations)
    {
        // iterate through the indexes of annotations events, print the chunks in between and then handle the annotation
        // events
        int previous = 0;
        for (int index : annotations.keySet()) {
            // create the current chunk
            String currentChunk = text.substring(previous, index);
            // print the current chunk
            if (currentChunk.length() > 0) {
                printXML(currentChunk);
            }
            // handle all annotations at this position
            for (AnnotationEvent evt : annotations.get(index)) {
                switch (evt.getType()) {
                    case START:
                        beginAnnotation(evt.getAnnotation());
                        break;
                    case END:
                        endAnnotation(evt.getAnnotation());
                        break;
                    default:
                        // nothing
                        break;
                }
            }
            // and prepare next iteration
            previous = index;
        }
        // print the last chunk of text
        String chunk = text.substring(previous);
        if (chunk.length() > 0) {
            printXML(chunk);
        }
    }
}
