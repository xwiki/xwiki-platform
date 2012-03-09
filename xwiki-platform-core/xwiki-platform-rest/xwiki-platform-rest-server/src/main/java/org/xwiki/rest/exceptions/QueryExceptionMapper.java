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
package org.xwiki.rest.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.QueryException;
import org.xwiki.rest.XWikiRestComponent;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.exceptions.QueryExceptionMapper")
@Provider
public class QueryExceptionMapper implements ExceptionMapper<QueryException>, XWikiRestComponent
{
    @Override
    public Response toResponse(QueryException exception)
    {
        return Response.serverError()
            .entity(String.format("%s\n%s\n", exception.getMessage(), exception.getCause().getMessage()))
            .type(MediaType.TEXT_PLAIN).build();
    }

}
