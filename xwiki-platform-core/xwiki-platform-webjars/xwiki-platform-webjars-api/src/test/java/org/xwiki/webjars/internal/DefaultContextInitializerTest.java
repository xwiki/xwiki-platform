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
package org.xwiki.webjars.internal;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultContextInitializer}.
 *
 * @version $Id$
 * @since 7.1.4, 7.4M2
 */
public class DefaultContextInitializerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultContextInitializer> mocker =
        new MockitoComponentMockingRule<>(DefaultContextInitializer.class);

    @Test
    public void initalizeContext() throws Exception
    {
        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = new ExecutionContext();
        XWikiContext xc = mock(XWikiContext.class);
        ec.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xc);
        when(execution.getContext()).thenReturn(ec);

        this.mocker.getComponentUnderTest().initialize("mywiki");

        // This is the test
        verify(xc).setWikiId("mywiki");
    }

    @Test
    public void initalizeContextWhenNoWikiIdPassed() throws Exception
    {
        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = new ExecutionContext();
        XWikiContext xc = mock(XWikiContext.class);
        ec.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xc);
        when(execution.getContext()).thenReturn(ec);

        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");

        this.mocker.getComponentUnderTest().initialize(null);

        // This is the test
        verify(xc).setWikiId("xwiki");
    }
}
