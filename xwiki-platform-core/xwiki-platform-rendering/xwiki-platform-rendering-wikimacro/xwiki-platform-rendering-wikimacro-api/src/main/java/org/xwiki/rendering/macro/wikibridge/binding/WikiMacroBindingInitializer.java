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
package org.xwiki.rendering.macro.wikibridge.binding;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Initialize the binding provided to the script macros. Called before executing each wiki macro is executed.
 *
 * @version $Id$
 * @since 11.6RC1
 * @since 11.3.2
 * @since 10.11.9
 */
@Role
public interface WikiMacroBindingInitializer
{
    /**
     * Initialize the binding provided to the script macros.
     *
     * @param wikiMacro the wiki macro
     * @param parameters the parameters of the macro
     * @param macroContent the content of the macro
     * @param context the macro execution context
     * @param macroBinding the binding map to fill
     */
    void initialize(WikiMacro wikiMacro, WikiMacroParameters parameters, String macroContent,
        MacroTransformationContext context, WikiMacroBinding macroBinding);
}
