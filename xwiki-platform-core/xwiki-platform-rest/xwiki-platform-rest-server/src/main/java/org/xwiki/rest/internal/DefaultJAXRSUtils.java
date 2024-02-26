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
package org.xwiki.rest.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.StringTokenizer;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Encoded;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.rest.JAXRSUtils;

/**
 * Default implementation of {@link JAXRSUtils}.
 * 
 * @version $Id$
 * @since 16.2.0RC1
 */
@Component
@Singleton
public class DefaultJAXRSUtils implements JAXRSUtils
{
    @Inject
    private Container container;

    @Override
    public Charset getCharset(MediaType mediaType)
    {
        if (mediaType != null) {
            String name = mediaType.getParameters().get(MediaType.CHARSET_PARAMETER);

            if (name != null) {
                return Charset.forName(name);
            }
        }

        return StandardCharsets.UTF_8;
    }

    private boolean decode(Annotation[] annotations)
    {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == Encoded.class) {
                return false;
            }
        }

        return true;
    }

    @Override
    public MultivaluedMap<String, String> readForm(MediaType mediaType, Annotation[] annotations,
        InputStream entityStream) throws IOException
    {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();

        boolean decode = decode(annotations);
        Charset charset = getCharset(mediaType);

        String encoded = IOUtils.toString(entityStream, charset);

        // If the content is empty, there is a very high change that it was actually consumed by a Servlet filter
        // upstream, so we have to the from through servlet request parameters
        if (encoded.isEmpty()) {
            readFormFromEntity(map);
        } else {
            readFormFromEntity(encoded, decode, charset, map);
        }

        return map;
    }

    private void readFormFromEntity(MultivaluedMap<String, String> map)
    {
        // Get the current request
        Request request = this.container.getRequest();
        if (request instanceof ServletRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getHttpServletRequest();

            // Gather the request parameters
            httpRequest.getParameterMap().forEach((k, v) -> map.put(k, Arrays.asList(v)));
        }
    }

    private void readFormFromEntity(String encoded, boolean decode, Charset charset, MultivaluedMap<String, String> map)
    {
        StringTokenizer tokenizer = new StringTokenizer(encoded, "&");
        String token;
        try {
            while (tokenizer.hasMoreTokens()) {
                token = tokenizer.nextToken();
                int idx = token.indexOf('=');
                if (idx < 0) {
                    map.add(decode ? URLDecoder.decode(token, charset) : token, null);
                } else if (idx > 0) {
                    if (decode) {
                        map.add(URLDecoder.decode(token.substring(0, idx), charset),
                            URLDecoder.decode(token.substring(idx + 1), charset));
                    } else {
                        map.add(token.substring(0, idx), token.substring(idx + 1));
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex);
        }
    }
}
