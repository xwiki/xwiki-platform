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
package org.xwiki.rest.internal.representations.comments;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;
import org.xwiki.rest.JAXRSUtils;
import org.xwiki.rest.model.jaxb.Comment;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link FormUrlEncodedCommentReader}.
 *
 * @version $Id$
 */
@ComponentTest
class FormUrlEncodedCommentReaderTest
{
    @InjectMockComponents
    private FormUrlEncodedCommentReader reader;

    @MockComponent
    private JAXRSUtils jaxrs;

    @Test
    void readsTextAndReplyTo() throws Exception
    {
        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.putSingle("text", "Nice post");
        form.putSingle("replyTo", "5");
        when(this.jaxrs.readForm(any(), any(), any())).thenReturn(form);

        Comment comment = this.reader.readFrom(Comment.class, null, null, null, null, null);

        assertEquals("Nice post", comment.getText());
        assertEquals(5, comment.getReplyTo());
    }

    @Test
    void invalidReplyToIsSwallowedAndLeftUnset() throws Exception
    {
        // Pin current behavior: a non-numeric "replyTo" throws NumberFormatException internally,
        // which is caught and ignored, leaving replyTo unset (null).
        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.putSingle("text", "Nice post");
        form.putSingle("replyTo", "abc");
        when(this.jaxrs.readForm(any(), any(), any())).thenReturn(form);

        Comment comment = this.reader.readFrom(Comment.class, null, null, null, null, null);

        assertEquals("Nice post", comment.getText());
        assertNull(comment.getReplyTo());
    }
}
