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
package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.XWiki;

/**
 * This is an helper class that contains information about the XWiki context and the current XMLRPC
 * user.
 */
public class XWikiXmlRpcContext
{
    /*
     * The XWiki API used by XMLRPC functions to perform operations
     */
    private XWiki xwiki;

    /*
     * The low-level XWiki API object. This is needed in some methods where the functionality cannot
     * be completed by using the API wrapper (e.g., removing an attachment)
     */
    private com.xpn.xwiki.XWiki baseXWiki;

    /*
     * This XWikiContext is needed for performing operations with the low-level API.
     */
    private XWikiContext xwikiContext;

    public XWikiXmlRpcContext(XWikiContext xwikiContext)
    {
        this.xwikiContext = xwikiContext;
        this.baseXWiki = xwikiContext.getWiki();
        this.xwiki = new com.xpn.xwiki.api.XWiki(baseXWiki, xwikiContext);
    }

    public XWiki getXWiki()
    {
        return xwiki;
    }

    public XWikiContext getXWikiContext()
    {
        return xwikiContext;
    }

    public com.xpn.xwiki.XWiki getBaseXWiki()
    {
        return baseXWiki;
    }
}
