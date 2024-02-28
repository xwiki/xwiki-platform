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
package org.xwiki.rest.jersey.internal;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Make sure Jackson is using the same {@link ObjectMapper} than when XWiki was based on Restlet (which mainly means not
 * taking into account JAX-RS annotations).
 * 
 * @version $Id$
 * @since 16.2.0RC1
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class RestletJacksonContextResolver implements ContextResolver<ObjectMapper>
{
    private ObjectMapper objectMapper;

    /**
     * Creates a new instance.
     */
    public RestletJacksonContextResolver()
    {
        JsonMapper.Builder builder = JsonMapper.builder();

        // Disable all annotation handling to make sure Jackson is not going to try to take into account JAX-RS
        // annotations
        builder.disable(MapperFeature.USE_ANNOTATIONS);

        this.objectMapper = builder.build();
    }

    @Override
    public ObjectMapper getContext(Class<?> objectType)
    {
        return this.objectMapper;
    }
}
