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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.ComponentMandatory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.script.MacroPermissionPolicy;
import org.xwiki.rendering.macro.script.PrivilegedScriptMacro;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Listens to {@link org.xwiki.script.event.ScriptEvaluatingEvent} and aborts execution if the user is
 * not permitted to execute the script.
 *
 * @version $Id$
 * @since 2.5M1
 */
@Component
@ComponentMandatory
@Named("permissionchecker")
@Singleton
public class PermissionCheckerListener extends AbstractScriptCheckerListener
{
    /** Used to find the type of a Macro defined by a Macro Marker block. */
    @Inject
    private MacroManager macroManager;

    /** Used to check if programming rights is allowed. */
    @Inject
    private ContextualAuthorizationManager authorizationManager;

    /**
     * Used to get Macro Permission Policy implementations.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public String getName()
    {
        return "permissionchecker";
    }

    @Override
    protected void check(CancelableEvent event, MacroTransformationContext context,
        ScriptMacroParameters parameters)
    {
        MacroId currentMacroId = new MacroId(context.getCurrentMacroBlock().getId());
        try {
            MacroPermissionPolicy mpp =
                this.componentManager.getInstance(MacroPermissionPolicy.class, currentMacroId.getId());
            if (!mpp.hasPermission(parameters, context)) {
                String sourceContentReference = extractSourceContentReference(context.getCurrentMacroBlock());
                String sourceText = "";
                if (sourceContentReference != null) {
                    sourceText = String.format(" in [%s]", sourceContentReference);
                } else {
                    sourceText = "";
                }
                event.cancel(String.format("The execution of the [%s] script macro is not allowed%s."
                    + " Check the rights of its last author or the parameters if it's rendered from another script.",
                    currentMacroId, sourceText));
            }
        } catch (ComponentLookupException e) {
            // Policy not found for macro, check permission using backward compatibility check
            backwardCompatibilityCheck(currentMacroId, event);
        }
    }

    /**
     * Used for backward compatibility. Uses the following algorithm:
     * <ul>
     *   <li>if the executing Macro doesn't implements PrivilegedScriptMacro then allow execution</li>
     *   <li>otherwise allow execution only if the current document has Programming Rights</li>
     * </ul>
     *
     * @param macroId the information about the current executing script macro
     * @param event the script event which we use to cancel script execution if permission is not allowed
     */
    private void backwardCompatibilityCheck(MacroId macroId, CancelableEvent event)
    {
        try {
            if (!(macroManager.getMacro(macroId) instanceof PrivilegedScriptMacro)) {
                // no special permission needed
                return;
            }
            // with not protected script engine, we are testing if programming right is allowed
            if (!this.authorizationManager.hasAccess(Right.PROGRAM)) {
                event.cancel(
                    String.format("You need Programming Rights to execute the script macro [%s]", macroId.getId()));
            }
        } catch (MacroLookupException exception) {
            // should not happen, this method was called from that macro
        }
    }

    private String extractSourceContentReference(Block source)
    {
        String contentSource = null;
        MetaDataBlock metaDataBlock =
                source.getFirstBlock(new MetadataBlockMatcher(MetaData.SOURCE), Block.Axes.ANCESTOR);
        if (metaDataBlock != null) {
            contentSource = (String) metaDataBlock.getMetaData().getMetaData(MetaData.SOURCE);
        }
        return contentSource;
    }
}
