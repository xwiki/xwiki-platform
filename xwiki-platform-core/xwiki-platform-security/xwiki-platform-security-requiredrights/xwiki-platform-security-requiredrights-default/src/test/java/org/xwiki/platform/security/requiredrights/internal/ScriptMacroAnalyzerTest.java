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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.properties.BeanManager;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.script.MacroPermissionPolicy;
import org.xwiki.rendering.macro.script.PrivilegedScriptMacro;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests for {@link ScriptMacroAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class ScriptMacroAnalyzerTest
{
    @InjectMockComponents
    private ScriptMacroAnalyzer analyzer;

    @MockComponent
    private BeanManager beanManager;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void analyzeWithMacroPermissionPolicy(boolean hasPermission) throws Exception
    {
        MacroBlock macroBlock = mock();
        String macroId = "myScript";
        when(macroBlock.getId()).thenReturn(macroId);
        String macroContent = "#Script";
        when(macroBlock.getContent()).thenReturn(macroContent);
        Map<String, String> parameters = mock();
        when(macroBlock.getParameters()).thenReturn(parameters);

        // Mock the descriptor with the parameters bean class.
        Macro<?> macro = mock();
        MacroDescriptor macroDescriptor = mock(MacroDescriptor.class);
        when(macro.getDescriptor()).thenReturn(macroDescriptor);
        doReturn(ScriptMacroParameters.class).when(macroDescriptor).getParametersBeanClass();

        MacroPermissionPolicy mpp = mock();
        when(this.contextComponentManager.getInstance(MacroPermissionPolicy.class, macroId)).thenReturn(mpp);

        when(mpp.hasPermission(any(), any())).thenReturn(hasPermission);
        Right myRight = mock();
        when(mpp.getRequiredRight()).thenReturn(myRight);

        MacroTransformationContext macroTransformationContext = mock();
        List<RequiredRightAnalysisResult> analysisResults =
            this.analyzer.analyze(macroBlock, macro, macroTransformationContext);

        verify(this.beanManager).populate(any(ScriptMacroParameters.class), eq(parameters));
        verify(mpp).hasPermission(any(ScriptMacroParameters.class), eq(macroTransformationContext));

        if (hasPermission) {
            assertEquals(List.of(), analysisResults);
        } else {
            assertEquals(List.of(new RequiredRightAnalysisResult(DefaultMacroBlockRequiredRightAnalyzer.ID,
                "security.requiredrights.scriptmacro", List.of(macroId, macroContent),
                myRight, EntityType.DOCUMENT)), analysisResults);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "true, true, true, true",
        "true, false, true, false",
        "false, true, true, true",
        "false, false, true, true",
        "false, false, false, false"
    })
    void analyzeWithFallback(boolean isPrivileged, boolean hasProgrammingRight, boolean hasScriptRight, boolean allow)
        throws ComponentLookupException
    {
        String macroId = "myScript";
        when(this.contextComponentManager.getInstance(MacroPermissionPolicy.class, macroId))
            .thenThrow(new ComponentLookupException("Not found"));
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(hasProgrammingRight);
        when(this.authorizationManager.hasAccess(Right.SCRIPT)).thenReturn(hasScriptRight);

        MacroBlock macroBlock = mock();
        when(macroBlock.getId()).thenReturn(macroId);
        String macroContent = "#Script";
        when(macroBlock.getContent()).thenReturn(macroContent);
        MacroDescriptor macroDescriptor = mock(MacroDescriptor.class);
        doReturn(ScriptMacroParameters.class).when(macroDescriptor).getParametersBeanClass();

        Macro<?> macro = isPrivileged ? mock(withSettings().extraInterfaces(PrivilegedScriptMacro.class)) : mock();
        when(macro.getDescriptor()).thenReturn(macroDescriptor);

        Right requiredRight = isPrivileged ? Right.PROGRAM : Right.SCRIPT;

        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(macroBlock, macro, mock());

        if (allow) {
            assertEquals(List.of(), analysisResults);
        } else {
            assertEquals(List.of(new RequiredRightAnalysisResult(DefaultMacroBlockRequiredRightAnalyzer.ID,
                "security.requiredrights.scriptmacro", List.of(macroId, macroContent),
                requiredRight, EntityType.DOCUMENT)), analysisResults);
        }
    }
}
