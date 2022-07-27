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

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.script.MacroPermissionPolicy;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PermissionCheckerListener}.
 *
 * @version $Id$
 */
@ComponentTest
class PermissionCheckerListenerTest
{
    @InjectMockComponents
    private PermissionCheckerListener listener;

    @MockComponent
    @Named("macro")
    private MacroPermissionPolicy macroPermissionPolicy;

    @Test
    void checkWhenPermission()
    {
        CancelableEvent event = mock(CancelableEvent.class);
        MacroTransformationContext context = new MacroTransformationContext();
        MacroBlock macroBlock = new MacroBlock("macro", Collections.emptyMap(), false);
        context.setCurrentMacroBlock(macroBlock);
        ScriptMacroParameters parameters = new ScriptMacroParameters();

        when(this.macroPermissionPolicy.hasPermission(parameters, context)).thenReturn(true);

        this.listener.check(event, context, parameters);

        verifyNoInteractions(event);
    }

    @Test
    void checkWhenNoPermissionAndNoSourceMetaData()
    {
        CancelableEvent event = mock(CancelableEvent.class);
        MacroTransformationContext context = new MacroTransformationContext();
        MacroBlock macroBlock = new MacroBlock("macro", Collections.emptyMap(), false);
        context.setCurrentMacroBlock(macroBlock);
        ScriptMacroParameters parameters = new ScriptMacroParameters();
        when(this.macroPermissionPolicy.hasPermission(parameters, context)).thenReturn(false);

        this.listener.check(event, context, parameters);

        verify(event).cancel("The execution of the [macro] script macro is not allowed. Check the rights of its last "
            + "author or the parameters if it's rendered from another script.");
    }

    @Test
    void checkWhenNoPermissionAndSourceMetaData()
    {
        CancelableEvent event = mock(CancelableEvent.class);
        MacroTransformationContext context = new MacroTransformationContext();
        MacroBlock macroBlock = new MacroBlock("macro", Collections.emptyMap(), false);
        MetaData sourceMetaData = new MetaData();
        sourceMetaData.addMetaData(MetaData.SOURCE, "location");
        new MetaDataBlock(Arrays.asList(macroBlock), sourceMetaData);
        context.setCurrentMacroBlock(macroBlock);
        ScriptMacroParameters parameters = new ScriptMacroParameters();
        when(this.macroPermissionPolicy.hasPermission(parameters, context)).thenReturn(false);

        this.listener.check(event, context, parameters);

        verify(event).cancel("The execution of the [macro] script macro is not allowed in [location]. "
            + "Check the rights of its last author or the parameters if it's rendered from another script.");
    }
}
