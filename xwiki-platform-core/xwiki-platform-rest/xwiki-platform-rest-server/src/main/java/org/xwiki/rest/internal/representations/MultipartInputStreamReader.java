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
package org.xwiki.rest.internal.representations;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiRestComponent;

/**
 * Binds the body of a {@code multipart/form-data} request to a plain {@link InputStream} entity parameter by returning
 * the content of the first uploaded file part. This lets resources that read a raw {@link InputStream} entity (such as
 * the XAR import on {@code POST /wikis/{wikiName}}) accept a browser/curl multipart upload ({@code -F file=@...})
 * transparently, in a way that is independent of the JAX-RS implementation.
 * <p>
 * The part content is streamed straight from the entity stream (never buffered in memory), and the entity stream is the
 * one exposed by the servlet request, so this reader relies on that stream still carrying the body. When an upstream
 * filter has consumed it (e.g. through {@code getParameter()}), the Jersey layer restores it from the servlet-cached
 * parts, which is what makes this reader work in the standard XWiki request chain.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
@Component
@Named("org.xwiki.rest.internal.representations.MultipartInputStreamReader")
@Provider
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Singleton
public class MultipartInputStreamReader implements MessageBodyReader<InputStream>, XWikiRestComponent
{
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return InputStream.class.isAssignableFrom(type);
    }

    @Override
    public InputStream readFrom(Class<InputStream> type, Type genericType, Annotation[] annotations,
        MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException
    {
        try {
            FileItemIterator iterator = new FileUpload().getItemIterator(new EntityRequestContext(mediaType,
                entityStream));
            while (iterator.hasNext()) {
                FileItemStream item = iterator.next();
                if (!item.isFormField()) {
                    // Return the file part content, streamed straight from the entity: it is read by the resource
                    // right away (synchronously), before the iterator is advanced again, so nothing is buffered.
                    return item.openStream();
                }
            }
        } catch (FileUploadException e) {
            throw new WebApplicationException("Failed to parse the multipart/form-data request", e,
                Status.BAD_REQUEST);
        }

        // No file part in the request: behave as an empty body.
        return InputStream.nullInputStream();
    }

    /**
     * Adapts the JAX-RS entity (its media type, which carries the multipart boundary, and its stream) to the
     * {@link RequestContext} expected by Commons FileUpload, so the request can be parsed without any Servlet or JAX-RS
     * implementation dependency.
     */
    private static final class EntityRequestContext implements RequestContext
    {
        private final MediaType mediaType;

        private final InputStream entityStream;

        EntityRequestContext(MediaType mediaType, InputStream entityStream)
        {
            this.mediaType = mediaType;
            this.entityStream = entityStream;
        }

        @Override
        public String getCharacterEncoding()
        {
            return this.mediaType.getParameters().get(MediaType.CHARSET_PARAMETER);
        }

        @Override
        public String getContentType()
        {
            // Includes the boundary parameter, which the parser needs to split the parts.
            return this.mediaType.toString();
        }

        @Override
        public int getContentLength()
        {
            // Unknown: the parser reads until the closing boundary, it does not rely on the length.
            return -1;
        }

        @Override
        public InputStream getInputStream()
        {
            return this.entityStream;
        }
    }
}
