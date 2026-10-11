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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.LESSContext}.
 *
 * @version $Id$
 * @since 6.4
 */
@ComponentTest
class LESSContextTest
{
    @InjectMockComponents
    private LESSContext lessContext;

    @MockComponent
    private Execution execution;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp()
    {
        this.executionContext = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(this.executionContext);
    }

    @Test
    void disableCache()
    {
        this.lessContext.disableCache();
        assertTrue((Boolean) this.executionContext.getProperty("less.cache.disable"));
    }

    @Test
    void stopDisablingCache()
    {
        this.lessContext.disableCache();
        assertTrue((Boolean) this.executionContext.getProperty("less.cache.disable"));
        this.lessContext.stopDisablingCache();
        assertFalse((Boolean) this.executionContext.getProperty("less.cache.disable"));
    }

    @Test
    void isCacheDisabled()
    {
        assertFalse(this.lessContext.isCacheDisabled());
        this.lessContext.disableCache();
        assertTrue(this.lessContext.isCacheDisabled());
        this.lessContext.stopDisablingCache();
        assertFalse(this.lessContext.isCacheDisabled());
    }

    @Test
    void setHTMLExport()
    {
        assertFalse(this.lessContext.isHtmlExport());
        this.lessContext.setHtmlExport(true);
        assertTrue(this.lessContext.isHtmlExport());
        this.lessContext.setHtmlExport(false);
        assertFalse(this.lessContext.isHtmlExport());
    }
}
