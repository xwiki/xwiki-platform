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
package org.xwiki.resource.servlet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.CreateResourceTypeException;
import org.xwiki.resource.ResourceReferenceHandlerManager;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoutingFilter}.
 *
 * @version $Id$
 */
@ComponentTest
class RoutingFilterTest
{
    private static final ResourceType HANDLED_TYPE = new ResourceType("handled");

    private static final EntityResourceAction HANDLED_ACTION = new EntityResourceAction("handledaction");

    private static final EntityResourceAction LEGACY_ACTION = EntityResourceAction.VIEW;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private ResourceTypeResolver<ExtendedURL> resourceTypeResolver;

    @MockComponent
    private ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver;

    @MockComponent
    private ResourceReferenceHandlerManager<ResourceType> resourceTypeHandlerManager;

    @MockComponent
    private ResourceReferenceHandlerManager<EntityResourceAction> entityActionHandlerManager;

    @Mock
    private FilterConfig filterConfig;

    @Mock
    private ServletContext servletContext;

    @Mock
    private RequestDispatcher resourceReferenceHandlerDispatcher;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServletResponse response;

    @Mock
    private FilterChain chain;

    private RoutingFilter filter;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.filterConfig.getServletContext()).thenReturn(this.servletContext);
        when(this.servletContext.getAttribute(ComponentManager.class.getName())).thenReturn(this.componentManager);
        when(this.servletContext.getNamedDispatcher("resourceReferenceHandler"))
            .thenReturn(this.resourceReferenceHandlerDispatcher);

        when(this.request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/xwiki/bin/view/A/B"));
        when(this.request.getContextPath()).thenReturn("/xwiki");

        when(this.resourceTypeHandlerManager.canHandle(HANDLED_TYPE)).thenReturn(true);
        when(this.resourceTypeHandlerManager.canHandle(EntityResourceReference.TYPE)).thenReturn(true);
        when(this.entityActionHandlerManager.canHandle(HANDLED_ACTION)).thenReturn(true);

        this.filter = new RoutingFilter();
        this.filter.init(this.filterConfig);
    }

    @Test
    void doFilterWhenResourceTypeIsHandled() throws Exception
    {
        when(this.resourceTypeResolver.resolve(any(), anyMap())).thenReturn(HANDLED_TYPE);

        this.filter.doFilter(this.request, this.response, this.chain);

        verify(this.request).setAttribute(RoutingFilter.RESOURCE_TYPE_NAME, HANDLED_TYPE);
        verify(this.resourceReferenceHandlerDispatcher).forward(this.request, this.response);
        verifyNoInteractions(this.chain, this.resourceReferenceResolver);
    }

    @Test
    void doFilterWhenResourceTypeIsNotHandled() throws Exception
    {
        when(this.resourceTypeResolver.resolve(any(), anyMap())).thenReturn(new ResourceType("rest"));

        this.filter.doFilter(this.request, this.response, this.chain);

        verify(this.chain).doFilter(this.request, this.response);
        verifyNoInteractions(this.resourceReferenceHandlerDispatcher);
    }

    @Test
    void doFilterWhenResourceTypeIsStatic() throws Exception
    {
        when(this.resourceTypeResolver.resolve(any(), anyMap())).thenReturn(new ResourceType("skins", true));

        this.filter.doFilter(this.request, this.response, this.chain);

        verify(this.chain).doFilter(this.request, this.response);
        verifyNoInteractions(this.resourceReferenceHandlerDispatcher, this.resourceTypeHandlerManager);
    }

    @Test
    void doFilterWhenResourceTypeCannotBeResolved() throws Exception
    {
        when(this.resourceTypeResolver.resolve(any(), anyMap())).thenThrow(new CreateResourceTypeException("error"));

        this.filter.doFilter(this.request, this.response, this.chain);

        verify(this.chain).doFilter(this.request, this.response);
        verifyNoInteractions(this.resourceReferenceHandlerDispatcher);
    }

    @Test
    void doFilterWhenEntityResourceActionIsHandled() throws Exception
    {
        when(this.resourceTypeResolver.resolve(any(), anyMap())).thenReturn(EntityResourceReference.TYPE);
        when(this.resourceReferenceResolver.resolve(any(), eq(EntityResourceReference.TYPE), anyMap()))
            .thenReturn(new EntityResourceReference(new DocumentReference("wiki", "A", "B"), HANDLED_ACTION));

        this.filter.doFilter(this.request, this.response, this.chain);

        verify(this.resourceReferenceHandlerDispatcher).forward(this.request, this.response);
        verifyNoInteractions(this.chain);
    }

    @Test
    void doFilterWhenEntityResourceActionIsNotHandled() throws Exception
    {
        when(this.resourceTypeResolver.resolve(any(), anyMap())).thenReturn(EntityResourceReference.TYPE);
        when(this.resourceReferenceResolver.resolve(any(), eq(EntityResourceReference.TYPE), anyMap()))
            .thenReturn(new EntityResourceReference(new DocumentReference("wiki", "A", "B"), LEGACY_ACTION));

        this.filter.doFilter(this.request, this.response, this.chain);

        // Legacy actions, as well as the Servlets reached through short URLs, must not go through the Resource
        // Reference Handler Servlet, which would initialize and authenticate the request for nothing.
        verify(this.chain).doFilter(this.request, this.response);
        verify(this.request, never()).setAttribute(RoutingFilter.RESOURCE_TYPE_NAME, EntityResourceReference.TYPE);
        verifyNoInteractions(this.resourceReferenceHandlerDispatcher);
    }

    @Test
    void doFilterWhenEntityResourceReferenceCannotBeResolved() throws Exception
    {
        when(this.resourceTypeResolver.resolve(any(), anyMap())).thenReturn(EntityResourceReference.TYPE);
        when(this.resourceReferenceResolver.resolve(any(), eq(EntityResourceReference.TYPE), anyMap()))
            .thenThrow(new CreateResourceReferenceException("error"));

        this.filter.doFilter(this.request, this.response, this.chain);

        verify(this.chain).doFilter(this.request, this.response);
        verifyNoInteractions(this.resourceReferenceHandlerDispatcher, this.entityActionHandlerManager);
    }
}
