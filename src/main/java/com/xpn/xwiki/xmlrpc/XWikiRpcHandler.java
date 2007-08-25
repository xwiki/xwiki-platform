/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
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
 */

package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import java.util.List;

public class XWikiRpcHandler extends BaseRpcHandler implements XWikiRpcInterface
{

    public String getPage(String name) throws XWikiException, Exception
    {
        try {
            XWikiContext context = getXWikiContext();
            XWikiDocument doc = context.getWiki().getDocument(name, context);
            return doc.getContent();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List getAllPages() throws XWikiException, Exception
    {
        try {
            XWikiContext context = getXWikiContext();
            List doclist = context.getWiki().getStore().searchDocumentsNames("", context);
            return doclist;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
