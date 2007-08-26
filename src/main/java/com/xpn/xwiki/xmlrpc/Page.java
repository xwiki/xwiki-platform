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
 */
package com.xpn.xwiki.xmlrpc;

import java.util.Map;

import org.suigeneris.jrcs.rcs.Version;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Represents a Page as described in the <a href="Confluence specification">
 * http://confluence.atlassian.com/display/DOC/Remote+API+Specification</a>.
 * 
 * @todo right now there's no validation done on any parameter and this class accepts null values
 *       for all parameters. In the future we need a validation strategy defined which corresponds
 *       to how this class is used: for creating a page, for udpating it, etc. The validation needs
 *       are different across the use cases so it might even be best to have different validation
 *       classes used where this class is used in the code.
 * @version $Id: $
 */
public class Page extends org.codehaus.swizzle.confluence.Page
{
    public Page()
    {
        super();
    }
    
    public Page(Map data)
    {
        super(data);
    }
    
    public Page(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        // since we don't have multiple inheritance
        // we had to copy paste this initial part from PageSummary
        if (doc.isMostRecent()) {
            // Current version of document
            setId(doc.getFullName());
            setUrl(doc.getURL("view", context));
        } else {
            // Old version of document
            setId(doc.getFullName() + ":" + doc.getVersion());
            setUrl(doc.getURL("view", "rev="+doc.getVersion(), context));
        }
        setSpace(doc.getSpace());
        setParentId(doc.getParent());
        setTitle(doc.getName());
        setLocks(0);

        setVersion(constructVersion(doc.getRCSVersion()));
        setContent(doc.getContent());
        setCreated(doc.getCreationDate());
        setCreator(doc.getAuthor());
        setModified(doc.getDate());
        setModifier(doc.getAuthor());
        setHomePage((doc.getName().equals("WebHome")));
    }

    // TODO this needs to be documented
    // also used in PageHistorySummary
    public static int constructVersion(Version ver)
    {
        return ((ver.at(0)-1) << 16) + ver.at(1);
    }
}
