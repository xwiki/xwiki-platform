/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 25 nov. 2003
 * Time: 21:20:04
 */


package com.xpn.xwiki.web;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.struts.action.*;
import com.xpn.xwiki.doc.*;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;

/**
 * <p>A simple action that handles the display and editing of an
 * wiki page.. </p>
 *
 * <p>The action support an <i>action</i> URL. The action in the URL
 * controls what this action class does. The following values are supported:</p>
 * <ul>
 *    <li>view - view the Wiki Document
 *   <li>edit - edit the Wiki Document
 *   <li>preview - preview the Wiki Document
 *   <li>save - save the Wiki Document
 * </ul>
 * 
 */
public class ViewEditAction extends XWikiAction
{

    public ViewEditAction() throws Exception {
        super();
    }

    // --------------------------------------------------------- Public Methods
    /**
     * Handle server requests.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception occurs
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
            throws Exception, ServletException
    {
        String action;
        HttpSession session;

        // ActionErrors errors = new ActionErrors();

            session = request.getSession();

            // fetch action from mapping
            action = mapping.getName();

            servlet.log("[DEBUG] ViewEditAction at perform(): Action ist " + action);
            XWiki xwiki = getXWiki();
            XWikiDocInterface doc;
            doc = xwiki.getDocument(request.getPathInfo());
            XWikiContext context = new XWikiContext(xwiki);
            session.setAttribute("doc", doc);
            session.setAttribute("context", context);
            session.setAttribute("xwiki", xwiki);

            // Determine what to do
            if ( action.equals("view") )
            {
                // TODO: Need to get the Wiki Document..
                // TODO: Pass the Wiki Document through the rendering engine
                // forward to view template
                return (mapping.findForward("view"));
            }
            else if ( action.equals("edit") )
            {
                // TODO: Need to get the Wiki Document..

                // forward to edit template
                return (mapping.findForward("edit"));
            }
            else if ( action.equals("preview") )
            {
                // TODO: Store the edited content in a temporary object in the session
                // TODO: Pass the edited content through the rendering engine
                doc.setContent(request.getParameter("content"));
                return (mapping.findForward("preview"));
            }
            else if (action.equals("save"))
            {
                // TODO: check the edited content
                // TODO: save the content
                // TODO: forward to 'view'..
                doc.setContent(request.getParameter("content"));
                xwiki.saveDocument(doc);

                // forward to list
                return (mapping.findForward("save"));
            }
        return (mapping.findForward("view"));
    }
}

