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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.annotation.AnnotationServiceException;
import org.xwiki.annotation.rest.model.jaxb.AnnotationAddRequest;
import org.xwiki.annotation.rest.model.jaxb.AnnotationField;
import org.xwiki.annotation.rest.model.jaxb.AnnotationFieldCollection;
import org.xwiki.annotation.rest.model.jaxb.AnnotationRequest;
import org.xwiki.annotation.rest.model.jaxb.AnnotationResponse;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 * @since 16.2.0RC1
 */
public abstract class AbstractAnnotationsRESTResource extends AbstractAnnotationRESTResource
{
    /**
     * Request parameter to request a field in an annotation request.
     */
    private static final String ANNOTATION_REQUEST_REQUESTED_FIELD_PARAMETER = "request_field";

    /**
     * Request parameter prefix for a filter in an annotation request.
     */
    private static final String ANNOTATION_REQUEST_FILTER_PARAMETER_PREFIX = "filter_";

    protected Response getAnnotatedContent(DocumentReference documentReference)
    {
        try {
            // Initialize the context with the correct value.
            updateContext(documentReference);

            String documentName = this.referenceSerializer.serialize(documentReference);

            // check access to this function
            if (!this.annotationRightService.canViewAnnotatedTarget(documentName, getXWikiUser())) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            // Manually construct the Annotation request entity
            MultivaluedMap<String, String> parameters = this.uriInfo.getQueryParameters();
            AnnotationRequest request = new AnnotationRequest();
            AnnotationFieldCollection fields = new AnnotationFieldCollection();
            List<AnnotationField> annotationFields = new ArrayList<>();
            AnnotationRequest.Request requestedFields = new AnnotationRequest.Request();
            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                String name = entry.getKey();
                if (StringUtils.startsWith(name, ANNOTATION_REQUEST_FILTER_PARAMETER_PREFIX)) {
                    for (String value : entry.getValue()) {
                        AnnotationField field = new AnnotationField();
                        field.setName(StringUtils.substringAfter(name, ANNOTATION_REQUEST_FILTER_PARAMETER_PREFIX));
                        field.setValue(value);
                        annotationFields.add(field);
                    }
                } else if (StringUtils.equals(name, ANNOTATION_REQUEST_REQUESTED_FIELD_PARAMETER)) {
                    requestedFields.getFields().addAll(entry.getValue());
                }
            }
            request.setRequest(requestedFields);
            fields.getFields().addAll(annotationFields);
            request.setFilter(fields);

            AnnotationResponse response = getSuccessResponseWithAnnotatedContent(documentName, request);
            return Response.ok(response).build();
        } catch (AnnotationServiceException | XWikiException e) {
            getLogger().error(e.getMessage(), e);
            return Response.ok(getErrorResponse(e)).build();
        }
    }

    protected AnnotationResponse postAnnotation(DocumentReference documentReference, AnnotationAddRequest request)
    {
        try {
            // Initialize the context with the correct value.
            updateContext(documentReference);

            String documentName = this.referenceSerializer.serialize(documentReference);

            // check access to this function
            if (!this.annotationRightService.canAddAnnotation(documentName, getXWikiUser())) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            // add the annotation
            Map<String, Object> annotationMetadata = getMap(request.getAnnotation());

            this.handleTemporaryUploadedFiles(documentReference, annotationMetadata);
            this.annotationService.addAnnotation(documentName, request.getSelection(), request.getSelectionContext(),
                request.getSelectionOffset(), getXWikiUser(), annotationMetadata);
            this.cleanTemporaryUploadedFiles(documentReference);

            // and then return the annotated content, as specified by the annotation request
            return getSuccessResponseWithAnnotatedContent(documentName, request);
        } catch (AnnotationServiceException | XWikiException e) {
            getLogger().error(e.getMessage(), e);
            return getErrorResponse(e);
        }
    }
}
