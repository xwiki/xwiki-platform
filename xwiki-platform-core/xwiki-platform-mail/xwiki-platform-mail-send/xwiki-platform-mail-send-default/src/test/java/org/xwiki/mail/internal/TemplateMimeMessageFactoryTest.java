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

import java.io.Writer;
import java.util.Collections;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.velocity.VelocityContext;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemplateMimeMessageFactory}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public class TemplateMimeMessageFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<TemplateMimeMessageFactory> mocker =
            new MockitoComponentMockingRule<>(TemplateMimeMessageFactory.class);

    @Test
    public void createMessage() throws Exception
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        VelocityEngine velocityEngine = mock(VelocityEngine.class);

        DocumentAccessBridge documentBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        when(documentBridge.getProperty(same(documentReference), any(DocumentReference.class), eq("subject")))
                .thenReturn(
                        "${company} news");
        VelocityManager velocityManager = this.mocker.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                ((Writer) args[1]).write("XWiki news");
                return null;
            }
        }).when(velocityEngine).evaluate(any(VelocityContext.class), any(Writer.class),
                anyString(), eq("${company} news"));

        Session session = Session.getDefaultInstance(new Properties());

        MimeMessage message = this.mocker.getComponentUnderTest()
                .createMessage(session, "news@xwiki.org", "john@doe.com", documentReference, Collections
                        .<String, Object>singletonMap("company", "XWiki"));

        assertEquals("XWiki news", message.getSubject());
        assertArrayEquals(new InternetAddress[]{ new InternetAddress("news@xwiki.org") }, message.getFrom());
        assertArrayEquals(new InternetAddress[]{ new InternetAddress("john@doe.com") },
                message.getRecipients(MimeMessage.RecipientType.TO));
    }
}