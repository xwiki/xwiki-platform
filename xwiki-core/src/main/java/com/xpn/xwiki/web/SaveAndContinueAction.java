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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import java.io.IOException;

public class SaveAndContinueAction extends XWikiAction {
    public boolean action(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();

        String back = request.getParameter("xcontinue");
        if (back == null || back.equals("")) {
            back = request.getParameter("xredirect");
            if (back == null || back.equals("")) {
                back = request.getHeader("Referer");
                if (back == null || back.equals("")) {
                    back = context.getDoc().getURL("edit", context);
                } else {
                    int qm = back.indexOf('?');
                    String base = back.substring(0, qm != -1 ? qm : back.length());
                    String query = "";
                    int start = back.indexOf("editor=");
                    if (start != -1) {
                        int end = back.indexOf('&', start);
                        if (end == -1) {
                            end = back.length();
                        }
                        query = query + back.substring(start, end);
                    }
                    back = base + "?" + query;
                }
            }
        }

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
        // Forward back to the originating page
        try {
            response.sendRedirect(back);
        } catch (IOException ignored) {
        }
        return false;
    }

    public String render(XWikiContext context) throws XWikiException {
        return "exception";
    }
}
