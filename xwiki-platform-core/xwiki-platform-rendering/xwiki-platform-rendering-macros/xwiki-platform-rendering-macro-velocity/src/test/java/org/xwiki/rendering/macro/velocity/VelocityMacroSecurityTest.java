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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verify that a Velocity macro's execution can be restricted.
 *
 * @version $Id$
 * @since 4.2M1
 */
@ComponentTest
@ComponentList({VelocityMacroPermissionPolicy.class, DefaultObservationManager.class, PermissionCheckerListener.class})
class VelocityMacroSecurityTest
{
    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private MacroManager macroManager;

    @MockComponent
    private BeanManager beanManager;

    @InjectMockComponents
    private VelocityMacro velocityMacro;

    @BeforeEach
    void setUp() throws Exception
    {
        BeanDescriptor mockBeanDescriptor = mock(BeanDescriptor.class);
        when(mockBeanDescriptor.getProperties()).thenReturn(Collections.emptyList());

        when(this.beanManager.getBeanDescriptor(any(Class.class))).thenReturn(mockBeanDescriptor);

        when(this.macroManager.getMacro(any(MacroId.class))).thenReturn((Macro) this.velocityMacro);
    }

    @Test
    void restrictedByContext()
    {
        VelocityMacroParameters params = new VelocityMacroParameters();
        MacroTransformationContext context = new MacroTransformationContext();
        context.setSyntax(Syntax.XWIKI_2_0);
        context.setCurrentMacroBlock(new MacroBlock("velocity", Collections.<String, String>emptyMap(), false));
        context.setId("page1");

        // Restrict the transformation context.
        context.getTransformationContext().setRestricted(true);

        when(this.authorizationManager.hasAccess(Right.SCRIPT)).thenReturn(true);

        assertThrows(MacroExecutionException.class, () -> {
            this.velocityMacro.execute(params, "#macro(testMacrosAreLocal)mymacro#end", context);
        });
    }

    @Test
    void restrictedByRights()
    {
        VelocityMacroParameters params = new VelocityMacroParameters();
        MacroTransformationContext context = new MacroTransformationContext();
        context.setSyntax(Syntax.XWIKI_2_0);
        context.setCurrentMacroBlock(new MacroBlock("velocity", Collections.<String, String>emptyMap(), false));
        context.setId("page1");

        context.getTransformationContext().setRestricted(false);

        // Restrict the SCRIPT right.
        when(this.authorizationManager.hasAccess(Right.SCRIPT)).thenReturn(false);

        assertThrows(MacroExecutionException.class, () -> {
            this.velocityMacro.execute(params, "#macro(testMacrosAreLocal)mymacro#end", context);
        });

        verify(this.authorizationManager).hasAccess(Right.SCRIPT);
    }
}
