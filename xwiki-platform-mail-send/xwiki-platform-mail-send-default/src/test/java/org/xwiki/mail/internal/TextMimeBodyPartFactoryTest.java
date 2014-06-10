/*
 *
 *  * See the NOTICE file distributed with this work for additional
 *  * information regarding copyright ownership.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.xwiki.mail.internal;

import java.util.Collections;
import java.util.Map;

import javax.mail.internet.MimeBodyPart;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link TextMimeBodyPartFactory}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class TextMimeBodyPartFactoryTest
{
    @Test
    public void createTextBodyPart() throws Exception
    {
        // Step 1: Create a TextMimeBodyPartFactory object
        TextMimeBodyPartFactory textPartFactory = new TextMimeBodyPartFactory();

        // Step 2: Create the text body part
        MimeBodyPart textPart = textPartFactory.create("Lorem ipsum");

        assertEquals("Lorem ipsum", textPart.getContent());
        assertEquals("text/plain", textPart.getContentType());
    }

    @Test
    public void createTextBodyPartWithHeaders() throws Exception
    {
        // Step 1: Create a TextMimeBodyPartFactory object
        TextMimeBodyPartFactory textPartFactory = new TextMimeBodyPartFactory();

        // Step 2: Add parameters Map
        Map<String, String> headers = Collections.singletonMap("Content-Transfer-Encoding", "quoted-printable");
        Map<String, Object> parameters = Collections.<String, Object>singletonMap("headers", headers);


        // Step 3: Create the text body part
        MimeBodyPart textPart = textPartFactory.create("Lorem ipsum", parameters);

        assertEquals("Lorem ipsum", textPart.getContent());
        assertEquals("text/plain", textPart.getContentType());
        String[] headersList = new String[]{"quoted-printable"};
        assertArrayEquals(headersList, textPart.getHeader("Content-Transfer-Encoding"));
    }
}
