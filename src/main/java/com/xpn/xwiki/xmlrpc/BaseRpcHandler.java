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

import java.io.UnsupportedEncodingException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

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
          XWiki.getXWiki(context);
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

