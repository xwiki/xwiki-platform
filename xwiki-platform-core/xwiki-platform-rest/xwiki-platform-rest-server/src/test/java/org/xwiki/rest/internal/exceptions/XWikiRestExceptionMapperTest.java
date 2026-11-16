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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.store.DocumentRevisionConflictException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link XWikiRestExceptionMapper}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiRestExceptionMapperTest
{
    private static final DocumentRevisionConflictException CONFLICT =
        new DocumentRevisionConflictException(new DocumentReference("wiki", "space", "page"), "1.1", "2.1");

    @InjectMockComponents
    private XWikiRestExceptionMapper mapper;

    @Test
    void toResponseWithConflict()
    {
        Response response = this.mapper.toResponse(new XWikiRestException(CONFLICT));

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        assertEquals(CONFLICT.getMessage(), response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
    }

    @Test
    void toResponseWithWrappedConflict()
    {
        XWikiException wrapper = new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC, "Failed to save", CONFLICT);

        Response response = this.mapper.toResponse(new XWikiRestException(wrapper));

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        assertEquals(CONFLICT.getMessage(), response.getEntity());
    }

    @Test
    void toResponseWithOtherFailure()
    {
        XWikiException failure = new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC, "Failed to save");
        XWikiRestException exception = new XWikiRestException(failure);

        Response response = this.mapper.toResponse(exception);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(exception.getMessage(), response.getEntity());
    }
}
