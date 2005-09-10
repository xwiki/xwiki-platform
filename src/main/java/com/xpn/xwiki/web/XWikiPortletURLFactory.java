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
 * Date: 26 mai 2004
 * Time: 12:08:24
 */
package com.xpn.xwiki.web;

import java.net.URL;
import java.util.Map;

import javax.portlet.PortletURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;

public class XWikiPortletURLFactory extends XWikiServletURLFactory {

    private static final Log log = LogFactory.getLog(XWikiPortletURLFactory.class);

    public void init(XWikiContext context) {
        URL url = context.getURL();

        try {
            serverURL = new URL(url, "/");
        } catch (Exception e) {
        }

        servletPath = "xwiki/";
        actionPath = "bin/";
    }

    public URL createURL(String web, String name, String action, String querystring, String anchor, String xwikidb, XWikiContext context) {
        try {
            if (log.isDebugEnabled())
                log.debug("Generating URL for: " + xwikidb + ":" + web + "." + name + " for action " + action
                        + " with querystring " + querystring + " and anchor " + anchor);

            XWikiResponse response = context.getResponse();
            PortletURL purl;

            if (action.equals("view")||action.equals("download")||action.equals("skin")||action.equals("dot"))
                purl = response.createRenderURL();

            if (action.equals("save")||action.equals("cancel")||action.equals("delete")||action.equals("propupdate")
                    ||action.equals("propadd")||action.equals("propdelete")
                    ||action.equals("objectadd")||action.equals("objectremove")
                    ||action.equals("commentadd")||action.equals("editprefs")
                    ||action.equals("upload")||action.equals("delattachment")
                    ||action.equals("login")||action.equals("logout"))
                purl = response.createActionURL();
            else
                purl = response.createRenderURL();

            Map map = null;

            try {
                map = Utils.parseParameters(querystring, "UTF-8");
                purl.setParameters(map);
            } catch (Exception e) {
            }

            purl.setParameter("topic", web + "." + name);
            purl.setParameter("action", action);

            if (log.isDebugEnabled())
                log.debug("Generated URL is: " + purl.toString());

            return new URL(serverURL, purl.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public URL createURL(String web, String name, String action, boolean redirect, XWikiContext context) {
        if (redirect==false)
            return createURL(web, name, action, context);

        try {
            if (log.isDebugEnabled())
                log.debug("Generating Redirect URL for: " + web + "." + name + " for action " + action);

            XWikiResponse response = context.getResponse();
            response.setRenderParameter("topic", web + "." + name);
            response.setRenderParameter("action", action);
            return null;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public URL createExternalURL(String web, String name, String action, String querystring, String anchor, String xwikidb, XWikiContext context) {
        return super.createURL(web, name, action, querystring, anchor, xwikidb, context);
    }

}
