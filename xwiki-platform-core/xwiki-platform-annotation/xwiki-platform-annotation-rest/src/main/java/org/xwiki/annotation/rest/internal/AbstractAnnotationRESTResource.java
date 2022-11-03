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

package org.xwiki.annotation.rest.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.AnnotationService;
import org.xwiki.annotation.AnnotationServiceException;
import org.xwiki.annotation.rest.model.jaxb.AnnotatedContent;
import org.xwiki.annotation.rest.model.jaxb.AnnotationField;
import org.xwiki.annotation.rest.model.jaxb.AnnotationFieldCollection;
import org.xwiki.annotation.rest.model.jaxb.AnnotationRequest;
import org.xwiki.annotation.rest.model.jaxb.AnnotationResponse;
import org.xwiki.annotation.rest.model.jaxb.AnnotationStub;
import org.xwiki.annotation.rest.model.jaxb.ObjectFactory;
import org.xwiki.annotation.rights.AnnotationRightService;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rest.XWikiResource;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.wysiwyg.converter.HTMLConverter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletResponse;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Base class for the annotation REST services, to implement common functionality to all annotation REST services.
 *
 * @version $Id$
 * @since 2.3M1
 */
public abstract class AbstractAnnotationRESTResource extends XWikiResource
{
    /**
     * The default action to render the document for. <br>
     * TODO: action should be obtained from the calling client in the parameters
     */
    protected static final String DEFAULT_ACTION = "view";

    private static final String COMMENT_KEY = "comment";

    private static final String COMMENT_SYNTAX_KEY = "comment_syntax";

    /**
     * The name of the request parameter value represents the parameter that requires
     * conversion from HTML to wiki syntax.
     * This parameter and its implementation is directly inspired from
     * {@link org.xwiki.wysiwyg.filter.ConversionFilter}.
     */
    private static final String REQUIRES_HTML_CONVERSION = "RequiresHTMLConversion";

    /**
     * The annotations service to be used by this REST interface.
     */
    @Inject
    protected AnnotationService annotationService;

    /**
     * The annotations rights checking service, to check user rights to perform annotations actions.
     */
    @Inject
    protected AnnotationRightService annotationRightService;

    /**
     * The execution needed to get the annotation author from the context user.
     */
    @Inject
    protected Execution execution;
    
    /**
     * Entity reference serializer used to get reference to the document to perform annotation operation on.
     */
    @Inject
    protected EntityReferenceSerializer<String> referenceSerializer;

    @Inject
    private HTMLConverter htmlConverter;

    @Inject
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    /**
     * Builds an annotation response containing the annotated content along with the annotation stubs, according to the
     * requirements in the passed annotations request.
     *
     * @param request the annotations request
     * @param documentName the name of the document to provide an annotated response for
     * @return an annotation response with the annotated content and the annotation stubs
     * @throws AnnotationServiceException in case something goes wrong handling the annotations
     * @throws XWikiException in case something goes wrong manipulating the xwiki context & documents
     */
    protected AnnotationResponse getSuccessResponseWithAnnotatedContent(String documentName, AnnotationRequest request)
        throws AnnotationServiceException, XWikiException
    {
        ObjectFactory factory = new ObjectFactory();
        AnnotationResponse response = factory.createAnnotationResponse();

        // get the annotations on this content
        Collection<Annotation> annotations = annotationService.getAnnotations(documentName);
        // filter them according to the request
        Collection<Annotation> filteredAnnotations = filterAnnotations(annotations, request);

        String renderedHTML = renderDocumentWithAnnotationsWithoutRedirects(documentName, filteredAnnotations);

        // prepare the annotated content
        AnnotatedContent annotatedContentResponse = factory.createAnnotatedContent();
        annotatedContentResponse.getAnnotations().addAll(
            prepareAnnotationStubsSet(filteredAnnotations, request.getRequest().getFields()));
        annotatedContentResponse.setContent(renderedHTML);
        // set the annotated content along with the return code in the response and return it
        response.setAnnotatedContent(annotatedContentResponse);
        response.setResponseCode(0);

        return response;
    }

    private String renderDocumentWithAnnotationsWithoutRedirects(String documentName,
        Collection<Annotation> filteredAnnotations) throws XWikiException, AnnotationServiceException
    {
        // Block the redirects as they are leading to an unexpected result for the annotations.
        XWikiContext context = this.xcontextProvider.get();
        XWikiResponse contextResponse = context.getResponse();
        try {
            context.setResponse(new XWikiServletResponse(contextResponse)
            {
                @Override
                public void sendRedirect(String location)
                {
                    // Do nothing as the purpose is to block the redirects.
                }
            });
            // render the document with the filtered annotations on it
            return renderDocumentWithAnnotations(documentName, null, DEFAULT_ACTION, filteredAnnotations);
        } finally {
            // Reset response to initial
            context.setResponse(contextResponse);
        }
    }

    /**
     * Helper function to translate a collection of annotations from the {@link Annotation} model to the JAXB model to
     * be serialized for REST communication.
     *
     * @param annotations the annotations collection to be translated
     * @param requestedFields the extra parameters that should be set for the prepared annotations
     * @return translate set of org.xwiki.annotation.internal.annotation.Annotation to set of
     *         org.xwiki.annotation.internal.annotation.Annotation
     */
    private Collection<AnnotationStub> prepareAnnotationStubsSet(Collection<Annotation> annotations,
        List<String> requestedFields)
    {
        ObjectFactory factory = new ObjectFactory();
        List<AnnotationStub> set = new ArrayList<AnnotationStub>();

        Set<String> actualRequestedFields = new LinkedHashSet<>(requestedFields);
        actualRequestedFields.add(Annotation.PLAIN_TEXT_START_OFFSET_FIELD);
        actualRequestedFields.add(Annotation.PLAIN_TEXT_END_OFFSET_FIELD);
        for (Annotation xwikiAnnotation : annotations) {
            AnnotationStub annotation = factory.createAnnotationStub();
            annotation.setAnnotationId(xwikiAnnotation.getId());
            annotation.setState(xwikiAnnotation.getState().toString());
            // for all the requested extra fields, get them from the annotation and send them
            for (String extraField : actualRequestedFields) {
                Object value = xwikiAnnotation.get(extraField);
                AnnotationField field = new AnnotationField();
                field.setName(extraField);
                // value.toString() by default, null if value is missing
                field.setValue(value != null ? value.toString() : null);
                annotation.getFields().add(field);
            }
            set.add(annotation);
        }
        return set;
    }

    /**
     * Helper function to create an error response from a passed exception. <br>
     *
     * @param exception the exception that was encountered during regular execution of service
     * @return an error response
     */
    protected AnnotationResponse getErrorResponse(Throwable exception)
    {
        AnnotationResponse result = new ObjectFactory().createAnnotationResponse();
        result.setResponseCode(1);
        String responseMessage = exception.getMessage();
        if (responseMessage == null) {
            // serialize the stack trace and send it as an error response
            StringWriter stackTraceWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stackTraceWriter));
            responseMessage = stackTraceWriter.toString();
        }
        result.setResponseMessage(responseMessage);
        result.setAnnotatedContent(null);
        return result;
    }

    /**
     * Helper function to get the rendered content of the document with annotations. All setup of context for rendering
     * content similar to the rendering on standard view will be done in this function. <br>
     * FIXME: find out if this whole context setup code has to be here or in the annotations service
     *
     * @param docName the name of the document to render
     * @param language the language in which to render the document
     * @param action the context action to render the document for
     * @param annotations the annotations to render on the document
     * @return the HTML rendered content of the document
     * @throws XWikiException if anything wrong happens while setting up the context for rendering
     * @throws AnnotationServiceException if anything goes wrong during the rendering of the annotations
     */
    private String renderDocumentWithAnnotations(String docName, String language, String action,
        Collection<Annotation> annotations) throws XWikiException, AnnotationServiceException
    {
        String isInRenderingEngineKey = "isInRenderingEngine";
        XWikiContext context = this.xcontextProvider.get();
        Object isInRenderingEngine = context.get(isInRenderingEngineKey);
        // set the context url factory to the servlet url factory so that all links get correctly generated as if we
        // were view-ing the page
        XWikiURLFactory oldFactory = context.getURLFactory();
        int oldMode = context.getMode();
        String result = null;
        try {
            context.setMode(XWikiContext.MODE_SERVLET);
            XWikiURLFactory urlf =
                context.getWiki().getURLFactoryService().createURLFactory(context.getMode(), context);
            context.setURLFactory(urlf);
            // setup documents on the context, and velocity context, and message tool for i18n and all
            setUpDocuments(docName, language);
            // set the current action on the context
            context.setAction(action);
            context.put(isInRenderingEngineKey, true);
            // render the content in xhtml syntax, with the passed list of annotations
            result = annotationService.getAnnotatedRenderedContent(docName, null, "xhtml/1.0", annotations);
        } finally {
            if (isInRenderingEngine != null) {
                context.put(isInRenderingEngineKey, isInRenderingEngine);
            } else {
                context.remove(isInRenderingEngineKey);
            }
            context.setURLFactory(oldFactory);
            context.setMode(oldMode);
        }
        return result;
    }

    /**
     * Helper function to prepare the XWiki documents and translations on the context and velocity context. <br>
     * TODO: check how this code could be written only once (not duplicate the prepareDocuments function in XWiki)
     *
     * @param docName the full name of the document to prepare context for
     * @param language the language of the document
     * @throws XWikiException if anything goes wrong accessing documents
     */
    private void setUpDocuments(String docName, String language) throws XWikiException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        // prepare the messaging tools and set them on context
        xwiki.prepareResources(context);

        XWikiDocument doc = xwiki.getDocument(docName, context);
        // setup the xwiki context
        context.put("doc", doc);
        context.put("cdoc", doc);

        XWikiDocument tdoc = doc.getTranslatedDocument(language, context);
        context.put("tdoc", tdoc);
        // and render the xwikivars to have all the variables set ($has*, $blacklistedSpaces, etc)
        context.getWiki().renderTemplate("xwikivars.vm", context);
    }

    /**
     * Helper method to filter a set of annotations according to the criteria in the passed annotation request. The
     * fields in the filter of the request will be interpreted as a filter for equality with the value in the actual
     * annotation, and all the fields conditions will be put together with an "or" operation.
     *
     * @param annotations the collection of annotations to filter
     * @param request the request according which to filter
     * @return the filtered collection of annotations
     */
    protected Collection<Annotation> filterAnnotations(Collection<Annotation> annotations, AnnotationRequest request)
    {
        Collection<Annotation> result = new ArrayList<Annotation>();

        Map<String, List<String>> filters = new HashMap<String, List<String>>();
        for (AnnotationField annotationField : request.getFilter().getFields()) {
            String filterName = annotationField.getName();
            List<String> values = filters.get(filterName);
            if (values == null) {
                values = new ArrayList<String>();
                filters.put(filterName, values);
            }
            if (annotationField.getValue() != null) {
                values.add(annotationField.getValue());
            }
        }

        if (filters.size() == 0) {
            return annotations;
        }

        for (Annotation ann : annotations) {
            boolean matches = true;
            for (Map.Entry<String, List<String>> filter : filters.entrySet()) {
                Object annotationValue = ann.get(filter.getKey());
                // if the values is not set or is not among the requested values,
                if (annotationValue == null || !filter.getValue().contains(annotationValue.toString())) {
                    // it doesn't match and exit
                    matches = false;
                    break;
                }
            }
            // if it matches in the end, add it to the results
            if (matches) {
                result.add(ann);
            }
        }
        return result;
    }

    /**
     * @return the xwiki user in the context.
     */
    protected String getXWikiUser()
    {
        return this.referenceSerializer.serialize(this.xcontextProvider.get().getUserReference());
    }

    /**
     * Helper method to make sure that the context is set to the right document and database name.
     *
     * @param wiki the REST wikiName path parameter
     * @param space the REST spaceName path parameter
     * @param page the REST pageName path parameter
     */
    protected void updateContext(DocumentReference documentReference)
    {
        try {
            // Set the database to the current wiki.
            XWikiContext deprecatedContext = (XWikiContext) execution.getContext().getProperty("xwikicontext");
            deprecatedContext.setWikiId(documentReference.getWikiReference().getName());

            // Set the document to the current document.
            XWiki xwiki = deprecatedContext.getWiki();
            XWikiDocument currentDocument =
                xwiki.getDocument(documentReference, deprecatedContext);
            deprecatedContext.setDoc(currentDocument);
        } catch (Exception e) {
            // Just log it.
            getLogger().error("Failed to update the context for page [{}].", documentReference, e);
        }
    }

    protected Map<String, Object> getMap(AnnotationFieldCollection annotation)
    {
        Map<String, Object> metadataMap = new HashMap<>();
        for (AnnotationField f : annotation.getFields()) {
            metadataMap.put(f.getName(), f.getValue());
        }

        // We perform conversion only if:
        //   1. there is a RequiresHTMLConversion parameter
        //   2. the value of this parameter is exactly "comment"
        //   3. the syntax of comment is given in the parameters
        // Note that this transformation only makes sense if the annotations are saved in an xobject that contains
        // a "comment" property.
        if (metadataMap.containsKey(REQUIRES_HTML_CONVERSION)
            && COMMENT_KEY.equals(metadataMap.get(REQUIRES_HTML_CONVERSION))
            && metadataMap.containsKey(COMMENT_SYNTAX_KEY)) {
            String syntax = (String) metadataMap.get(COMMENT_SYNTAX_KEY);

            String convertedComment = this.htmlConverter.fromHTML(String.valueOf(metadataMap.get(COMMENT_KEY)), syntax);
            metadataMap.put(COMMENT_KEY, convertedComment);
            metadataMap.remove(REQUIRES_HTML_CONVERSION);
        }
        return metadataMap;
    }

    protected void handleTemporaryUploadedFiles(DocumentReference documentReference, Map<String, Object> metadataMap)
        throws XWikiException
    {
        String documentName = this.referenceSerializer.serialize(documentReference);
        boolean canUploadAttachment = this.annotationRightService.canUploadAttachment(documentName, getXWikiUser());
        if (canUploadAttachment && metadataMap.containsKey("uploadedFiles")) {
            XWikiContext context = this.xcontextProvider.get();
            XWikiDocument document = context.getWiki().getDocument(documentReference, context);
            String[] uploadedFiles = StringUtils.split(String.valueOf(metadataMap.get("uploadedFiles")), ",");
            this.temporaryAttachmentSessionsManager
                .attachTemporaryAttachmentsInDocument(document, Arrays.asList(uploadedFiles));
        }
    }

    protected void cleanTemporaryUploadedFiles(DocumentReference documentReference)
    {
        this.temporaryAttachmentSessionsManager.removeUploadedAttachments(documentReference);
    }
}
