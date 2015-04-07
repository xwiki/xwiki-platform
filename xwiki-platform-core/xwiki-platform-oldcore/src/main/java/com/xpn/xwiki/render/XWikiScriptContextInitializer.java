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
package com.xpn.xwiki.render;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.script.ScriptContextInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * Inject in the {@link ScriptContext} the XWiki context and the {@link XWiki} instance for backward compatibility.
 * <p>
 * Bridge. To be removed later.
 *
 * @version $Id$
 */
@Component
@Named("xwiki")
@Singleton
public class XWikiScriptContextInitializer implements ScriptContextInitializer
{
    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Override
    public void initialize(ScriptContext scriptContext)
    {
        XWikiContext xcontext = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");

        if (scriptContext.getAttribute("util") == null) {
            // Put the Util API in the Script context.
            scriptContext.setAttribute("util", new com.xpn.xwiki.api.Util(xcontext.getWiki(), xcontext),
                ScriptContext.ENGINE_SCOPE);

            // We put the com.xpn.xwiki.api.XWiki object into the context and not the com.xpn.xwiki.XWiki one which is
            // for internal use only. In this manner we control what the user can access.
            scriptContext.setAttribute("xwiki", new XWiki(xcontext.getWiki(), xcontext), ScriptContext.ENGINE_SCOPE);

            scriptContext.setAttribute("request", xcontext.getRequest(), ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("response", xcontext.getResponse(), ScriptContext.ENGINE_SCOPE);

            // We put the com.xpn.xwiki.api.Context object into the context and not the com.xpn.xwiki.XWikiContext one
            // which is for internal use only. In this manner we control what the user can access.
            // We use "xcontext" because "context" is a reserved binding in JSR-223 specifications
            scriptContext.setAttribute("xcontext", new Context(xcontext), ScriptContext.ENGINE_SCOPE);

            // Make the Syntax Factory component available from Script.
            // TODO: We need to decide how we want to expose components in general and how to protect users from
            // "dangerous" apis.
            scriptContext.setAttribute("syntaxFactory", Utils.getComponent(SyntaxFactory.class),
                ScriptContext.ENGINE_SCOPE);

            // Make deprecated XWiki message tool available from scripts
            scriptContext.setAttribute("msg",
                new XWikiMessageTool(Utils.getComponent(ContextualLocalizationManager.class)),
                ScriptContext.ENGINE_SCOPE);
        }

        if (xcontext.getDoc() != null) {
            Document apiDocument = xcontext.getDoc().newDocument(xcontext);
            Document translatedDocument = apiDocument;
            try {
                translatedDocument = apiDocument.getTranslatedDocument();
            } catch (XWikiException e) {
                this.logger.warn("Failed to retrieve the translated document for [{}]. "
                    + "Continue using the default translation.", apiDocument.getFullName(), e);
            }
            scriptContext.setAttribute("doc", apiDocument, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("cdoc", translatedDocument, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("tdoc", translatedDocument, ScriptContext.ENGINE_SCOPE);
        }
    }
}
