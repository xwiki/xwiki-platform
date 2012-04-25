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
package org.xwiki.rendering.internal.macro.groovy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.groovy.GroovyConfiguration;
import org.xwiki.rendering.macro.script.AbstractScriptMacroPermissionPolicy;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Decide if Groovy script execution is allowed. Allow execution if one of the following conditions is met:
 * <ul>
 *   <li>if the Secure Groovy Customizer is active</li>
 *   <li>if the current document has programming rights</li>
 * </ul>
 *
 * @version $Id$
 * @since 4.1M1
 */
@Component
@Named("groovy")
@Singleton
public class GroovyMacroPermissionPolicy extends AbstractScriptMacroPermissionPolicy
{
    /**
     * Used to verify if the Groovy Secure Customizer is active. If so, we delegate security checks to it.
     */
    @Inject
    private GroovyConfiguration configuration;

    @Override
    public boolean hasPermission(ScriptMacroParameters parameters, MacroTransformationContext context)
    {
        boolean hasPermission;
        if (this.configuration.getCompilationCustomizerNames().contains("secure")) {
            // Security is delegated to Groovy Secure Customizer
            hasPermission = true;
        } else {
            hasPermission = super.hasPermission(parameters, context);
        }
        return hasPermission;
    }
}
