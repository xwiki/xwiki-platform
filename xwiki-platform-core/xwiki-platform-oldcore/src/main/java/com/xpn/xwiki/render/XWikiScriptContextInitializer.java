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
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.script.ScriptContextInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;

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
    private SyntaxFactory syntaxFactory;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void initialize(ScriptContext scriptContext)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

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
            // TODO: Actually this should probably be moved to legacy, we have a rendering ScriptService now
            scriptContext.setAttribute("syntaxFactory", this.syntaxFactory, ScriptContext.ENGINE_SCOPE);
        }

        // Current document
        XWikiDocument doc = xcontext.getDoc();
        if (doc != null) {
            setDocument(scriptContext, "doc", doc, xcontext);

            XWikiDocument tdoc = (XWikiDocument) xcontext.get("tdoc");
            if (tdoc == null) {
                try {
                    tdoc = doc.getTranslatedDocument(xcontext);
                } catch (XWikiException e) {
                    this.logger.warn("Failed to retrieve the translated document for [{}]. "
                        + "Continue using the default translation.", doc.getDocumentReference(), e);
                    tdoc = doc;
                }
            }
            setDocument(scriptContext, "tdoc", tdoc, xcontext);

            XWikiDocument cdoc = (XWikiDocument) xcontext.get("cdoc");
            if (cdoc == null) {
                cdoc = tdoc;
                if (cdoc == null) {
                    cdoc = doc;
                }
            }
            setDocument(scriptContext, "cdoc", cdoc, xcontext);
        }

        // Current secure document
        XWikiDocument sdoc = (XWikiDocument) xcontext.get("sdoc");
        if (sdoc != null) {
            setDocument(scriptContext, "sdoc", sdoc, xcontext);
        }

        // Miscellaneous
        scriptContext.setAttribute("locale", xcontext.getLocale(), ScriptContext.ENGINE_SCOPE);
    }

    private void setDocument(ScriptContext scriptContext, String key, XWikiDocument document, XWikiContext xcontext)
    {
        // Change the Document instance only if it's not already wrapping the same XWikiDocument (otherwise we might
        // loose modifications made in a previous script and not yet saved)
        Document previousDoc = (Document) scriptContext.getAttribute(key);
        if (previousDoc == null || !previousDoc.same(document)) {
            Document apiDocument = document.newDocument(xcontext);
            scriptContext.setAttribute(key, apiDocument, ScriptContext.ENGINE_SCOPE);
        }
    }
}
