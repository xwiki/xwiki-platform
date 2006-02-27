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
 * @author sdumitriu
 */

package com.xpn.xwiki.xmlrpc;

import java.io.UnsupportedEncodingException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.web.*;

public class BaseRpcHandler extends Object {

    private XWikiEngineContext econtext;
    private XWikiRequest request;
    private XWikiResponse response;

    public BaseRpcHandler(XWikiRequest request, XWikiResponse response, XWikiEngineContext econtext) {
        this.request = request;
        this.response = response;
        this.econtext = econtext;
    }

    public XWikiContext init() throws XWikiException {
        XWikiContext context = Utils.prepareContext("", request, response, econtext);
        XWiki xwiki = XWiki.getXWiki(context);
        XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
        context.setURLFactory(urlf);
        XWikiVelocityRenderer.prepareContext(context);
        return context;
    }


    protected byte[] convertToBase64( String content )
    {
        try
        {
            return content.getBytes("UTF-8");
        }
        catch( UnsupportedEncodingException e )
        {
            return content.getBytes();
        }
    }

}

