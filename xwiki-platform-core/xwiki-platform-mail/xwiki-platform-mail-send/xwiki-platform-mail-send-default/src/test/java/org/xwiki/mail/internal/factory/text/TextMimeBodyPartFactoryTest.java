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
package org.xwiki.mail.internal.factory.text;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeBodyPart;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link TextMimeBodyPartFactory}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
class TextMimeBodyPartFactoryTest
{
    @InjectMockComponents
    private TextMimeBodyPartFactory factory;

    @Test
    void createWithoutMimeTypePassed() throws Exception
    {
        MimeBodyPart bodyPart = this.factory.create("Lorem ipsum", Collections.emptyMap());

        assertEquals("Lorem ipsum", bodyPart.getContent());
        assertEquals("text/plain; charset=UTF-8", bodyPart.getContentType());
    }

    @Test
    void createWithMimeTypePassedAndWithHeaders() throws Exception
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("headers", Collections.singletonMap("Content-Transfer-Encoding", "quoted-printable"));
        parameters.put("mimetype", "text/calendar");

        MimeBodyPart bodyPart = this.factory.create("Lorem ipsum", parameters);

        assertEquals("Lorem ipsum", bodyPart.getContent());
        assertEquals("text/calendar", bodyPart.getContentType());
        assertArrayEquals(new String[]{ "quoted-printable" }, bodyPart.getHeader("Content-Transfer-Encoding"));
    }
}
