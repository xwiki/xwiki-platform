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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

/**
 * Unit tests for {@link SheetContextStore}.
 * 
 * @version $Id$
 */
@ComponentTest
class SheetContextStoreTest
{
    @InjectMockComponents
    private SheetContextStore sheetContextStore;

    @MockComponent
    private Execution execution;

    private ExecutionContext context = new ExecutionContext();

    private Map<String, Serializable> contextStore = new HashMap<>();

    @Test
    void saveAndRestoreWithoutContext()
    {
        this.sheetContextStore.save(this.contextStore, Collections.singletonList("sheet"));
        assertTrue(this.contextStore.isEmpty());

        this.contextStore.put("shet", "Some.Sheet");
        this.sheetContextStore.restore(this.contextStore);
    }

    @Test
    void saveAndRestore()
    {
        when(this.execution.getContext()).thenReturn(this.context);

        // Save

        this.context.setProperty("sheet", "Some.Sheet");
        this.sheetContextStore.save(this.contextStore, Collections.singletonList("test"));
        assertTrue(this.contextStore.isEmpty());

        this.context.removeProperty("sheet");
        this.sheetContextStore.save(this.contextStore, Collections.singletonList("sheet"));
        assertTrue(this.contextStore.isEmpty());

        this.context.setProperty("sheet", "Some.Sheet");
        this.sheetContextStore.save(this.contextStore, Collections.singletonList("sheet"));
        assertEquals(1, this.contextStore.size());
        assertEquals("Some.Sheet", this.contextStore.get("sheet"));

        // Restore

        this.context.removeProperty("sheet");
        this.sheetContextStore.restore(Collections.singletonMap("test", "foo"));
        assertFalse(this.context.hasProperty("sheet"));

        this.sheetContextStore.restore(this.contextStore);
        assertEquals("Some.Sheet", this.context.getProperty("sheet"));
    }
}
