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

import java.util.Locale;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.xwiki.annotation.rest.model.jaxb.AnnotationAddRequest;
import org.xwiki.annotation.rest.model.jaxb.AnnotationResponse;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;

/**
 * @version $Id$
 * @since 11.3RC1
 * @since 10.11.7
 */
@Component
@Named("org.xwiki.annotation.rest.internal.AnnotationsTranslationRESTResource")
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/translations/{language}/annotations")
@Singleton
public class AnnotationsTranslationRESTResource extends AnnotationsRESTResource
{
    /**
     * @param wiki the wiki of the document to get annotations for
     * @param spaceNames the space names of the document to get annotations for
     * @param page the name of the document to get annotation for
     * @param language the language of the translation of the document to get the annotation for
     * @return annotations of a given XWiki page. Note that we're returning a response holding the AnnotatedContent
     *         instead of an AnnotatedContent object because we need to be able to set custom expire fields to prevent
     *         IE from caching this resource.
     * @throws XWikiRestException when failing to parse space
     */
    @GET
    public Response doGetAnnotatedContent(@PathParam("spaceName") @Encoded String spaceNames,
        @PathParam("pageName") String page, @PathParam("wikiName") String wiki, @PathParam("language") String language)
        throws XWikiRestException
    {
        DocumentReference documentReference = new DocumentReference(wiki, parseSpaceSegments(spaceNames), page,
            LocaleUtils.toLocale(language, Locale.ROOT));
        return getAnnotatedContent(documentReference);
    }

    /**
     * Add annotation to a given page.
     *
     * @param wiki the wiki of the document to add annotation on
     * @param spaceNames the space names of the document to add annotation on
     * @param page the name of the document to add annotation on
     * @param language he language of the translation of the document to add the annotation on
     * @param request the request object with the annotation to be added
     * @return AnnotationRequestResponse, responseCode = 0 if no error
     * @throws XWikiRestException when failing to parse space
     */
    @POST
    public AnnotationResponse doPostAnnotation(@PathParam("wikiName") String wiki,
        @PathParam("spaceName") @Encoded String spaceNames, @PathParam("pageName") String page,
        @PathParam("language") String language, AnnotationAddRequest request) throws XWikiRestException
    {
        DocumentReference documentReference = new DocumentReference(wiki, parseSpaceSegments(spaceNames), page);
        return postAnnotation(documentReference, request);
    }
}
