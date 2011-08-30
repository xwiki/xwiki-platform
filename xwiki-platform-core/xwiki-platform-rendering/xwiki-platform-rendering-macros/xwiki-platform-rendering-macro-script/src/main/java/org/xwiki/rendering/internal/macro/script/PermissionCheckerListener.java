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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.script.PrivilegedScriptMacro;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Listens to {@link org.xwiki.script.event.ScriptEvaluatingEvent} and aborts execution if the user is
 * not permitted to execute the script.
 * 
 * @version $Id$
 * @since 2.5M1
 */
@Component
@Named("permissionchecker")
@Singleton
public class PermissionCheckerListener extends AbstractScriptCheckerListener
{
    /** Used to find the type of a Macro defined by a Macro Marker block. */
    @Inject
    private MacroManager macroManager;

    /** Used to check if the current document's author has programming rights. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;
    
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    public String getName()
    {
        return "permissionchecker";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.macro.script.AbstractScriptCheckerListener#check(org.xwiki.observation.event.CancelableEvent, org.xwiki.rendering.transformation.MacroTransformationContext, org.xwiki.rendering.macro.script.ScriptMacroParameters)
     */
    @Override
    protected void check(CancelableEvent event, MacroTransformationContext context,
        ScriptMacroParameters parameters)
    {
        try {
            MacroId currentMacro = new MacroId(context.getCurrentMacroBlock().getId());
            if (!(macroManager.getMacro(currentMacro) instanceof PrivilegedScriptMacro)) {
                // no special permission needed
                return;
            }
            // with not protected script engine, we are testing if the current dcument's author has "programming" right
            if (!this.documentAccessBridge.hasProgrammingRights()) {
                event.cancel("You don't have the right to execute this script");
            }
        } catch (MacroLookupException exception) {
            // should not happen, this method was called from that macro
        }
    }
}

