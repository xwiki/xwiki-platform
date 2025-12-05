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
package org.xwiki.wysiwyg.filter;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wysiwyg.converter.RequestParameterConverter;

import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ConversionFilter}.
 */
@ComponentTest
class ConversionFilterTest
{
    private ConversionFilter conversionFilter = new ConversionFilter();

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private Execution execution;

    @MockComponent
    private RequestParameterConverter defaultParameterConverter;

    @Mock
    private ServletRequest request;

    @Mock
    private ServletResponse response;

    @Mock
    private FilterChain chain;

    @BeforeEach
    void configure()
    {
        Utils.setComponentManager(this.componentManager);
    }

    @Test
    void doFilter() throws Exception
    {
        ServletRequest reqFromFirstFilter = mock(ServletRequest.class);
        when(defaultParameterConverter.convert(this.request, this.response))
            .thenReturn(Optional.of(reqFromFirstFilter));
        this.conversionFilter.doFilter(this.request, this.response, this.chain);
        verify(this.chain).doFilter(reqFromFirstFilter, this.response);
    }

    @Test
    void doFilterWithoutChaining() throws Exception
    {
        when(defaultParameterConverter.convert(this.request, this.response)).thenReturn(Optional.empty());
        this.conversionFilter.doFilter(this.request, this.response, this.chain);
        verify(this.chain, never()).doFilter(any(ServletRequest.class), eq(this.response));
    }

    @Test
    void doFilterWithException() throws Exception
    {
        IOException exception = new IOException("failure");
        when(defaultParameterConverter.convert(this.request, this.response)).thenThrow(exception);

        try {
            this.conversionFilter.doFilter(this.request, this.response, this.chain);
            fail();
        } catch (IOException e) {
            assertSame(exception, e);
        }
        verify(this.chain, never()).doFilter(any(ServletRequest.class), eq(this.response));
        verify(this.execution).popContext();
    }
}
