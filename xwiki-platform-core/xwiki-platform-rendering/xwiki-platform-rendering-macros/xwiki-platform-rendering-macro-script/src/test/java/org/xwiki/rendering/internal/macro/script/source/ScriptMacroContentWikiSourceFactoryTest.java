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
package org.xwiki.rendering.internal.macro.script.source;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.rendering.macro.source.MacroContentWikiSource;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Validate {@link ScriptMacroContentWikiSourceFactory}.
 * 
 * @version $Id$
 */
@ComponentTest
class ScriptMacroContentWikiSourceFactoryTest
{
    @InjectMockComponents
    private ScriptMacroContentWikiSourceFactory factory;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @Test
    void getContent() throws MacroExecutionException, AccessDeniedException
    {
        MacroTransformationContext context = new MacroTransformationContext();
        MacroContentSourceReference reference = new MacroContentSourceReference("script", "variable");

        MacroExecutionException exception =
            assertThrows(MacroExecutionException.class, () -> this.factory.getContent(reference, context));
        assertEquals("No script context could be found in the current context", exception.getMessage());

        ScriptContext scriptContext = new SimpleScriptContext();
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(scriptContext);

        exception = assertThrows(MacroExecutionException.class, () -> this.factory.getContent(reference, context));
        assertEquals("No script context value could be found for name [variable]", exception.getMessage());

        context.getTransformationContext().setRestricted(true);

        exception = assertThrows(MacroExecutionException.class, () -> this.factory.getContent(reference, context));
        assertEquals("Script binding is not supported in a restricted context", exception.getMessage());

        context.getTransformationContext().setRestricted(false);

        scriptContext.setAttribute("variable", "value", ScriptContext.ENGINE_SCOPE);

        MacroContentWikiSource source = this.factory.getContent(reference, context);
        assertEquals("value", source.getContent());
        assertNull(source.getSyntax());
        assertEquals(reference, source.getReference());

        doThrow(AccessDeniedException.class).when(this.authorization).checkAccess(Right.SCRIPT);

        exception = assertThrows(MacroExecutionException.class, () -> this.factory.getContent(reference, context));
        assertEquals("Current author must have script right to access a script binding", exception.getMessage());
    }
}
