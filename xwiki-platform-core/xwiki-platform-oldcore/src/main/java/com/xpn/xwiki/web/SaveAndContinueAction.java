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
package com.xpn.xwiki.web;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import org.xwiki.csrf.CSRFToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action used for saving and returning to the edit page rather than viewing changes.
 * 
 * @version $Id$
 */
public class SaveAndContinueAction extends XWikiAction
{

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveAndContinueAction.class);

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        CSRFToken csrf = Utils.getComponent(CSRFToken.class);
        String token = context.getRequest().getParameter("form_token");

        if (!csrf.isTokenValid(token)) {
            try {
                context.getResponse().setContentType("text/plain");
                context.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
                context.getResponse().setCharacterEncoding(context.getWiki().getEncoding());
                context.getResponse().getWriter().print(context.getMessageTool()
                                                        .get("core.editors.saveandcontinue.csrfCheckFailed"));
            } catch (IOException e) {
                LOGGER.error("Failed to send error in response to save & continue action with mismatching CSRF token.",
                             e);
            }

            return false;
        }

        // Try to find the URL of the edit page which we came from
        String back = findBackURL(context);

        // This will never be true if "back" comes from request.getHeader("referer")
        if (back != null && back.contains("editor=class")) {
            PropUpdateAction pua = new PropUpdateAction();
            if (pua.propUpdate(context)) {
                pua.render(context);
            }
        } else {
            SaveAction sa = new SaveAction();
            if (sa.save(context)) {
                sa.render(context);
            } else {
                // Lock back the document
                context.getDoc().getTranslatedDocument(context).setLock(context.getUser(), context);
            }
        }

        // If this is an ajax request, no need to redirect.
        if (Utils.isAjaxRequest(context)) {
            context.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
            return false;
        }

        // Forward back to the originating page
        try {
            context.getResponse().sendRedirect(back);
        } catch (IOException ignored) {
            // This exception is ignored because it will only be thrown if content has already been sent to the
            // response. This should never happen but we have to catch the exception anyway.
        }
        return false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        return "exception";
    }

    /**
     * Try to find the URL of the edit page which we came from.
     * 
     * @param context current xwiki context
     * @return URL of the edit page
     */
    private String findBackURL(XWikiContext context)
    {
        XWikiRequest request = context.getRequest();
        String back = request.getParameter("xcontinue");
        if (StringUtils.isEmpty(back)) {
            back = request.getParameter("xredirect");
        }
        if (StringUtils.isEmpty(back)) {
            back = removeAllParametersFromQueryStringExceptEditor(request.getHeader("Referer"));
        }
        if (StringUtils.isEmpty(back)) {
            back = context.getDoc().getURL("edit", context);
        }
        return back;
    }

    /**
     * @param url the URL to get a modified version of.
     * @return A modified version of the input url where all parameters are stripped from the query string except
     *         "editor"
     */
    private String removeAllParametersFromQueryStringExceptEditor(String url)
    {
        if (url == null) {
            return "";
        }

        String[] baseAndQuery = url.split("\\?");
        // No query string: no change.
        if (baseAndQuery.length < 2) {
            return url;
        }

        String[] queryBeforeAndAfterEditor = baseAndQuery[1].split("editor=");
        // No editor=* in query string: return URI
        if (queryBeforeAndAfterEditor.length < 2) {
            return baseAndQuery[0];
        }

        return baseAndQuery[0] + "?editor=" + queryBeforeAndAfterEditor[1].split("&")[0];
    }
}
