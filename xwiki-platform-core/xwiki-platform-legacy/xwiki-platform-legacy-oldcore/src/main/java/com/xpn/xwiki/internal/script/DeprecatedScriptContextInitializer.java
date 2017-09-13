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
package com.xpn.xwiki.internal.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.script.ScriptContextInitializer;

import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * Inject old msg binding in the script context.
 * 
 * @version $Id$
 * @since 8.3M1
 */
@Component
@Named("msg")
@Singleton
public class DeprecatedScriptContextInitializer implements ScriptContextInitializer
{
    @Inject
    private SyntaxFactory syntaxFactory;

    @Override
    public void initialize(ScriptContext scriptContext)
    {
        // Make deprecated XWiki message tool available from scripts
        scriptContext.setAttribute("msg", new XWikiMessageTool(Utils.getComponent(ContextualLocalizationManager.class)),
            ScriptContext.ENGINE_SCOPE);

        // Make the Syntax Factory component available from Script.
        // TODO: We need to decide how we want to expose components in general and how to protect users from
        // "dangerous" apis.
        scriptContext.setAttribute("syntaxFactory", this.syntaxFactory, ScriptContext.ENGINE_SCOPE);
    }
}
