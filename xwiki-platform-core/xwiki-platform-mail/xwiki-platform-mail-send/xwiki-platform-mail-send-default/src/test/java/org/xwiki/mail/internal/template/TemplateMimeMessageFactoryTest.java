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
package org.xwiki.mail.internal.template;

import java.util.Collections;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.mail.internal.template.DefaultMailTemplateManager;
import org.xwiki.mail.internal.template.TemplateMimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.template.TemplateMimeMessageFactory}.
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

        DefaultMailTemplateManager mailTemplateManager = this.mocker.getInstance(DefaultMailTemplateManager.class);
        when(mailTemplateManager.evaluate(same(documentReference), eq("subject"), anyMap(), anyString())).thenReturn(
                "XWiki news");

        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage message = this.mocker.getComponentUnderTest()
                .createMessage(session, documentReference, Collections.<String, Object>singletonMap("company", "XWiki"));

        assertEquals("XWiki news", message.getSubject());
    }
}