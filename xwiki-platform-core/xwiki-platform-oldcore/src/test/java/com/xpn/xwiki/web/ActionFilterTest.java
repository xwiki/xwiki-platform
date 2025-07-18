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
package com.xpn.xwiki.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.mockito.MockitoComponentManager;

import static java.util.Collections.enumeration;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of {@link ActionFilter}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@ComponentTest
class ActionFilterTest
{
    private final ActionFilter filter = new ActionFilter();

    @BeforeComponent
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
        Utils.setComponentManager(componentManager);
        componentManager.registerMockComponent(ConfigurationSource.class, "xwikicfg");
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @Test
    void doFilterNotAnHttpServletRequest() throws Exception
    {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        this.filter.doFilter(request, response, chain);

        verify(request, never()).getParameterValues("xaction");
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterActionDispatcherIsTrue() throws Exception
    {
        ServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getAttribute(ActionFilter.class.getName() + ".actionDispatched")).thenReturn("true");

        this.filter.doFilter(request, response, chain);

        verify(request, never()).getParameterValues("xaction");
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterXactionNull() throws Exception
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameterValues("xaction")).thenReturn(null);
        when(request.getParameterNames()).thenReturn(enumeration(singletonList("a")));

        this.filter.doFilter(request, response, chain);

        verify(request).getParameterValues("xaction");
        verify(chain).doFilter(request, response);
        verify(request, never()).getRequestDispatcher(any());
    }

    @Test
    void doFilterXactionNullParamActionExist() throws Exception
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameterValues("xaction")).thenReturn(null);
        when(request.getParameterNames()).thenReturn(enumeration(singletonList("action_a")));
        when(request.getRequestURI()).thenReturn("/segment1/segment2/segment3/");
        when(request.getServletPath()).thenReturn("/serv");
        when(request.getContextPath()).thenReturn("/ctx/");

        this.filter.doFilter(request, response, chain);

        verify(request).getParameterValues("xaction");
        verify(chain).doFilter(request, response);
        verify(request).getRequestDispatcher(any());
    }

    @Test
    void doFilterXactionIsAction() throws Exception
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameterValues("xaction")).thenReturn(new String[] {
            "a"
        });

        when(request.getParameterNames()).thenReturn(enumeration(singletonList("action_a")));

        when(request.getRequestURI()).thenReturn("/segment1/segment2/segment3/");
        when(request.getServletPath()).thenReturn("/serv");
        when(request.getContextPath()).thenReturn("/ctx/");

        this.filter.doFilter(request, response, chain);

        verify(request).getParameterValues("xaction");
        verify(chain).doFilter(request, response);
        verify(request).getRequestDispatcher(any());
    }

    @Test
    void doFilterXactionIsNotAction() throws Exception
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameterValues("xaction")).thenReturn(new String[] {
            "b"
        });

        when(request.getParameterNames()).thenReturn(enumeration(singletonList("action_a")));

        when(request.getRequestURI()).thenReturn("/segment1/segment2/segment3/");
        when(request.getServletPath()).thenReturn("/serv");
        when(request.getContextPath()).thenReturn("/ctx/");

        this.filter.doFilter(request, response, chain);

        verify(request).getParameterValues("xaction");
        verify(chain).doFilter(request, response);
        verify(request, never()).getRequestDispatcher(any());
    }
}