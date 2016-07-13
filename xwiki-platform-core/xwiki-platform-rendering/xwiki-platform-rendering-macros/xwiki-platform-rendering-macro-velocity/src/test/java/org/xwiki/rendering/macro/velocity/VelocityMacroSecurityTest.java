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
package org.xwiki.rendering.macro.velocity;

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.properties.BeanDescriptor;
import org.xwiki.properties.BeanManager;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.internal.macro.script.PermissionCheckerListener;
import org.xwiki.rendering.internal.macro.velocity.VelocityMacro;
import org.xwiki.rendering.internal.macro.velocity.VelocityMacroPermissionPolicy;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verify that a Velocity macro's execution can be restricted.
 *
 * @version $Id$
 * @since 4.2M1
 */
@ComponentList({VelocityMacroPermissionPolicy.class, DefaultObservationManager.class, PermissionCheckerListener.class})
public class VelocityMacroSecurityTest
{
    @Rule
    public MockitoComponentMockingRule<Macro<VelocityMacroParameters>> mocker =
        new MockitoComponentMockingRule<Macro<VelocityMacroParameters>>(VelocityMacro.class);

    ContextualAuthorizationManager authorizationManager;

    @Before
    public void setUp() throws Exception
    {
        authorizationManager = mocker.registerMockComponent(ContextualAuthorizationManager.class);

        BeanDescriptor mockBeanDescriptor = mock(BeanDescriptor.class);
        when(mockBeanDescriptor.getProperties()).thenReturn(Collections.EMPTY_LIST);

        BeanManager beanManager = mocker.getInstance(BeanManager.class);
        when(beanManager.getBeanDescriptor(Matchers.any(Class.class))).thenReturn(mockBeanDescriptor);

        Macro velocityMacro = mocker.getComponentUnderTest();
        MacroManager mockMacroManager = mocker.registerMockComponent(MacroManager.class);
        when(mockMacroManager.getMacro(Matchers.any(MacroId.class))).thenReturn(velocityMacro);
    }

    @Test(expected = MacroExecutionException.class)
    public void testRestrictedByContext() throws Exception
    {
        VelocityMacroParameters params = new VelocityMacroParameters();
        MacroTransformationContext context = new MacroTransformationContext();
        context.setSyntax(Syntax.XWIKI_2_0);
        context.setCurrentMacroBlock(new MacroBlock("velocity", Collections.<String, String>emptyMap(), false));
        context.setId("page1");

        // Restrict the transformation context.
        context.getTransformationContext().setRestricted(true);

        when(authorizationManager.hasAccess(Right.SCRIPT)).thenReturn(true);

        mocker.getComponentUnderTest().execute(params, "#macro(testMacrosAreLocal)mymacro#end", context);
    }

    @Test(expected = MacroExecutionException.class)
    public void testRestrictedByRights() throws Exception
    {
        VelocityMacroParameters params = new VelocityMacroParameters();
        MacroTransformationContext context = new MacroTransformationContext();
        context.setSyntax(Syntax.XWIKI_2_0);
        context.setCurrentMacroBlock(new MacroBlock("velocity", Collections.<String, String>emptyMap(), false));
        context.setId("page1");

        context.getTransformationContext().setRestricted(false);

        // Restrict the SCRIPT right.
        when(authorizationManager.hasAccess(Right.SCRIPT)).thenReturn(false);

        mocker.getComponentUnderTest().execute(params, "#macro(testMacrosAreLocal)mymacro#end", context);

        verify(authorizationManager.hasAccess(Right.SCRIPT));
    }
}
