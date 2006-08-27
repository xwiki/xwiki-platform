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
 * @author sdumitriu
 */

package com.xpn.xwiki.xmlrpc;

import java.util.List;
import java.util.Vector;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

public class XWikiRpcHandler extends BaseRpcHandler {

    // JSP Wiki API
    public Vector getAllPages() throws XWikiException, Exception {
        try {
        XWikiContext context = getXWikiContext();
        List doclist = context.getWiki().getStore().searchDocumentsNames("", context);
        Vector result = new Vector();
        for (int i=0;i<doclist.size();i++) {
            result.add(doclist.get(i));
        }
        return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public byte[] getPage(String name) throws XWikiException, Exception {
        try {
          XWikiContext context = getXWikiContext();
          XWikiDocument doc = context.getWiki().getDocument(name, context);
          return convertToBase64( doc.getContent());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
