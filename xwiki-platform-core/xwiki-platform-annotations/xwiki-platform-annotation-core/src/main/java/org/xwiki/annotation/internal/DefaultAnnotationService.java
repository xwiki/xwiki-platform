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
package org.xwiki.annotation.internal;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.AnnotationService;
import org.xwiki.annotation.AnnotationServiceException;
import org.xwiki.annotation.io.IOService;
import org.xwiki.annotation.io.IOServiceException;
import org.xwiki.annotation.io.IOTargetService;
import org.xwiki.annotation.maintainer.AnnotationState;
import org.xwiki.annotation.renderer.AnnotationPrintRenderer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.TransformationManager;

/**
 * Default annotation service, using the default {@link IOTargetService} and and {@link IOTargetService}, dispatching
 * calls and implementing the rendering of the content based on these data from the 2 services.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Singleton
public class DefaultAnnotationService implements AnnotationService
{
    /**
     * The storage service for annotations.
     */
    @Inject
    private IOService ioService;

    /**
     * Component manager used to lookup the content alterer needed for the specific document.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The storage service for annotation targets (documents).
     */
    @Inject
    private IOTargetService targetIoService;

    @Override
    public void addAnnotation(String target, String selection, String selectionContext, int offset, String author,
        Map<String, Object> metadata) throws AnnotationServiceException
    {
        try {
            // create the annotation with this data and send it to the storage service
            // TODO: also think of mapping the annotation on the document at add time and fail it if it's not mappable,
            // for extra security
            // TODO: normalize spaces at this level
            String leftContext = selectionContext.substring(0, offset);
            String rightContext = selectionContext.substring(offset + selection.length());
            Annotation annotation = new Annotation(selection, leftContext, rightContext);
            annotation.setAuthor(author);
            // skip these fields as we don't want to overwrite them with whatever is in this map. Setters should be used
            // for these values or constructor
            Collection<String> skippedFields =
                Arrays.asList(new String[] {Annotation.SELECTION_FIELD, Annotation.SELECTION_LEFT_CONTEXT_FIELD,
                    Annotation.SELECTION_RIGHT_CONTEXT_FIELD, Annotation.ORIGINAL_SELECTION_FIELD, 
                    Annotation.AUTHOR_FIELD, Annotation.STATE_FIELD});
            for (Map.Entry<String, Object> field : metadata.entrySet()) {
                if (!skippedFields.contains(field.getKey())) {
                    annotation.set(field.getKey(), field.getValue());
                }
            }
            ioService.addAnnotation(target, annotation);
        } catch (IOServiceException e) {
            throw new AnnotationServiceException("An exception occurred when accessing the storage services", e);
        }
    }

    @Override
    public String getAnnotatedRenderedContent(String sourceReference, String sourceSyntax, String outputSyntax,
        Collection<Annotation> annotations) throws AnnotationServiceException
    {
        try {
            String source = targetIoService.getSource(sourceReference);
            String sourceSyntaxId = sourceSyntax;
            // get if unspecified, get the source from the io service
            if (sourceSyntaxId == null) {
                sourceSyntaxId = targetIoService.getSourceSyntax(sourceReference);
            }

            Parser parser = componentManager.getInstance(Parser.class, sourceSyntaxId);
            XDOM xdom = parser.parse(new StringReader(source));

            // run transformations
            SyntaxFactory syntaxFactory = componentManager.getInstance(SyntaxFactory.class);
            Syntax sSyntax = syntaxFactory.createSyntaxFromIdString(sourceSyntaxId);
            TransformationManager transformationManager = componentManager.getInstance(TransformationManager.class);
            transformationManager.performTransformations(xdom, sSyntax);

            // build the annotations renderer hint for the specified output syntax
            String outputSyntaxId = "annotations-" + outputSyntax;
            AnnotationPrintRenderer annotationsRenderer =
                componentManager.getInstance(AnnotationPrintRenderer.class, outputSyntaxId);
            WikiPrinter printer = new DefaultWikiPrinter();
            annotationsRenderer.setPrinter(printer);
            // set the annotations for this renderer
            annotationsRenderer.setAnnotations(annotations);

            xdom.traverse(annotationsRenderer);

            return printer.toString();
        } catch (Exception exc) {
            throw new AnnotationServiceException(exc);
        }
    }

    @Override
    public String getAnnotatedHTML(String sourceReference) throws AnnotationServiceException
    {
        return getAnnotatedRenderedContent(sourceReference, null, "xhtml/1.0", getValidAnnotations(sourceReference));
    }

    @Override
    public Collection<Annotation> getAnnotations(String target) throws AnnotationServiceException
    {
        try {
            return ioService.getAnnotations(target);
        } catch (IOServiceException e) {
            throw new AnnotationServiceException(e);
        }
    }

    @Override
    public Collection<Annotation> getValidAnnotations(String target) throws AnnotationServiceException
    {
        try {
            List<Annotation> result = new ArrayList<Annotation>();
            for (Annotation it : ioService.getAnnotations(target)) {
                if (it.getState() == AnnotationState.SAFE || it.getState() == AnnotationState.UPDATED) {
                    result.add(it);
                }
            }
            return result;
        } catch (IOServiceException e) {
            throw new AnnotationServiceException(e);
        }
    }

    @Override
    public void removeAnnotation(String target, String annotationID) throws AnnotationServiceException
    {
        try {
            ioService.removeAnnotation(target, annotationID);
        } catch (IOServiceException e) {
            throw new AnnotationServiceException(e.getMessage());
        }
    }

    @Override
    public void updateAnnotation(String target, Annotation annotation) throws AnnotationServiceException
    {
        try {
            ioService.updateAnnotations(target, Arrays.asList(annotation));
        } catch (IOServiceException e) {
            throw new AnnotationServiceException(e.getMessage());
        }
    }

    @Override
    public Annotation getAnnotation(String target, String id) throws AnnotationServiceException
    {
        try {
            return ioService.getAnnotation(target, id);
        } catch (IOServiceException e) {
            throw new AnnotationServiceException(e.getMessage());
        }
    }
}
