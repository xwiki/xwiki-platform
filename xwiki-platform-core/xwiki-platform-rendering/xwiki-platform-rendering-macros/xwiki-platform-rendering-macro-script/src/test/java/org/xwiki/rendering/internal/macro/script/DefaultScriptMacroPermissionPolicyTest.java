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
package org.xwiki.rendering.internal.macro.script;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.macro.script.DefaultScriptMacroParameters;
import org.xwiki.rendering.macro.script.MacroPermissionPolicy;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.xwiki.security.authorization.Right.PROGRAM;
import static org.xwiki.security.authorization.Right.SCRIPT;

/**
 * Test of {@link DefaultScriptMacroPermissionPolicy}.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@ComponentTest
class DefaultScriptMacroPermissionPolicyTest
{
    @InjectMockComponents
    private DefaultScriptMacroPermissionPolicy permissionPolicy;

    @MockComponent
    private ComponentManager componentManager;

    @Mock
    private MacroPermissionPolicy macroPermissionPolicy;

    @Test
    void getRequiredRight() throws Exception
    {
        DefaultScriptMacroParameters scriptMacroParameters = new DefaultScriptMacroParameters();
        String language = "en";
        scriptMacroParameters.setLanguage(language);
        when(this.componentManager.getInstance(MacroPermissionPolicy.class, language))
            .thenReturn(this.macroPermissionPolicy);
        when(this.macroPermissionPolicy.getRequiredRight(scriptMacroParameters)).thenReturn(SCRIPT);
        assertEquals(SCRIPT, this.permissionPolicy.getRequiredRight(scriptMacroParameters));
    }

    @Test
    void getRequiredRightComponentLookupException() throws Exception
    {
        DefaultScriptMacroParameters scriptMacroParameters = new DefaultScriptMacroParameters();
        String language = "en";
        scriptMacroParameters.setLanguage(language);
        when(this.componentManager.getInstance(MacroPermissionPolicy.class, language))
            .thenThrow(ComponentLookupException.class);
        assertEquals(PROGRAM, this.permissionPolicy.getRequiredRight(scriptMacroParameters));
    }
}
