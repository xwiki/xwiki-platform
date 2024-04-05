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
package org.xwiki.rendering.internal.macro.groovy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.groovy.GroovyConfiguration;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GroovyMacroPermissionPolicy}.
 *
 * @version $Id$
 */
@ComponentTest
class GroovyMacroPermissionPolicyTest
{
    @MockComponent
    private GroovyConfiguration configuration;

    @InjectMockComponents
    private GroovyMacroPermissionPolicy policy;

    @Test
    void getRequiredRightSecure()
    {
        when(this.configuration.getCompilationCustomizerNames()).thenReturn(List.of("secure"));

        assertEquals(Right.SCRIPT, this.policy.getRequiredRight(mock(ScriptMacroParameters.class)));
    }

    @Test
    void getRequiredRightNotSecure()
    {
        when(this.configuration.getCompilationCustomizerNames()).thenReturn(List.of());

        assertEquals(Right.PROGRAM, this.policy.getRequiredRight(mock(ScriptMacroParameters.class)));
    }
}
