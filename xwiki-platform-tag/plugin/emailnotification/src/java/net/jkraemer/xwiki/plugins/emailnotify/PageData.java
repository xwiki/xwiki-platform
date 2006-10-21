/*
 * Copyright 2005 Jens Krämer
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
 * Created on 15.04.2005
 * Version: $Id$
 */
package net.jkraemer.xwiki.plugins.emailnotify;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @author <a href="mailto:jk@jkraemer.net">Jens Kraemer </a>
 */
public class PageData
{
    private final String wikiAndWebName;
    private final String fullPageName;
    private final String url;
    private final String wikiName;
    private final String docName;
    private final String web;

    public PageData (XWikiDocument document, XWikiContext context)
    {
        wikiName = context.getDatabase ();
        docName = document.getName ();
        web = document.getWeb ();
        url = document.getURL ("view", context);
        wikiAndWebName = new StringBuffer (wikiName).append (":").append (web).toString ();
        fullPageName = buildKey (document, context);
    }

    public String getDocName ()
    {
        return docName;
    }

    public String getUrl ()
    {
        return url;
    }

    public String getWeb ()
    {
        return web;
    }

    public String getWikiName ()
    {
        return wikiName;
    }

    public String getFullPageName ()
    {
        return fullPageName;
    }

    public String getWikiAndWebName ()
    {
        return wikiAndWebName;
    }

    /**
     * @param document
     * @param context
     * @return
     */
    public static final String buildKey (XWikiDocument document, XWikiContext context)
    {
        return new StringBuffer (context.getDatabase ()).append (":").append (document.getWeb ())
                .append (".").append (document.getName ()).toString ();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals (Object obj)
    {
        return fullPageName.equals ( ((PageData) obj).fullPageName);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode ()
    {
        return fullPageName.hashCode ();
    }

    public String toString ()
    {
        return getFullPageName ();
    }
}
