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
package org.xwiki.icon.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.internal.context.IconSetContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link IconSetContext}.
 * 
 * @version $Id$
 */
@ComponentTest
public class IconSetContextTest
{
    @InjectMockComponents
    private IconSetContext isContext;

    @MockComponent
    private Execution execution;

    @Test
    public void getIconSet()
    {
        assertNull(this.isContext.getIconSet());

        ExecutionContext eContext = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(eContext);

        assertNull(this.isContext.getIconSet());

        IconSet iconSet = mock(IconSet.class);
        eContext.setProperty("icon.set", iconSet);

        assertSame(iconSet, this.isContext.getIconSet());
    }

    @Test
    public void setIconSet()
    {
        this.isContext.setIconSet(null);

        ExecutionContext eContext = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(eContext);

        this.isContext.setIconSet(null);

        IconSet iconSet = mock(IconSet.class);
        eContext.setProperty("icon.set", iconSet);

        assertNotNull(eContext.getProperty("icon.set"));

        this.isContext.setIconSet(null);

        assertNull(eContext.getProperty("icon.set"));

        this.isContext.setIconSet(iconSet);

        assertSame(iconSet, eContext.getProperty("icon.set"));
    }
}
