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
package org.xwiki.rest.internal.exceptions;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.QueryException;
import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.rest.XWikiRestException;

import com.xpn.xwiki.XWikiException;

/**
 * Exception mapper for XWikiRestException, the exception raised by REST resources in case of failure.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("org.xwiki.rest.internal.exceptions.XWikiRestExceptionMapper")
@Provider
@Singleton
public class XWikiRestExceptionMapper implements ExceptionMapper<XWikiRestException>, XWikiRestComponent
{
    @Override
    public Response toResponse(XWikiRestException exception)
    {
        Throwable cause = exception.getCause();

        if (cause instanceof XWikiException) {
            XWikiException xwikiException = (XWikiException) cause;
            if (xwikiException.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                return Response.status(Status.UNAUTHORIZED).entity(exception.getMessage()).type(MediaType.TEXT_PLAIN)
                        .build();
            }
        } else if (cause instanceof QueryException) {
            QueryException queryException = (QueryException) cause;

            return Response.serverError()
                    .entity(String.format("%s\n%s\n", exception.getMessage(), queryException.getCause().getMessage()))
                    .type(MediaType.TEXT_PLAIN).build();
        }

        return Response.serverError().entity(exception.getMessage()).type(MediaType.TEXT_PLAIN).build();
    }
}
