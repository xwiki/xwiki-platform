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
package org.xwiki.attachment.validation.internal;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.json.JSONObject;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiRestComponent;

/**
 * Exception mapper for {@link AttachmentValidationException}.
 *
 * @version $Id$
 * @since 14.10
 */
@Component
@Named("org.xwiki.attachment.validation.internal.AttachmentValidationExceptionMapper")
@Provider
@Singleton
public class AttachmentValidationExceptionMapper implements ExceptionMapper<AttachmentValidationException>,
    XWikiRestComponent
{
    @Override
    public Response toResponse(AttachmentValidationException exception)
    {
        JSONObject entity = new JSONObject();
        entity.put("message", exception.getMessage());
        entity.put("translationKey", exception.getTranslationKey());
        entity.put("translationParameters", exception.getTranslationParameters());
        return Response
            .serverError()
            .entity(entity.toString())
            .type(MediaType.APPLICATION_JSON_TYPE)
            .status(exception.getHttpStatus())
            .build();
    }
}
