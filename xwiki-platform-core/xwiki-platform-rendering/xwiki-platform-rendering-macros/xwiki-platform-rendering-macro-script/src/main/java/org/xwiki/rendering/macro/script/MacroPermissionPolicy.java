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
package org.xwiki.rendering.macro.script;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

/**
 * Decides whether a Script Macro can execute or not. Script Macros should implement this Role with a Hint being the
 * same as the Macro Hint.
 *
 * @version $Id$
 * @since 4.1M1
 */
@Role
public interface MacroPermissionPolicy
{
    /**
     * Verifies if the current execution Script Macro is allowed to execute its content or not.
     *
     * @param parameters the executing macro parameters
     * @param context the transformation context in which the current macro is executing
     * @return true if the script can execute or false otherwise
     */
    boolean hasPermission(ScriptMacroParameters parameters, MacroTransformationContext context);

    /**
     * Retrieves the required permission level for executing the script macro.
     *
     * @param parameters the executing macro parameters
     * @return the required permission level
     * @since 15.9RC1
     */
    @Unstable
    default Right getRequiredRight(ScriptMacroParameters parameters)
    {
        return Right.PROGRAM;
    }
}
