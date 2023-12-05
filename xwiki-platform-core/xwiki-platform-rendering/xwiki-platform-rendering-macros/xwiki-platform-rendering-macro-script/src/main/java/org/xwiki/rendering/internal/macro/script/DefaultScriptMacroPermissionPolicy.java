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
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.macro.script.AbstractScriptMacroPermissionPolicy;
import org.xwiki.rendering.macro.script.DefaultScriptMacroParameters;
import org.xwiki.rendering.macro.script.MacroPermissionPolicy;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.Right;

/**
 * Default Script Macro Permission policy: delegate to specific Script Macro Permission Policy using the
 * {@code language} parameter and if no Policy is found then allow only if the current document has Programming Rights.
 *
 * @version $Id$
 * @since 4.1M1
 */
@Component
@Named("script")
@Singleton
public class DefaultScriptMacroPermissionPolicy extends AbstractScriptMacroPermissionPolicy
{
    /**
     * Used to get Macro Permission Policy implementations.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public boolean hasPermission(ScriptMacroParameters parameters, MacroTransformationContext context)
    {
        boolean hasPermission;
        try {
            hasPermission = getMacroPermissionPolicy(parameters).hasPermission(parameters, context);
        } catch (ComponentLookupException e) {
            // No policy for that Macro, use the default implementation which forbids execution if the doc doesn't
            // have Programming Rights.
            hasPermission = super.hasPermission(parameters, context);
        }
        return hasPermission;
    }

    @Override
    public Right getRequiredRight(ScriptMacroParameters parameters)
    {
        Right right;
        try {
            right = getMacroPermissionPolicy(parameters).getRequiredRight(parameters);
        } catch (ComponentLookupException e) {
            // No policy for that Macro, use the default implementation which requires Programming Rights.
            right = super.getRequiredRight(parameters);
        }
        return right;
    }

    private MacroPermissionPolicy getMacroPermissionPolicy(ScriptMacroParameters parameters)
        throws ComponentLookupException
    {
        return this.componentManager.getInstance(MacroPermissionPolicy.class,
            ((DefaultScriptMacroParameters) parameters).getLanguage());
    }
}
