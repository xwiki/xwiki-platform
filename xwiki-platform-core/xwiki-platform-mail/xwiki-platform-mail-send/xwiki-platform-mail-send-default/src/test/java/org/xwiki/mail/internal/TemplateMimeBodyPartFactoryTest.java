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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.velocity.VelocityEngine;

import com.xpn.xwiki.api.Attachment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemplateMimeBodyPartFactory}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public class TemplateMimeBodyPartFactoryTest
{
    private DocumentReference documentReference = mock(DocumentReference.class);

    private VelocityEngine velocityEngine = mock(VelocityEngine.class);

    @Rule
    public MockitoComponentMockingRule<TemplateMimeBodyPartFactory> mocker =
        new MockitoComponentMockingRule<>(TemplateMimeBodyPartFactory.class);

    @Before
    public void setUp() throws Exception
    {
        DefaultMailTemplateManager mailTemplateManager = this.mocker.getInstance(DefaultMailTemplateManager.class);
        when(mailTemplateManager.evaluate(this.documentReference, "text", new HashMap<String, String>())).thenReturn(
                "Hello John Doe, john@doe.com");
        when(mailTemplateManager.evaluate(this.documentReference, "html", new HashMap<String, String>())).thenReturn(
                "Hello <b>John Doe</b> <br />john@doe.com");
    }

    @Test
    public void createWithoutAttachment() throws Exception
    {
        MimeBodyPartFactory<String> htmlMimeBodyPartFactory = this.mocker.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class), "text/html");

        this.mocker.getComponentUnderTest().create(this.documentReference,
            Collections.<String, Object>singletonMap("velocityVariables", new HashMap<String, String>()));

        verify(htmlMimeBodyPartFactory).create("Hello <b>John Doe</b> <br />john@doe.com",
            Collections.<String, Object>singletonMap("alternate", "Hello John Doe, john@doe.com"));
    }

    @Test
    public void createWithAttachment() throws Exception
    {
        MimeBodyPartFactory<String> htmlMimeBodyPartFactory = this.mocker.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class), "text/html");

        Attachment attachment = mock(Attachment.class);
        List<Attachment> attachments = Collections.singletonList(attachment);

        Map<String, Object> bodyPartParameters = new HashMap<>();
        bodyPartParameters.put("velocityVariables", new HashMap<String, String>());
        bodyPartParameters.put("attachments", attachments);

        this.mocker.getComponentUnderTest().create(documentReference, bodyPartParameters);

        Map<String, Object> htmlParameters = new HashMap<>();
        htmlParameters.put("alternate", "Hello John Doe, john@doe.com");
        htmlParameters.put("attachments", attachments);

        verify(htmlMimeBodyPartFactory).create("Hello <b>John Doe</b> <br />john@doe.com", htmlParameters);
    }


}