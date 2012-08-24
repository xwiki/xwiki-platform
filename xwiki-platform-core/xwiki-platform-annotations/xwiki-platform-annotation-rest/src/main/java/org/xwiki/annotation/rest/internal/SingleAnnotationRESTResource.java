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

import java.util.logging.Level;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.AnnotationServiceException;
import org.xwiki.annotation.rest.model.jaxb.AnnotationField;
import org.xwiki.annotation.rest.model.jaxb.AnnotationRequest;
import org.xwiki.annotation.rest.model.jaxb.AnnotationResponse;
import org.xwiki.annotation.rest.model.jaxb.AnnotationUpdateRequest;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiException;

/**
 * This class allow to do delete a single annotation.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Named("org.xwiki.annotation.rest.internal.SingleAnnotationRESTResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/annotation/{id}")
@Singleton
public class SingleAnnotationRESTResource extends AbstractAnnotationRESTResource
{
    /**
     * Entity reference serializer used to get reference to the document to perform annotation operation on.
     */
    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    /**
     * Deletes the specified annotation.
     * 
     * @param space the space of the document to delete the annotation from
     * @param page the name of the document to delete the annotation from
     * @param wiki the wiki of the document to delete the annotation from
     * @param id the id of the annotation to delete
     * @param request the annotation request to configure the returned annotated content after the execution of the
     *            operation
     * @return a annotation response for which the response code will be 0 in case of success and non-zero otherwise
     */
    @DELETE
    public AnnotationResponse doDelete(@PathParam("spaceName") String space, @PathParam("pageName") String page,
        @PathParam("wikiName") String wiki, @PathParam("id") String id, AnnotationRequest request)
    {
        try {
            // Initialize the context with the correct value.
            updateContext(wiki, space, page);

            DocumentReference docRef = new DocumentReference(wiki, space, page);
            String documentName = referenceSerializer.serialize(docRef);

            // check access to this function
            if (!annotationRightService.canEditAnnotation(id, documentName, getXWikiUser())) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }
            // remove the annotation
            annotationService.removeAnnotation(documentName, id);
            // and then return the annotated content, as specified by the annotation request
            AnnotationResponse response = getSuccessResponseWithAnnotatedContent(documentName, request);
            return response;
        } catch (XWikiException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return getErrorResponse(e);
        } catch (AnnotationServiceException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return getErrorResponse(e);
        }
    }

    /**
     * Updates the specified annotation with the values of the fields in received collection.
     * 
     * @param space the space of the document to update the annotation from
     * @param page the name of the document to update the annotation from
     * @param wiki the wiki of the document to update the annotation from
     * @param id the id of the annotation to update
     * @param updateRequest the request to update the annotation pointed by the id
     * @return a annotation response for which the response code will be 0 in case of success and non-zero otherwise
     */
    @PUT
    public AnnotationResponse doUpdate(@PathParam("spaceName") String space, @PathParam("pageName") String page,
        @PathParam("wikiName") String wiki, @PathParam("id") String id, AnnotationUpdateRequest updateRequest)
    {
        try {
            // Initialize the context with the correct value.
            updateContext(wiki, space, page);

            DocumentReference docRef = new DocumentReference(wiki, space, page);
            String documentName = referenceSerializer.serialize(docRef);

            // check access to this function
            if (!annotationRightService.canEditAnnotation(id, documentName, getXWikiUser())) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            // id from the url
            Annotation newAnnotation = new Annotation(id);
            // fields from the posted content
            for (AnnotationField field : updateRequest.getAnnotation().getFields()) {
                newAnnotation.set(field.getName(), field.getValue());
            }
            // overwrite author if any was set because we know better who's logged in
            newAnnotation.setAuthor(getXWikiUser());
            // and update
            annotationService.updateAnnotation(documentName, newAnnotation);
            // and then return the annotated content, as specified by the annotation request
            AnnotationResponse response = getSuccessResponseWithAnnotatedContent(documentName, updateRequest);
            return response;
        } catch (XWikiException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return getErrorResponse(e);
        } catch (AnnotationServiceException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return getErrorResponse(e);
        }
    }
}
