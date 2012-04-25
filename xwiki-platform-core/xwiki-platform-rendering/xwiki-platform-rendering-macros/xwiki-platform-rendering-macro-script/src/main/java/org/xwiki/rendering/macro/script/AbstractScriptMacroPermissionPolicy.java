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

import javax.inject.Inject;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Default Script Macro Permission policy that Script Macro implementations can extends: allow script execution only
 * if the current document has Programming Rights.
 *
 * @version $Id$
 * @since 4.1M1
 */
public abstract class AbstractScriptMacroPermissionPolicy implements MacroPermissionPolicy
{
    /**
     * Used to verify if the current doc has programming rights.
     */
    @Inject
    private DocumentAccessBridge dab;

    @Override
    public boolean hasPermission(ScriptMacroParameters parameters, MacroTransformationContext context)
    {
        return this.dab.hasProgrammingRights();
    }
}
