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
package org.xwiki.security.authentication.internal.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.servlet.ServletOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.security.authentication.AuthenticationAction;
import org.xwiki.security.authentication.AuthenticationResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiContextInitializer;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.web.XWikiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AuthenticationResourceReferenceHandler}.
 *
 * @version $Id$
 * @since 13.0RC1
 */
@ComponentTest
class AuthenticationResourceReferenceHandlerTest
{
    @InjectMockComponents
    private AuthenticationResourceReferenceHandler resourceReferenceHandler;

    @MockComponent
    private XWikiContextInitializer xWikiContextInitializer;

    @MockComponent
    private Execution execution;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    private XWikiResponse response;

    private XWiki xwiki;

    private XWikiContext context;

    private ServletOutputStream servletOutputStream;

    private WikiReference currentWiki;

    @BeforeEach
    void setup() throws XWikiException, IOException
    {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        this.context = mock(XWikiContext.class);
        when(this.xWikiContextInitializer.initialize(executionContext)).thenReturn(context);
        this.response = mock(XWikiResponse.class);
        when(context.getResponse()).thenReturn(response);
        this.xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);
        when(this.xwiki.getEncoding()).thenReturn("UTF-8");
        XWikiPluginManager pluginManager = mock(XWikiPluginManager.class);
        when(this.xwiki.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.endParsing(any(), eq(context)))
            .then(invocationOnMock -> invocationOnMock.getArgument(0));
        this.servletOutputStream = mock(ServletOutputStream.class);
        when(this.response.getOutputStream()).thenReturn(servletOutputStream);
        currentWiki = new WikiReference("currentWiki");
        when(context.getWikiReference()).thenReturn(currentWiki);
    }

    @Test
    void getSupportedResourceReferences()
    {
        assertEquals(Collections.singletonList(AuthenticationResourceReference.TYPE),
            this.resourceReferenceHandler.getSupportedResourceReferences());
    }

    @Test
    void handleResetPassword() throws Exception
    {
        WikiReference wikiReference = new WikiReference("foo");
        when(this.wikiDescriptorManager.exists("foo")).thenReturn(false);
        AuthenticationResourceReference resourceReference = new AuthenticationResourceReference(
            wikiReference,
            AuthenticationAction.RESET_PASSWORD);

        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        ResourceReferenceHandlerException exception =
            assertThrows(ResourceReferenceHandlerException.class,
                () -> this.resourceReferenceHandler.handle(resourceReference, chain));
        assertEquals("The wiki [foo] does not exist.", exception.getMessage());

        when(this.wikiDescriptorManager.exists("foo")).thenReturn(true);
        when(this.xwiki.evaluateTemplate("resetpassword.vm", context)).thenReturn("Reset password content");
        this.resourceReferenceHandler.handle(resourceReference, chain);

        verify(response).setContentType("text/html; charset=UTF-8");
        verify(this.xWikiContextInitializer).initialize(any(ExecutionContext.class));
        verify(servletOutputStream).write("Reset password content".getBytes(StandardCharsets.UTF_8));
        verify(chain).handleNext(resourceReference);
        verify(context).setWikiReference(wikiReference);
        verify(context).setWikiReference(currentWiki);
    }

    @Test
    void handleForgotUsername() throws Exception
    {
        WikiReference wikiReference = new WikiReference("bar");
        when(this.wikiDescriptorManager.exists("bar")).thenReturn(false);
        AuthenticationResourceReference resourceReference = new AuthenticationResourceReference(
            wikiReference,
            AuthenticationAction.RETRIEVE_USERNAME);

        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        ResourceReferenceHandlerException exception =
            assertThrows(ResourceReferenceHandlerException.class,
                () -> this.resourceReferenceHandler.handle(resourceReference, chain));
        assertEquals("The wiki [bar] does not exist.", exception.getMessage());

        when(this.wikiDescriptorManager.exists("bar")).thenReturn(true);
        when(this.xwiki.evaluateTemplate("forgotusername.vm", context)).thenReturn("Forgot user name content");

        this.resourceReferenceHandler.handle(resourceReference, chain);
        verify(response).setContentType("text/html; charset=UTF-8");
        verify(this.xWikiContextInitializer).initialize(any(ExecutionContext.class));
        verify(servletOutputStream).write("Forgot user name content".getBytes(StandardCharsets.UTF_8));
        verify(chain).handleNext(resourceReference);
        verify(context).setWikiReference(wikiReference);
        verify(context).setWikiReference(currentWiki);
    }

    @Test
    void handleForgotUsernameWikiDescriptorError() throws Exception
    {
        WikiReference wikiReference = new WikiReference("bar");
        when(this.wikiDescriptorManager.exists("bar")).thenThrow(new WikiManagerException("Cannot access wiki"));
        AuthenticationResourceReference resourceReference = new AuthenticationResourceReference(
            wikiReference,
            AuthenticationAction.RETRIEVE_USERNAME);

        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        ResourceReferenceHandlerException exception =
            assertThrows(ResourceReferenceHandlerException.class,
                () -> this.resourceReferenceHandler.handle(resourceReference, chain));
        assertEquals("Error when checking if wiki [bar] exists.", exception.getMessage());
    }
}
