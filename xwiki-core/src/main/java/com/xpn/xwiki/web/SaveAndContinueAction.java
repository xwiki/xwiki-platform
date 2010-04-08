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
 *
 */
package com.xpn.xwiki.web;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Action used for saving and returning to the edit page rather than viewing changes.
 * 
 * @version $Id$
 */
public class SaveAndContinueAction extends XWikiAction
{
    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#action(XWikiContext)
     */
    @Override
    public boolean action(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();

        // Try to find the URL of the edit page which we came from.
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

        // This will never be true if "back" comes from request.getHeader("referer")
        if (back != null && back.indexOf("editor=class") >= 0) {
            PropUpdateAction pua = new PropUpdateAction();
            if (pua.propUpdate(context)) {
                pua.render(context);
            }
        } else {
            SaveAction sa = new SaveAction();
            if (sa.save(context)) {
                sa.render(context);
            }
        }

        // If this is an ajax request, no need to redirect.
        if (BooleanUtils.isTrue((Boolean) context.get("ajax"))) {
            context.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
            return false;
        }

        // Forward back to the originating page
        try {
            response.sendRedirect(back);
        } catch (IOException ignored) {
            // This exception is ignored because it will only be thrown if content has already been sent to the
            // response. This should never happen but we have to catch the exception anyway.
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#render(XWikiContext)
     */
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        return "exception";
    }

    /**
     * @param url the URL to get a modified version of.
     * @return A modified version of the input url where all parameters 
     *         are stripped from the query string except "editor"
     */
    private String removeAllParametersFromQueryStringExceptEditor(String url)
    {
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
