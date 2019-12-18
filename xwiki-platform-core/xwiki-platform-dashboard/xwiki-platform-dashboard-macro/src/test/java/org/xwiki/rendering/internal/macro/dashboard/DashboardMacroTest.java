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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.properties.BeanDescriptor;
import org.xwiki.properties.BeanManager;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.dashboard.DashboardMacroParameters;
import org.xwiki.rendering.macro.dashboard.DashboardRenderer;
import org.xwiki.rendering.macro.dashboard.GadgetRenderer;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DashboardMacro}.
 *
 * @version $Id$
 */
public class DashboardMacroTest
{
    @Rule
    public MockitoComponentMockingRule<DashboardMacro> mocker =
        new MockitoComponentMockingRule<>(DashboardMacro.class);

    @Test
    public void executeWhenInsideDashboardMacro() throws Exception
    {
        BeanManager beanManager = this.mocker.getInstance(BeanManager.class);
        BeanDescriptor descriptor = mock(BeanDescriptor.class);
        when(beanManager.getBeanDescriptor(any())).thenReturn(descriptor);
        when(descriptor.getProperties()).thenReturn(Collections.emptyList());

        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = new ExecutionContext();
        when(execution.getContext()).thenReturn(ec);
        ec.setProperty("dashboardMacroCalls", 1);

        DashboardMacroParameters parameters = new DashboardMacroParameters();
        MacroTransformationContext macroContext = new MacroTransformationContext();

        try {
            this.mocker.getComponentUnderTest().execute(parameters, "", macroContext);
            fail("Exception should have been raised here");
        } catch (MacroExecutionException expected) {
            assertEquals("Dashboard macro recursion detected. Don't call the Dashboard macro inside of itself...",
                expected.getMessage());
        }
    }

    @Test
    public void executeWhenNotInsideDashboardMacro() throws Exception
    {
        BeanManager beanManager = this.mocker.getInstance(BeanManager.class);
        BeanDescriptor descriptor = mock(BeanDescriptor.class);
        when(beanManager.getBeanDescriptor(any())).thenReturn(descriptor);
        when(descriptor.getProperties()).thenReturn(Collections.emptyList());

        DashboardRenderer renderer = this.mocker.registerMockComponent(DashboardRenderer.class, "columns");

        GadgetRenderer gadgetRenderer = this.mocker.registerMockComponent(GadgetRenderer.class);

        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = new ExecutionContext();
        when(execution.getContext()).thenReturn(ec);

        DashboardMacroParameters parameters = new DashboardMacroParameters();
        MacroTransformationContext macroContext = new MacroTransformationContext();
        this.mocker.getComponentUnderTest().execute(parameters, "", macroContext);

        // We verify that the counter ends up at 0 so that calls to subsequent dashboard macros can succeed.
        assertEquals(0, ec.getProperty("dashboardMacroCalls"));
    }
}
