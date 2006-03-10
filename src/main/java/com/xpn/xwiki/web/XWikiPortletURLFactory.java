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
 * @author namphunghai
 * @author wr0ngway
 * @author sdumitriu
 */

package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.portlet.PortletURL;
import java.net.URL;
import java.util.Map;

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

    public URL createAttachmentURL(String filename, String web, String name, String action, String xwikidb, XWikiContext context) {
        try {
            XWikiResponse response = context.getResponse();
            PortletURL purl;
            if (action.equals("download"))
                return super.createAttachmentURL(filename, web, name, action, xwikidb, context);    //To change body of overridden methods use File | Settings | File Templates.
            else if (action.equals("viewattachrev")) {
                purl = response.createRenderURL();
                purl.setParameter("filename", filename);
                purl.setParameter("action", action);
                purl.setParameter("topic", web + "." + name);
            } else {
                purl = response.createActionURL();
                purl.setParameter("topic", web + "." + name);
                purl.setParameter("filename", filename);
                purl.setParameter("action", action);
            }

            if (log.isDebugEnabled())
                log.debug("Generated URL is: " + purl.toString());

            return new URL(serverURL, purl.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public URL createURL(String web, String name, String action, String querystring, String anchor, String xwikidb, XWikiContext context) {
        try {
            if (log.isDebugEnabled())
                log.debug("Generating URL for: " + xwikidb + ":" + web + "." + name + " for action " + action
                        + " with querystring " + querystring + " and anchor " + anchor);

            XWikiResponse response = context.getResponse();
            PortletURL purl;

            if (action.equals("view") || action.equals("viewrev") || action.equals("attach") || action.equals("download") || action.equals("downloadrev") || action.equals("viewattachrev") || action.equals("skin") || action.equals("dot"))
                purl = response.createRenderURL();
            else
            if (action.equals("save") || action.equals("cancel") || action.equals("delete") || action.equals("propupdate")
                    || action.equals("propadd") || action.equals("propdelete")
                    || action.equals("objectadd") || action.equals("objectremove")
                    || action.equals("commentadd") || action.equals("editprefs")
                    || action.equals("upload") || action.equals("delattachment")
                    || action.equals("login") || action.equals("logout"))
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
        if (redirect == false)
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
