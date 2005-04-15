/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 16 juin 2004
 * Time: 23:30:39
 */
package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

import java.util.List;
import java.util.Vector;

public class XWikiRpcHandler extends BaseRpcHandler {

    public XWikiRpcHandler(XWikiRequest request, XWikiResponse response, XWikiEngineContext econtext) {
        super(request, response, econtext);
    }

    // JSP Wiki API
    public Vector getAllPages() throws XWikiException, Exception {
        try {
        XWikiContext context = init();
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
          XWikiContext context = init();
          XWikiDocument doc = context.getWiki().getDocument(name, context);
          return convertToBase64( doc.getContent());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
