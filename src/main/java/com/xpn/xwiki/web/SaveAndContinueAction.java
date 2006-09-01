/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author ludovic
 * @author namphunghai
 * @author sdumitriu
 */
package com.xpn.xwiki.web;

import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class SaveAndContinueAction extends XWikiAction {
	public boolean action(XWikiContext context) throws XWikiException {
		XWikiRequest request = context.getRequest();
		XWikiResponse response = context.getResponse();

		String back = request.getParameter("xredirect");
		if (back == null || back.equals("")) {
			back = request.getHeader("Referer");
			if (back == null || back.equals("")) {
				back = context.getDoc().getURL("edit", context);
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
