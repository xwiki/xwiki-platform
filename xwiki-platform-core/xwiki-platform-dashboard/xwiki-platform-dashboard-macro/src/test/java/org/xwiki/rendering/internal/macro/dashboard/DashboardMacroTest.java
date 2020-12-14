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
package org.xwiki.rendering.internal.macro.dashboard;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.properties.BeanDescriptor;
import org.xwiki.properties.BeanManager;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.dashboard.DashboardMacroParameters;
import org.xwiki.rendering.macro.dashboard.DashboardRenderer;
import org.xwiki.rendering.macro.dashboard.GadgetRenderer;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DashboardMacro}.
 *
 * @version $Id$
 */
@ComponentTest
class DashboardMacroTest
{
    @InjectMockComponents
    private DashboardMacro macro;

    @MockComponent
    private BeanManager beanManager;

    @MockComponent
    private Execution execution;

    @Test
    void executeWhenInsideDashboardMacro()
    {
        BeanDescriptor descriptor = mock(BeanDescriptor.class);
        when(this.beanManager.getBeanDescriptor(any())).thenReturn(descriptor);
        when(descriptor.getProperties()).thenReturn(Collections.emptyList());

        ExecutionContext ec = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(ec);
        ec.setProperty("dashboardMacroCalls", 1);

        DashboardMacroParameters parameters = new DashboardMacroParameters();
        MacroTransformationContext macroContext = new MacroTransformationContext();

        Throwable exception = assertThrows(MacroExecutionException.class,
            () -> this.macro.execute(parameters, "", macroContext));
        assertEquals("Dashboard macro recursion detected. Don't call the Dashboard macro inside of itself...",
            exception.getMessage());
    }

    @Test
    void executeWhenNotInsideDashboardMacro(MockitoComponentManager componentManager) throws Exception
    {
        BeanDescriptor descriptor = mock(BeanDescriptor.class);
        when(this.beanManager.getBeanDescriptor(any())).thenReturn(descriptor);
        when(descriptor.getProperties()).thenReturn(Collections.emptyList());

        componentManager.registerMockComponent(DashboardRenderer.class, "columns");
        componentManager.registerMockComponent(GadgetRenderer.class);

        ExecutionContext ec = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(ec);

        DashboardMacroParameters parameters = new DashboardMacroParameters();
        MacroTransformationContext macroContext = new MacroTransformationContext();
        this.macro.execute(parameters, "", macroContext);

        // We verify that the counter ends up at 0 so that calls to subsequent dashboard macros can succeed.
        assertEquals(0, ec.getProperty("dashboardMacroCalls"));
    }
}
