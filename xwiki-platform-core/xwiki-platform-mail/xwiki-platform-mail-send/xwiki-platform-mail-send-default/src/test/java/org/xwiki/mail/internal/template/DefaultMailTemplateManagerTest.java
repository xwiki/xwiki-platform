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

import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;

import javax.mail.MessagingException;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.template.DefaultMailTemplateManager}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public class DefaultMailTemplateManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultMailTemplateManager> mocker =
        new MockitoComponentMockingRule<>(DefaultMailTemplateManager.class);

    @Before
    public void setUp() throws Exception
    {
        Execution execution = this.mocker.getInstance(Execution.class);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);

        XWikiContext xwikiContext = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xwikiContext);

        XWiki xwiki = mock(XWiki.class);
        when(xwikiContext.getWiki()).thenReturn(xwiki);
        when(xwiki.getDefaultLocale(xwikiContext)).thenReturn(LocaleUtils.toLocale("en"));

        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(any(DocumentReference.class), eq(xwikiContext))).thenReturn(document);

        BaseObject object = mock(BaseObject.class);

        when(document.getXObjects(any(DocumentReference.class))).thenReturn(Collections.singletonList(object));
    }

    @Test
    public void evaluate() throws Exception
    {
        DocumentAccessBridge documentBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = mock(DocumentReference.class);

        when(documentBridge.getProperty(same(documentReference), any(DocumentReference.class), anyInt(), eq("html")))
            .thenReturn(
                "Hello <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.mocker.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                ((Writer) args[1]).write("Hello <b>John Doe</b> <br />john@doe.com");
                return null;
            }
        }).when(velocityEngine).evaluate(any(VelocityContext.class), any(Writer.class),
            anyString(), eq("Hello <b>${name}</b> <br />${email}"));

        String result =
            this.mocker.getComponentUnderTest().evaluate(documentReference, "html", new HashMap<String, String>());

        assertEquals(result, "Hello <b>John Doe</b> <br />john@doe.com");
    }

    @Test
    public void evaluateWithLanguage() throws Exception
    {
        DocumentAccessBridge documentBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = mock(DocumentReference.class);

        when(documentBridge
            .getObjectNumber(any(DocumentReference.class), any(DocumentReference.class), eq("language"), anyString()))
            .thenReturn(1);
        when(documentBridge.getProperty(any(DocumentReference.class), any(DocumentReference.class), eq(1), eq("html")))
            .thenReturn(
                "Salut <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.mocker.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                ((Writer) args[1]).write("Salut <b>John Doe</b> <br />john@doe.com");
                return null;
            }
        }).when(velocityEngine).evaluate(any(VelocityContext.class), any(Writer.class),
            anyString(), eq("Salut <b>${name}</b> <br />${email}"));

        String result =
            this.mocker.getComponentUnderTest().evaluate(documentReference, "html", new HashMap<String, String>(),
                LocaleUtils.toLocale("fr"));

        assertEquals(result, "Salut <b>John Doe</b> <br />john@doe.com");
    }

    @Test
    public void evaluateWithError() throws Exception
    {
        DocumentAccessBridge documentBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        when(documentBridge.getProperty(same(documentReference), any(DocumentReference.class), anyInt(), eq("html")))
            .thenReturn("Hello <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.mocker.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        when(velocityEngine.evaluate(any(VelocityContext.class), any(Writer.class),
            anyString(), eq("Hello <b>${name}</b> <br />${email}"))).thenThrow(new XWikiVelocityException("Error"));

        try {
            this.mocker.getComponentUnderTest().evaluate(documentReference, "html", new HashMap<String, String>());
            fail("Should have thrown an exception here!");
        } catch (MessagingException expected) {
            assertEquals("Failed to evaluate property [html] for Document [wiki:space.page] and locale [null]",
                expected.getMessage());
        }
    }
}
