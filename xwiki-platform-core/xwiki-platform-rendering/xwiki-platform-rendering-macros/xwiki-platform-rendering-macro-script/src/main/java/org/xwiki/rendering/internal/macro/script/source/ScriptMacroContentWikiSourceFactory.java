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
package org.xwiki.rendering.internal.macro.script.source;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.rendering.macro.source.MacroContentWikiSource;
import org.xwiki.rendering.macro.source.MacroContentWikiSourceFactory;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Get content from a script binding.
 * 
 * @version $Id$
 * @since 15.1RC1
 * @since 14.10.5
 */
@Component
@Singleton
@Named(MacroContentSourceReference.TYPE_SCRIPT)
public class ScriptMacroContentWikiSourceFactory implements MacroContentWikiSourceFactory
{
    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Override
    public MacroContentWikiSource getContent(MacroContentSourceReference reference, MacroTransformationContext context)
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

        ScriptContext scriptContext = this.scriptContextManager.getCurrentScriptContext();

        if (scriptContext == null) {
            throw new MacroExecutionException("No script context could be found in the current context");
        }

        Object value = scriptContext.getAttribute(reference.getReference());

        if (value == null) {
            throw new MacroExecutionException(
                "No script context value could be found for name [" + reference.getReference() + "]");
        }

        return new MacroContentWikiSource(reference, value.toString(), null);
    }
}
