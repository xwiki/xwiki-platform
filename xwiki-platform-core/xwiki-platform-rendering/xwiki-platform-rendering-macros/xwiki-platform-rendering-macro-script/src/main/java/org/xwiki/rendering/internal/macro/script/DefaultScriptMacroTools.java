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

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.script.ScriptContext;

import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.script.ScriptMacroTools;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Default implementation of {@link ScriptMacroTools}.
 * 
 * @version $Id$
 * @since 18.4.0RC1
 * @since 17.10.9
 */
@Component
@Singleton
public class DefaultScriptMacroTools implements ScriptMacroTools
{
    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private ConverterManager converterManager;

    @Override
    public <T> T getScriptValue(String variableName, MacroTransformationContext context, Type type)
        throws MacroExecutionException
    {
        // Script binding is not allowed in restricted context
        if (context.getTransformationContext().isRestricted()) {
            throw new MacroExecutionException("Script binding is not supported in a restricted context");
        }

        // Current author must have script right
        try {
            this.authorization.checkAccess(Right.SCRIPT);
        } catch (AccessDeniedException e) {
            throw new MacroExecutionException("Current author must have script right to access a script binding", e);
        }

        // Get the script context
        ScriptContext scriptContext = this.scriptContextManager.getCurrentScriptContext();

        Object value = null;

        if (scriptContext != null) {
            // Get the value from the script context
            value = scriptContext.getAttribute(variableName);

            // Converter the value if needed
            if (type != null) {
                value = this.converterManager.convert(type, value);
            }
        }

        return (T) value;
    }

    @Override
    public <T> T getScriptValue(String variableName, MacroTransformationContext context)
        throws MacroExecutionException
    {
        return getScriptValue(variableName, context, null);
    }
}
