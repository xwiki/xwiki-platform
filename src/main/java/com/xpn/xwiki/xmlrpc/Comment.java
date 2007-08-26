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

import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class Comment extends org.codehaus.swizzle.confluence.Comment
{

    public Comment()
    {
        super();
    }
    
    public Comment(Map data)
    {
        super(data);
    }

    public Comment(XWikiDocument doc, BaseObject obj, XWikiContext context) throws XWikiException
    {
        if (doc.isMostRecent()) {
            setId(doc.getFullName() + ";" + obj.getNumber());
            setPageId(doc.getFullName());
            setUrl(doc.getURL("view", context));
        } else {
            setId(doc.getFullName() + ":" + doc.getVersion() + ";" + obj.getNumber());
            setPageId(doc.getFullName() + ":" + doc.getVersion());
            setUrl(doc.getURL("view", "rev=" + doc.getVersion(), context));
        }
        setTitle(doc.getName());
        setContent(obj.getStringValue("comment"));
        setCreated(obj.getDateValue("date"));
        setCreator(obj.getStringValue("author"));
    }
}
