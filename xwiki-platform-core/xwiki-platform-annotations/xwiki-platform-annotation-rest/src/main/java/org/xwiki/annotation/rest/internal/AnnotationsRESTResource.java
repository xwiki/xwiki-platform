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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.annotation.AnnotationServiceException;
import org.xwiki.annotation.rest.model.jaxb.AnnotationAddRequest;
import org.xwiki.annotation.rest.model.jaxb.AnnotationField;
import org.xwiki.annotation.rest.model.jaxb.AnnotationFieldCollection;
import org.xwiki.annotation.rest.model.jaxb.AnnotationRequest;
import org.xwiki.annotation.rest.model.jaxb.AnnotationResponse;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Named("org.xwiki.annotation.rest.internal.AnnotationsRESTResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/annotations")
@Singleton
public class AnnotationsRESTResource extends AbstractAnnotationRESTResource
{
    /**
     * Entity reference serializer used to get reference to the document to perform annotation operation on.
     */
    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    /**
     * @param wiki the wiki of the document to get annotations for
     * @param space the space of the document to get annotations for
     * @param page the name of the document to get annotation for
     * @param request the extra fields to be returned from the annotation structure when returning the annotation resume
     *            to the client side
     * @return annotations of a given XWiki page. Note that we're returning a response holding the AnnotatedContent
     *         instead of an AnnotatedContent object because we need to be able to set custom expire fields to prevent
     *         IE from caching this resource.
     */
    @GET
    public Response doGetAnnotatedContent(@PathParam("spaceName") String space, @PathParam("pageName") String page,
        @PathParam("wikiName") String wiki, AnnotationRequest request)
    {
        try {
            DocumentReference docRef = new DocumentReference(wiki, space, page);
            String documentName = referenceSerializer.serialize(docRef);
            // check access to this function
            if (!annotationRightService.canViewAnnotatedTarget(documentName, getXWikiUser())) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            AnnotationResponse response = getSuccessResponseWithAnnotatedContent(documentName, request);
            // make this content expire now because cacheControl is not implemented in this version of restlet
            return Response.ok(response).expires(new Date()).build();
        } catch (AnnotationServiceException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return Response.ok(getErrorResponse(e)).build();
        } catch (XWikiException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return Response.ok(getErrorResponse(e)).build();
        }
    }

    /**
     * Add annotation to a given page.
     * 
     * @param wiki the wiki of the document to add annotation on
     * @param space the space of the document to add annotation on
     * @param page the name of the document to add annotation on
     * @param request the request object with the annotation to be added
     * @return AnnotationRequestResponse, responseCode = 0 if no error
     */
    @POST
    public AnnotationResponse doPostAnnotation(@PathParam("wikiName") String wiki,
        @PathParam("spaceName") String space, @PathParam("pageName") String page, AnnotationAddRequest request)
    {
        try {
            DocumentReference docRef = new DocumentReference(wiki, space, page);
            String documentName = referenceSerializer.serialize(docRef);

            // check access to this function
            if (!annotationRightService.canAddAnnotation(documentName, getXWikiUser())) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            // add the annotation
            Map<String, Object> annotationMetadata = getMap(request.getAnnotation());
            annotationService.addAnnotation(documentName, request.getSelection(), request.getSelectionContext(),
                request.getSelectionOffset(), getXWikiUser(), annotationMetadata);
            // and then return the annotated content, as specified by the annotation request
            AnnotationResponse response = getSuccessResponseWithAnnotatedContent(documentName, request);
            return response;
        } catch (AnnotationServiceException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return getErrorResponse(e);
        } catch (XWikiException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return getErrorResponse(e);
        }
    }

    /**
     * Builds a simple map from a field collection.
     * 
     * @param fields the collection of fields to build a map for
     * @return a map of the fields, as string key and Object value pair
     */
    protected Map<String, Object> getMap(AnnotationFieldCollection fields)
    {
        Map<String, Object> metadataMap = new HashMap<String, Object>();
        for (AnnotationField f : fields.getFields()) {
            metadataMap.put(f.getName(), f.getValue());
        }

        return metadataMap;
    }
}
