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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.script.AbstractJSR223ScriptMacro;
import org.xwiki.rendering.macro.script.DefaultScriptMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Execute script in provided script language.
 * 
 * @version $Id$
 * @since 1.7M3
 */
@Component
@Named("script")
@Singleton
public class DefaultScriptMacro extends AbstractJSR223ScriptMacro<DefaultScriptMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Execute script in provided script language.";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "the script to execute";

    /**
     * The identifier of the script language. If null, {@link DefaultScriptMacroParameters#getLanguage()} has to be not
     * null.
     */
    private String language;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public DefaultScriptMacro()
    {
        super("Script", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION),
            DefaultScriptMacroParameters.class);
    }

    @Override
    protected String getScriptEngineName(DefaultScriptMacroParameters parameters, MacroTransformationContext context)
    {
        String engineName;

        if (this.language == null) {
            String macroName = context.getCurrentMacroBlock().getId().toLowerCase();

            if ("script".equals(macroName)) {
                engineName = parameters.getLanguage();
            } else {
                engineName = macroName;
            }
        } else {
            engineName = this.language;
        }

        return engineName;
    }
}
