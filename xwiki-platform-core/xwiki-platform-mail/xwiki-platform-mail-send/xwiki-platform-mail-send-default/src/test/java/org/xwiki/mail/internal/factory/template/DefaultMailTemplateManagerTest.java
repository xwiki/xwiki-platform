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
package org.xwiki.mail.internal.factory.template;

import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Provider;
import javax.mail.MessagingException;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.velocity.VelocityEvaluator;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.template.DefaultMailTemplateManager}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public class DefaultMailTemplateManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultMailTemplateManager> mocker =
        new MockitoComponentMockingRule<>(DefaultMailTemplateManager.class);

    private XWiki xwiki;

    private XWikiContext xwikiContext;

    private VelocityEvaluator velocityEvaluator;

    @Before
    public void setUp() throws Exception
    {
        this.xwikiContext = mock(XWikiContext.class);
        Provider<XWikiContext> contextProvider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(contextProvider.get()).thenReturn(this.xwikiContext);

        this.xwiki = mock(XWiki.class);
        when(xwikiContext.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getDefaultLocale(this.xwikiContext)).thenReturn(Locale.ENGLISH);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(any(DocumentReference.class), eq(this.xwikiContext))).thenReturn(document);

        BaseObject object = mock(BaseObject.class);

        when(document.getXObjects(any())).thenReturn(Collections.singletonList(object));

        // Needed so that xcontext.setURLFactory(new ExternalServletURLFactory(xcontext)); will not fail even
        // though we don't want this line to have any behavior.
        when(this.xwikiContext.getURL()).thenReturn(new URL("http://localhost:8080/dummy"));
        when(this.xwikiContext.getRequest()).thenReturn(mock(XWikiRequest.class));

        velocityEvaluator = mocker.getInstance(VelocityEvaluator.class);
    }

    @Test
    public void evaluate() throws Exception
    {
        DocumentAccessBridge documentBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        when(documentBridge.getProperty(same(documentReference), any(), anyInt(), eq("html")))
            .thenReturn("Hello <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.mocker.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);
        when(velocityEvaluator.evaluateVelocity(eq("Hello <b>${name}</b> <br />${email}"), any(),
                any(VelocityContext.class))).thenReturn("Hello <b>John Doe</b> <br />john@doe.com");

        String result =
            this.mocker.getComponentUnderTest().evaluate(documentReference, "html", Collections.emptyMap());

        assertEquals(result, "Hello <b>John Doe</b> <br />john@doe.com");
    }

    @Test
    public void evaluateWithLanguage() throws Exception
    {
        DocumentAccessBridge documentBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("fr"))).thenReturn(1);
        when(documentBridge.getProperty(any(DocumentReference.class), any(), eq(1), eq("html")))
            .thenReturn("Salut <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.mocker.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);
        when(velocityEvaluator.evaluateVelocity(eq("Salut <b>${name}</b> <br />${email}"), any(),
                any(VelocityContext.class))).thenReturn("Salut <b>John Doe</b> <br />john@doe.com");


        // Set the default Locale to be different from the locale we pass to verify we restore it properly
        when(this.xwikiContext.getLocale()).thenReturn(Locale.ITALIAN);

        String result = this.mocker.getComponentUnderTest().evaluate(documentReference, "html", Collections.emptyMap(),
            Locale.FRENCH);

        verify(documentBridge).getObjectNumber(any(), any(), eq("language"), eq("fr"));

        // Make sure we set the right locale in the XWiki Context
        verify(this.xwikiContext).setLocale(Locale.FRENCH);
        verify(this.xwikiContext).setLocale(Locale.ITALIAN);

        assertEquals(result, "Salut <b>John Doe</b> <br />john@doe.com");
    }

    @Test
    public void evaluateWithObjectNotFoundWithLanguagePassed() throws Exception
    {
        DocumentAccessBridge documentBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        // First call with the passed language, return -1 (No XWiki.Mail xobject found)
        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("fr"))).thenReturn(-1);

        // Second call with the default language (en), return one (Only XWiki.Mail xobject is found)
        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("en"))).thenReturn(1);

        when(documentBridge.getProperty(any(DocumentReference.class), any(), eq(1), eq("html")))
            .thenReturn("Salut <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.mocker.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);
        when(velocityEvaluator.evaluateVelocity(eq("Salut <b>${name}</b> <br />${email}"), any(),
                any(VelocityContext.class))).thenReturn("Salut <b>John Doe</b> <br />john@doe.com");

        String result = this.mocker.getComponentUnderTest().evaluate(documentReference, "html", Collections.emptyMap(),
            Locale.FRENCH);

        verify(documentBridge).getObjectNumber(any(), any(), eq("language"), eq("fr"));
        verify(documentBridge).getObjectNumber(any(), any(), eq("language"), eq("en"));

        assertEquals(result, "Salut <b>John Doe</b> <br />john@doe.com");
    }

    @Test
    public void evaluateWithObjectNotFoundWithDefaultLanguage() throws Exception
    {
        DocumentAccessBridge documentBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        // First call with the passed language, return -1 (No XWiki.Mail xobject found)
        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("fr"))).thenReturn(-1);

        // Second call with the default language, return -1 (No XWiki.Mail xobject found)
        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("en"))).thenReturn(-1);

        when(documentBridge.getProperty(any(DocumentReference.class), any(), eq(0), eq("html")))
            .thenReturn("Salut <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.mocker.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);
        when(velocityEvaluator.evaluateVelocity(eq("Salut <b>${name}</b> <br />${email}"), any(),
                any(VelocityContext.class))).thenReturn("Salut <b>John Doe</b> <br />john@doe.com");

        String result = this.mocker.getComponentUnderTest().evaluate(documentReference, "html", Collections.emptyMap(),
            Locale.FRENCH);

        verify(documentBridge).getObjectNumber(any(), any(), eq("language"), eq("fr"));
        verify(documentBridge).getObjectNumber(any(), any(), eq("language"), eq("en"));

        assertEquals(result, "Salut <b>John Doe</b> <br />john@doe.com");
    }

    @Test
    public void evaluateWithErrorNoObjectMatches() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(any(DocumentReference.class), any())).thenReturn(document);

        BaseObject object1 = mock(BaseObject.class);
        BaseObject object2 = mock(BaseObject.class);

        List<BaseObject> xobjects = Arrays.asList(object1, object2);
        when(document.getXObjects(any())).thenReturn(xobjects);

        DocumentAccessBridge documentBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        // First call with the passed language, return -1 (No XWiki.Mail xobject found)
        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("fr"))).thenReturn(-1);

        // Second call with the default language, return -1 (No XWiki.Mail xobject found)
        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("en"))).thenReturn(-1);

        try {
            this.mocker.getComponentUnderTest().evaluate(documentReference, "html", Collections.emptyMap(),
                Locale.FRENCH);

            fail("Should have thrown an exception here!");
        } catch (MessagingException expected) {
            assertEquals(
                "No [Document XWiki.Mail] object matches the locale [fr] or the default locale [en] "
                +"in the Document [wiki:space.page]",
                expected.getMessage());
        }
    }

    @Test
    public void evaluateWhenVelocityError() throws Exception
    {
        DocumentAccessBridge documentBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        when(documentBridge.getProperty(same(documentReference), any(), anyInt(), eq("html")))
            .thenReturn("Hello <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.mocker.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        when(velocityEvaluator.evaluateVelocity(eq("Hello <b>${name}</b> <br />${email}"), any(),
                any(VelocityContext.class))).thenThrow(new XWikiException(0, 0, "Error"));

        try {
            this.mocker.getComponentUnderTest().evaluate(documentReference, "html",
                Collections.<String, Object>emptyMap());
            fail("Should have thrown an exception here!");
        } catch (MessagingException expected) {
            assertEquals("Failed to evaluate property [html] for Document [wiki:space.page] and locale [null]",
                expected.getMessage());
        }
    }
}
