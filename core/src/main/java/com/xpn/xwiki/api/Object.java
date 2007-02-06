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
 * @author amelentev
 */
package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class Object extends Collection
{
    public Object(BaseObject obj, XWikiContext context)
    {
        super(obj, context);
    }

    protected BaseObject getBaseObject()
    {
        return (BaseObject) getCollection();
    }

    public BaseObject getXWikiObject()
    {
        if (hasProgrammingRights()) {
            return (BaseObject) getCollection();
        } else {
            return null;
        }
    }

    public java.lang.Object get(String name)
    {
        String docname = getCollection().getName();
        try {
            XWikiDocument doc = getXWikiContext().getWiki().getDocument(docname, getXWikiContext());
            return doc.display(name, this.getBaseObject(), getXWikiContext());
        } catch (XWikiException e) {
            return null;
        }
    }

    public java.lang.Object display(String name, String mode)
    {
        String docname = getCollection().getName();
        try {
            XWikiDocument doc = getXWikiContext().getWiki().getDocument(docname, getXWikiContext());
            return doc.display(name, mode, this.getBaseObject(), getXWikiContext());
        } catch (XWikiException e) {
            return null;
        }
    }

    public boolean equals(java.lang.Object arg0)
    {
        if (!(arg0 instanceof Object)) {
            return false;
        }
        Object o = (Object) arg0;
        return o.getXWikiContext().equals(getXWikiContext()) && element.equals(o.element);
    }

    public void set(String fieldname, java.lang.Object value)
    {
        getBaseObject().set(fieldname, value, getXWikiContext());
    }
}
