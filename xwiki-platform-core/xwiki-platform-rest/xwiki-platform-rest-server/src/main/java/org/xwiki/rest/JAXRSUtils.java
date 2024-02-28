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
package org.xwiki.rest;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Provider various tools to help implement {@link MessageBodyWriter} and {@link MessageBodyReader}.
 * 
 * @version $Id$
 * @since 16.2.0RC1
 */
@Role
@Unstable
public interface JAXRSUtils
{
    /**
     * Extract the charset from the media type.
     * 
     * @param mediaType the media type of the request
     * @return the charset of the media type or {@link java.nio.charset.StandardCharsets#UTF_8} if none is provided
     */
    Charset getCharset(MediaType mediaType);

    /**
     * @param mediaType the media type of the HTTP entity
     * @param annotations the annotation associated with the method
     * @param entityStream the stream containing the request entity
     * @return the map containing the form metadata
     * @throws IOException when failing to read the stream
     */
    MultivaluedMap<String, String> readForm(MediaType mediaType, Annotation[] annotations, InputStream entityStream)
        throws IOException;
}
