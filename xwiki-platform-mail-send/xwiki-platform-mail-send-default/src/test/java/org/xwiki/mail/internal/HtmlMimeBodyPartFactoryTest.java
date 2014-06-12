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
package org.xwiki.mail.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HtmlMimeBodyPartFactory}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class HtmlMimeBodyPartFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<MimeBodyPartFactory> mocker =
            new MockitoComponentMockingRule<MimeBodyPartFactory>(HtmlMimeBodyPartFactory.class);

    @Test
    public void createHtmlBodyPart() throws Exception
    {
        MimeBodyPart htmlBodyPart = this.mocker.getComponentUnderTest().create("<p>Lorem ipsum</p>");

        String rawContent = getRawContent(htmlBodyPart);

        assertTrue(rawContent.contains("<p>Lorem ipsum</p>"));
        assertTrue(rawContent.contains("Content-Type: text/html"));
    }

    private String getRawContent(MimeBodyPart htmlBodyPart) throws IOException, MessagingException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        htmlBodyPart.writeTo(baos);
        baos.close();
        return baos.toString();
    }

    @Test
    public void createHtmlBodyPartWithAlternate() throws Exception
    {
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent("text plain", "text/plain; charset=" + StandardCharsets.UTF_8.name());
        bodyPart.setHeader("Content-Type", "text/plain");

        MimeBodyPartFactory defaultBodyPart = this.mocker.getInstance(MimeBodyPartFactory.class);
        when(defaultBodyPart.create(anyString())).thenReturn(bodyPart);

        MimeBodyPart htmlBodyPart = this.mocker.getComponentUnderTest().create("<p>Lorem ipsum</p>",
                Collections.<String, Object>singletonMap("alternate", "Lorem ipsum"));

        String rawContent = getRawContent(htmlBodyPart);

        assertTrue(rawContent.contains("text plain"));
        assertTrue(rawContent.contains("Content-Type: text/plain"));

        assertTrue(rawContent.contains("<p>Lorem ipsum</p>"));
        assertTrue(rawContent.contains("Content-Type: text/html"));
    }

    @Test
    @Ignore
    public void createHtmlBodyPartWitEmbeddedImages() throws Exception
    {

    }
}