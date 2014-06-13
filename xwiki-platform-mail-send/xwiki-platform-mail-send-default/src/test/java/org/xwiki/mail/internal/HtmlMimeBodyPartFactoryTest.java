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
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.api.Attachment;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

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
    public void createWhenOnlyHTMLContent() throws Exception
    {
        MimeBodyPart bodyPart = this.mocker.getComponentUnderTest().create("<p>some html</p>");

        assertResult("Content-Type: text/html.*<p>some html</p>", bodyPart);
    }

    @Test
    public void createWhenHTMLAndAlternateTextContent() throws Exception
    {
        MimeBodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setContent("some text", "text/plain; charset=" + StandardCharsets.UTF_8.name());
        textBodyPart.setHeader("Content-Type", "text/plain");

        MimeBodyPartFactory defaultBodyPartFactory = this.mocker.getInstance(MimeBodyPartFactory.class);
        when(defaultBodyPartFactory.create("some text")).thenReturn(textBodyPart);

        MimeBodyPart bodyPart = this.mocker.getComponentUnderTest().create("<p>some html</p>",
            Collections.<String, Object>singletonMap("alternate", "some text"));

        // Note: we also verify the order here, it's important to have the text before the html!
        assertResult(".*Content-Type: text/plain.*some text.*Content-Type: text/html.*<p>some html</p>.*", bodyPart);
    }

    @Test
    @Ignore
    public void createWhenHTMLAndEmbeddedImages() throws Exception
    {
        Attachment attachment = mock(Attachment.class);
        MimeBodyPart bodyPart = this.mocker.getComponentUnderTest().create("<p>some html</p>",
            Collections.<String, Object>singletonMap("attachments", Arrays.asList(attachment)));
    }

    private void assertResult(String regex, MimeBodyPart htmlBodyPart) throws Exception
    {
        String result = getRawContent(htmlBodyPart);
        assertTrue("Got: [" + result + "]", Pattern.compile(regex, Pattern.DOTALL).matcher(result).matches());
    }

    private String getRawContent(MimeBodyPart htmlBodyPart) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        htmlBodyPart.writeTo(baos);
        baos.close();
        return baos.toString();
    }

}