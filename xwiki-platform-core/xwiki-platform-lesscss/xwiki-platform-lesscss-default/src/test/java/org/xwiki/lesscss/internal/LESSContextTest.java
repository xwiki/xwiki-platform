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
package org.xwiki.lesscss.internal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.LESSContext}.
 *
 * @version $Id$
 * @since 6.4
 */
public class LESSContextTest
{
    @Rule
    public MockitoComponentMockingRule<LESSContext> mocker =
            new MockitoComponentMockingRule<>(LESSContext.class);

    private Execution execution;

    private ExecutionContext executionContext;

    @Before
    public void setUp() throws Exception
    {
        execution = mocker.getInstance(Execution.class);
        executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);
    }

    @Test
    public void disableCache() throws Exception
    {
        mocker.getComponentUnderTest().disableCache();
        assertTrue((Boolean)executionContext.getProperty("less.cache.disable"));
    }

    @Test
    public void stopDisablingCache() throws Exception
    {
        mocker.getComponentUnderTest().disableCache();
        assertTrue((Boolean) executionContext.getProperty("less.cache.disable"));
        mocker.getComponentUnderTest().stopDisablingCache();
        assertFalse((Boolean)executionContext.getProperty("less.cache.disable"));
    }

    @Test
    public void isCacheDisabled() throws Exception
    {
        assertFalse(mocker.getComponentUnderTest().isCacheDisabled());
        mocker.getComponentUnderTest().disableCache();
        assertTrue(mocker.getComponentUnderTest().isCacheDisabled());
        mocker.getComponentUnderTest().stopDisablingCache();
        assertFalse(mocker.getComponentUnderTest().isCacheDisabled());
    }

    @Test
    public void setHTMLExport() throws Exception
    {
        assertFalse(mocker.getComponentUnderTest().isHtmlExport());
        mocker.getComponentUnderTest().setHtmlExport(true);
        assertTrue(mocker.getComponentUnderTest().isHtmlExport());
        mocker.getComponentUnderTest().setHtmlExport(false);
        assertFalse(mocker.getComponentUnderTest().isHtmlExport());
    }

}
