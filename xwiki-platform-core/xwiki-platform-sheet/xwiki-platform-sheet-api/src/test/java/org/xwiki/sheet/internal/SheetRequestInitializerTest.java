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
package org.xwiki.sheet.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.container.Request;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SheetRequestInitializer}.
 *
 * @version $Id$
 */
@ComponentTest
class SheetRequestInitializerTest
{
    @InjectMockComponents
    private SheetRequestInitializer initializer;

    @MockComponent
    private Execution execution;

    private final ExecutionContext context = new ExecutionContext();

    private final Request request = mock(Request.class);

    @BeforeEach
    void configure()
    {
        when(this.execution.getContext()).thenReturn(this.context);
    }

    @Test
    void initializeWithSheetParameter() throws Exception
    {
        when(this.request.getProperty(SheetRequestInitializer.SHEET_PROPERTY_NAME)).thenReturn("Code.Sheet");

        this.initializer.initialize(this.request);

        assertEquals("Code.Sheet", this.context.getProperty(SheetRequestInitializer.SHEET_PROPERTY_NAME));
    }

    /**
     * An empty sheet parameter means the document must be displayed without a sheet, so it must be put on the
     * execution context as well.
     */
    @Test
    void initializeWithEmptySheetParameter() throws Exception
    {
        when(this.request.getProperty(SheetRequestInitializer.SHEET_PROPERTY_NAME)).thenReturn("");

        this.initializer.initialize(this.request);

        assertEquals("", this.context.getProperty(SheetRequestInitializer.SHEET_PROPERTY_NAME));
    }

    @Test
    void initializeWithoutSheetParameter() throws Exception
    {
        when(this.request.getProperty(SheetRequestInitializer.SHEET_PROPERTY_NAME)).thenReturn(null);

        this.initializer.initialize(this.request);

        assertFalse(this.context.hasProperty(SheetRequestInitializer.SHEET_PROPERTY_NAME));
    }
}
