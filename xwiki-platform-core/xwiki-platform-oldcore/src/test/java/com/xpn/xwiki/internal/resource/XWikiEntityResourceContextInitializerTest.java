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
package com.xpn.xwiki.internal.resource;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiContextInitializer;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiEntityResourceContextInitializer}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiEntityResourceContextInitializerTest
{
    private static final EntityResourceAction ACTION = new EntityResourceAction("editcr");

    private static final EntityResourceReference SUBWIKI_REFERENCE =
        new EntityResourceReference(new DocumentReference("subwiki", "Space", "Page"), ACTION);

    @InjectMockComponents
    private XWikiEntityResourceContextInitializer initializer;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private Container container;

    @MockComponent
    private Execution execution;

    @MockComponent
    private XWikiContextInitializer xcontextInitializer;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private RenderingContext renderingContext;

    @MockComponent
    private ResourceTypeResolver<ExtendedURL> resourceTypeResolver;

    @MockComponent
    private ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ServletContext servletContext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiContext authenticatedContext;

    @Mock
    private XWikiRequest xwikiRequest;

    @MockComponent
    private VelocityManager velocityManager;

    @Mock
    private VelocityContext velocityContext;

    private final ExecutionContext econtext = new ExecutionContext();

    private final StringWriter responseContent = new StringWriter();

    @BeforeEach
    void setUp() throws Exception
    {
        Utils.setComponentManager(this.componentManager);

        when(this.container.getRequest()).thenReturn(new ServletRequest(this.request));
        when(this.container.getResponse()).thenReturn(new ServletResponse(this.response));
        when(this.execution.getContext()).thenReturn(this.econtext);

        when(this.request.getRequestURL())
            .thenReturn(new StringBuffer("http://localhost:8080/xwiki/bin/editcr/Space/Page"));
        when(this.request.getRequestURI()).thenReturn("/xwiki/bin/editcr/Space/Page");
        when(this.request.getContextPath()).thenReturn("/xwiki");
        when(this.request.getServletContext()).thenReturn(this.servletContext);
        when(this.response.getWriter()).thenReturn(new PrintWriter(this.responseContent));
        // XWiki itself is initialized.
        when(this.servletContext.getAttribute(XWiki.DEFAULT_MAIN_WIKI)).thenReturn(this.xwiki);

        when(this.resourceReferenceResolver.resolve(any(), any(), anyMap())).thenReturn(SUBWIKI_REFERENCE);
        when(this.wikiDescriptorManager.getById("subwiki")).thenReturn(new WikiDescriptor("subwiki", "subwiki"));

        when(this.xcontextInitializer.authenticate()).thenReturn(this.xcontextInitializer);
        when(this.xcontextInitializer.initialize(this.econtext)).thenReturn(this.authenticatedContext);
        when(this.authenticatedContext.getWiki()).thenReturn(this.xwiki);
        when(this.authenticatedContext.getRequest()).thenReturn(this.xwikiRequest);
        when(this.velocityManager.getVelocityContext()).thenReturn(this.velocityContext);
        when(this.xwiki.prepareDocuments(this.xwikiRequest, this.authenticatedContext, this.velocityContext))
            .thenReturn(true);
    }

    @AfterEach
    void tearDown()
    {
        Utils.setComponentManager(null);
    }

    @Test
    void initializeWhenWikiIsInitialized() throws Exception
    {
        when(this.xwiki.initializeWiki(eq("subwiki"), eq(false), any())).thenReturn(true);

        assertTrue(this.initializer.initialize(SUBWIKI_REFERENCE));

        // The documents are prepared like for the legacy UnknownAction, before setting the action of the URL.
        InOrder inOrder = inOrder(this.authenticatedContext, this.xwiki);
        inOrder.verify(this.authenticatedContext).setAction("unknown");
        inOrder.verify(this.xwiki).prepareDocuments(this.xwikiRequest, this.authenticatedContext,
            this.velocityContext);
        inOrder.verify(this.authenticatedContext).setAction("editcr");
        verify(this.authenticatedContext).put("ajax", false);
        assertTrue(this.econtext.hasProperty(ResourceReferenceHandlerAuthenticatorListener.AUTHENTICATED_PROPERTY));
        verifyNoInteractions(this.templateManager);
    }

    @Test
    void initializeWithAjaxRequest() throws Exception
    {
        when(this.xwiki.initializeWiki(eq("subwiki"), eq(false), any())).thenReturn(true);
        when(this.xwikiRequest.get("ajax")).thenReturn("1");

        assertTrue(this.initializer.initialize(SUBWIKI_REFERENCE));

        verify(this.authenticatedContext).put("ajax", true);
    }

    @Test
    void initializeWhenAccessIsDenied() throws Exception
    {
        when(this.xwiki.initializeWiki(eq("subwiki"), eq(false), any())).thenReturn(true);
        when(this.xwiki.prepareDocuments(this.xwikiRequest, this.authenticatedContext, this.velocityContext))
            .thenThrow(new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "denied"));

        assertFalse(this.initializer.initialize(SUBWIKI_REFERENCE));

        verify(this.response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verify(this.authenticatedContext, never()).setAction("editcr");
    }

    @Test
    void initializeWhenGuestIsRedirectedToLogin() throws Exception
    {
        when(this.xwiki.initializeWiki(eq("subwiki"), eq(false), any())).thenReturn(true);
        when(this.xwiki.prepareDocuments(this.xwikiRequest, this.authenticatedContext, this.velocityContext))
            .thenThrow(new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "denied"));
        // The right service has already redirected the guest to the login form.
        when(this.response.isCommitted()).thenReturn(true);

        assertFalse(this.initializer.initialize(SUBWIKI_REFERENCE));

        verify(this.response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void initializeWhenWikiIsInitializing() throws Exception
    {
        // The wiki initialization is started without waiting for it to finish.
        when(this.xwiki.initializeWiki(eq("subwiki"), eq(false), any())).thenReturn(false);

        assertFalse(this.initializer.initialize(SUBWIKI_REFERENCE));

        verify(this.response).setStatus(HttpServletResponse.SC_ACCEPTED);
        verify(this.templateManager).render(eq("init.vm"), any());
        verifyNoInteractions(this.xcontextInitializer);
        assertFalse(this.econtext.hasProperty(ResourceReferenceHandlerAuthenticatorListener.AUTHENTICATED_PROPERTY));
    }

    @Test
    void initializeWhenWaitingForInitializationIsRequested() throws Exception
    {
        when(this.request.getParameter("async")).thenReturn("false");
        when(this.xwiki.initializeWiki(eq("subwiki"), eq(true), any())).thenReturn(true);

        assertTrue(this.initializer.initialize(SUBWIKI_REFERENCE));

        verify(this.xwiki, never()).initializeWiki(eq("subwiki"), eq(false), any());
        verifyNoInteractions(this.templateManager);
    }

    @Test
    void initializeWhenWikiDoesNotExist() throws Exception
    {
        when(this.wikiDescriptorManager.getById("subwiki")).thenReturn(null);

        assertFalse(this.initializer.initialize(SUBWIKI_REFERENCE));

        verify(this.response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
        verifyNoInteractions(this.xcontextInitializer, this.templateManager);
    }

    @Test
    void initializeWhenAuthenticationFails() throws Exception
    {
        when(this.xwiki.initializeWiki(eq("subwiki"), eq(false), any())).thenReturn(true);
        when(this.xcontextInitializer.initialize(this.econtext)).thenThrow(new RuntimeException("error"));

        assertThrows(ResourceReferenceHandlerException.class, () -> this.initializer.initialize(SUBWIKI_REFERENCE));
    }

    @Test
    void initializeWithUnsupportedRequest()
    {
        when(this.container.getRequest()).thenReturn(null);

        assertThrows(ResourceReferenceHandlerException.class, () -> this.initializer.initialize(SUBWIKI_REFERENCE));
    }
}
