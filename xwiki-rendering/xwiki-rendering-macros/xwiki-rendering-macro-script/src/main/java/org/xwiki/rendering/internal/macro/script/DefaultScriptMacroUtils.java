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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.script.ScriptMacro;
import org.xwiki.rendering.macro.script.ScriptMacroConfiguration;

/**
 * A collection of utility methods for script macros.
 * 
 * @version $Id$
 * @since 2.4M2
 */
@Component
public class DefaultScriptMacroUtils implements ScriptMacroUtils
{
    /** Used to get a macro by id. */
    @Requirement
    private MacroManager macroManager;

    /** Script macro configuration. */
    @Requirement
    private ScriptMacroConfiguration configuration;

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.internal.macro.script.ScriptMacroUtils#isScriptNested(org.xwiki.rendering.block.MacroBlock)
     */
    public boolean isScriptNested(MacroBlock currentBlock)
    {
        if (configuration.isNestedScriptsEnabled()) {
            return false;
        }
        // traverse the XDOM tree up to the root
        MacroMarkerBlock parent = currentBlock.getParentBlockByType(MacroMarkerBlock.class);
        while (parent != null) {
            String parentId = parent.getId();
            try {
                if (macroManager.getMacro(new MacroId(parentId)) instanceof ScriptMacro) {
                    return true;
                } else if ("include".equals(parentId)) {
                    // included documents intercept the chain of nested script macros with XWiki syntax
                    return false;
                }
            } catch (MacroLookupException exception) {
                // shouldn't happen, the parent macro was already successfully executed earlier
            }
            parent = parent.getParentBlockByType(MacroMarkerBlock.class);
        }
        return false;
    }
}
