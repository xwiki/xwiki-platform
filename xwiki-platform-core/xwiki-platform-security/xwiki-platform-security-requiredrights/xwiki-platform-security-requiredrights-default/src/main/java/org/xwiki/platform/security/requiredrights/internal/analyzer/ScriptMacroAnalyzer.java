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
package org.xwiki.platform.security.requiredrights.internal.analyzer;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.properties.BeanManager;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.script.MacroPermissionPolicy;
import org.xwiki.rendering.macro.script.PrivilegedScriptMacro;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;
import org.xwiki.security.authorization.Right;

/**
 * Component for analyzing a script macro.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(ScriptMacroAnalyzer.ID)
public class ScriptMacroAnalyzer extends AbstractMacroBlockRequiredRightAnalyzer
    implements RequiredRightAnalyzer<MacroBlock>
{
    /**
     * The id of this analyzer.
     */
    public static final String ID = "script";

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private BeanManager beanManager;

    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    private BlockSupplierProvider<MacroBlock> macroDisplayerProvider;

    /**
     * @param macroBlock the macro block to analyze
     * @return the required rights for the macro
     */
    @Override
    public List<RequiredRightAnalysisResult> analyze(MacroBlock macroBlock)
    {
        Right requiredRight = Right.PROGRAM;
        Macro<?> macro = getMacro(macroBlock);

        try {
            Object macroParameters =
                macro.getDescriptor().getParametersBeanClass().getDeclaredConstructor().newInstance();
            this.beanManager.populate(macroParameters, macroBlock.getParameters());

            if (macroParameters instanceof ScriptMacroParameters) {
                MacroPermissionPolicy mpp =
                    this.componentManagerProvider.get().getInstance(MacroPermissionPolicy.class, macroBlock.getId());
                requiredRight = mpp.getRequiredRight((ScriptMacroParameters) macroParameters);
            }
        } catch (Exception ex) {
            if (!(macro instanceof PrivilegedScriptMacro)) {
                requiredRight = Right.SCRIPT;
            }
        }

        return generateResult(macroBlock, macroBlock.getId(), requiredRight);
    }

    private List<RequiredRightAnalysisResult> generateResult(MacroBlock macroBlock, String macroId, Right right)
    {
        EntityReference reference = extractSourceReference(macroBlock);

        String messageKey;
        List<RequiredRight> requiredRights;
        if (right == Right.PROGRAM) {
            messageKey = "security.requiredrights.macro.script.program";
            requiredRights = List.of(RequiredRight.PROGRAM);
        } else {
            messageKey = "security.requiredrights.macro.script.script";
            // Scripts macros could always use APIs that require programming right but this is impossible to check
            // reliably so manual review is required.
            requiredRights = List.of(new RequiredRight(right, EntityType.DOCUMENT, false), RequiredRight.MAYBE_PROGRAM);
        }

        return List.of(new RequiredRightAnalysisResult(reference,
            this.translationMessageSupplierProvider.get(messageKey, macroId),
            this.macroDisplayerProvider.get(macroBlock),
            requiredRights)
        );
    }
}
