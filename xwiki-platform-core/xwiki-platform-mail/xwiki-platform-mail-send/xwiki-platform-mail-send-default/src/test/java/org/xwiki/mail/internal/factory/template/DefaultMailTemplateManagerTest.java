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

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Provider;
import javax.mail.MessagingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.velocity.VelocityEvaluator;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.template.DefaultMailTemplateManager}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@ComponentTest
public class DefaultMailTemplateManagerTest
{
    @InjectMockComponents
    DefaultMailTemplateManager templateManager;

    @InjectComponentManager
    MockitoComponentManager componentManager;

    @MockComponent
    Provider<XWikiContext> contextProvider;

    @Mock
    XWikiContext xwikiContext;

    @Mock
    XWiki xwiki;

    @MockComponent
    VelocityEvaluator velocityEvaluator;

    @BeforeEach
    public void setUp() throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.xwikiContext);

        when(this.xwikiContext.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getDefaultLocale(this.xwikiContext)).thenReturn(Locale.ENGLISH);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(any(DocumentReference.class), eq(this.xwikiContext))).thenReturn(document);

        BaseObject object = mock(BaseObject.class);

        when(document.getXObjects(any())).thenReturn(Collections.singletonList(object));

        // Needed so that xcontext.setURLFactory(new ExternalServletURLFactory(xcontext)); will not fail even
        // though we don't want this line to have any behavior.
        when(this.xwikiContext.getURL()).thenReturn(new URL("http://localhost:8080/dummy"));
        when(this.xwikiContext.getRequest()).thenReturn(mock(XWikiRequest.class));
    }

    @Test
    public void evaluate() throws Exception
    {
        DocumentAccessBridge documentBridge = this.componentManager.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        when(documentBridge.getProperty(same(documentReference), any(), anyInt(), eq("html")))
            .thenReturn("Hello <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);
        when(this.velocityEvaluator.evaluateVelocity(eq("Hello <b>${name}</b> <br />${email}"), any(),
            any())).thenReturn("Hello <b>John Doe</b> <br />john@doe.com");

        String result = this.templateManager.evaluate(documentReference, "html", Collections.emptyMap());

        assertEquals("Hello <b>John Doe</b> <br />john@doe.com", result);
    }

    @Test
    public void evaluateWithLanguage() throws Exception
    {
        DocumentAccessBridge documentBridge = this.componentManager.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("fr"))).thenReturn(1);
        when(documentBridge.getProperty(any(DocumentReference.class), any(), eq(1), eq("html")))
            .thenReturn("Salut <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);
        when(this.velocityEvaluator.evaluateVelocity(eq("Salut <b>${name}</b> <br />${email}"), any(),
            any())).thenReturn("Salut <b>John Doe</b> <br />john@doe.com");

        // Set the default Locale to be different from the locale we pass to verify we restore it properly
        when(this.xwikiContext.getLocale()).thenReturn(Locale.ITALIAN);

        String result = this.templateManager.evaluate(documentReference, "html", Collections.emptyMap(),
            Locale.FRENCH);

        verify(documentBridge).getObjectNumber(any(), any(), eq("language"), eq("fr"));

        // Make sure we set the right locale in the XWiki Context
        verify(this.xwikiContext).setLocale(Locale.FRENCH);
        verify(this.xwikiContext).setLocale(Locale.ITALIAN);

        assertEquals("Salut <b>John Doe</b> <br />john@doe.com", result);
    }

    @Test
    public void evaluateWithObjectNotFoundWithLanguagePassed() throws Exception
    {
        DocumentAccessBridge documentBridge = this.componentManager.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        // First call with the passed language, return -1 (No XWiki.Mail xobject found)
        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("fr"))).thenReturn(-1);

        // Second call with the default language (en), return one (Only XWiki.Mail xobject is found)
        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("en"))).thenReturn(1);

        when(documentBridge.getProperty(any(DocumentReference.class), any(), eq(1), eq("html")))
            .thenReturn("Salut <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);
        when(this.velocityEvaluator.evaluateVelocity(eq("Salut <b>${name}</b> <br />${email}"), any(),
            any())).thenReturn("Salut <b>John Doe</b> <br />john@doe.com");

        String result = this.templateManager.evaluate(documentReference, "html", Collections.emptyMap(),
            Locale.FRENCH);

        verify(documentBridge).getObjectNumber(any(), any(), eq("language"), eq("fr"));
        verify(documentBridge).getObjectNumber(any(), any(), eq("language"), eq("en"));

        assertEquals("Salut <b>John Doe</b> <br />john@doe.com", result);
    }

    @Test
    public void evaluateWithObjectNotFoundWithDefaultLanguage() throws Exception
    {
        DocumentAccessBridge documentBridge = this.componentManager.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        // First call with the passed language, return -1 (No XWiki.Mail xobject found)
        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("fr"))).thenReturn(-1);

        // Second call with the default language, return -1 (No XWiki.Mail xobject found)
        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("en"))).thenReturn(-1);

        when(documentBridge.getProperty(any(DocumentReference.class), any(), eq(0), eq("html")))
            .thenReturn("Salut <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);
        when(this.velocityEvaluator.evaluateVelocity(eq("Salut <b>${name}</b> <br />${email}"), any(),
            any())).thenReturn("Salut <b>John Doe</b> <br />john@doe.com");

        String result = this.templateManager.evaluate(documentReference, "html", Collections.emptyMap(),
            Locale.FRENCH);

        verify(documentBridge).getObjectNumber(any(), any(), eq("language"), eq("fr"));
        verify(documentBridge).getObjectNumber(any(), any(), eq("language"), eq("en"));

        assertEquals("Salut <b>John Doe</b> <br />john@doe.com", result);
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

        DocumentAccessBridge documentBridge = this.componentManager.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        // First call with the passed language, return -1 (No XWiki.Mail xobject found)
        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("fr"))).thenReturn(-1);

        // Second call with the default language, return -1 (No XWiki.Mail xobject found)
        when(documentBridge.getObjectNumber(any(), any(), eq("language"), eq("en"))).thenReturn(-1);

        Throwable exception = assertThrows(MessagingException.class,
            () -> this.templateManager.evaluate(documentReference, "html", Collections.emptyMap(), Locale.FRENCH));
        assertEquals("No [Document XWiki.Mail] object matches the locale [fr] or the default locale [en] "
            + "in the Document [wiki:space.page]", exception.getMessage());
    }

    @Test
    public void evaluateWhenVelocityError() throws Exception
    {
        DocumentAccessBridge documentBridge = this.componentManager.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        when(documentBridge.getProperty(same(documentReference), any(), anyInt(), eq("html")))
            .thenReturn("Hello <b>${name}</b> <br />${email}");

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        when(this.velocityEvaluator.evaluateVelocity(eq("Hello <b>${name}</b> <br />${email}"), any(),
            any())).thenThrow(new XWikiException(0, 0, "Error"));

        Throwable exception = assertThrows(MessagingException.class,
            () -> this.templateManager.evaluate(documentReference, "html", Collections.emptyMap()));
        assertEquals("Failed to evaluate property [html] for Document [wiki:space.page] and locale [null]",
            exception.getMessage());
    }
}
