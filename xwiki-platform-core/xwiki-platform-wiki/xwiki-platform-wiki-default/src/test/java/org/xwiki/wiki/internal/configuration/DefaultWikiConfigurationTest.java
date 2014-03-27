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
package org.xwiki.wiki.internal.configuration;

import java.net.MalformedURLException;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.configuration.DefaultWikiConfiguration}.
 *
 * @version $Id$
 * @since 5.4.4
 */
public class DefaultWikiConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiConfiguration> mocker =
            new MockitoComponentMockingRule(DefaultWikiConfiguration.class);

    private ConfigurationSource configuration;

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext context;

    @Before
    public void setUp() throws Exception
    {
        configuration = mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        context = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(context);
    }

    @Test
    public void getAliasSuffix() throws Exception
    {
        when(configuration.getProperty("wiki.alias.suffix")).thenReturn("xwiki.org");
        assertEquals("xwiki.org", mocker.getComponentUnderTest().getAliasSuffix());
    }

    @Test
    public void getAliasSuffixWhenNoConfig() throws Exception
    {
        XWikiRequest request = mock(XWikiRequest.class);
        when(context.getRequest()).thenReturn(request);
        StringBuffer requestURL = new StringBuffer("http://www.xwiki.org/xwiki/bin/view/Main/WebHome");
        when(request.getRequestURL()).thenReturn(requestURL);

        assertEquals("xwiki.org", mocker.getComponentUnderTest().getAliasSuffix());

        StringBuffer requestURL2 = new StringBuffer("http://www.xwiki.org:8080/xwiki/bin/view/Main/WebHome");
        when(request.getRequestURL()).thenReturn(requestURL2);

        assertEquals("xwiki.org", mocker.getComponentUnderTest().getAliasSuffix());
    }

    @Test
    public void getAliasSuffixWhenIP() throws Exception
    {
        XWikiRequest request = mock(XWikiRequest.class);
        when(context.getRequest()).thenReturn(request);
        StringBuffer requestURL = new StringBuffer("http://127.0.0.1/xwiki/bin/view/Main/WebHome");
        when(request.getRequestURL()).thenReturn(requestURL);

        assertEquals("", mocker.getComponentUnderTest().getAliasSuffix());
    }

    @Test
    public void getAliasSuffixWhenLocalhost() throws Exception
    {
        XWikiRequest request = mock(XWikiRequest.class);
        when(context.getRequest()).thenReturn(request);
        StringBuffer requestURL = new StringBuffer("http://localhost/xwiki/bin/view/Main/WebHome");
        when(request.getRequestURL()).thenReturn(requestURL);

        assertEquals("", mocker.getComponentUnderTest().getAliasSuffix());
    }

    @Test
    public void getAliasSuffixWhenURLIsMalformed() throws Exception
    {
        XWikiRequest request = mock(XWikiRequest.class);
        when(context.getRequest()).thenReturn(request);
        StringBuffer requestURL = new StringBuffer("xwiki");
        when(request.getRequestURL()).thenReturn(requestURL);

        assertEquals("", mocker.getComponentUnderTest().getAliasSuffix());

        verify(mocker.getMockedLogger()).error(eq("Failed to get the host name of the request."),
                any(MalformedURLException.class));
    }

}
