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
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.struts.action.*;
import com.xpn.xwiki.doc.*;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.objects.meta.MetaClass;

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
        // session is needed for the velocity renderer
        // I need to check-out why..
        session = request.getSession();

        // fetch action from mapping
        action = mapping.getName();

        // Test works with xwiki-test.cfg instead of xwiki.cfg
        boolean test = false;
        if (request.getServletPath().startsWith ("/testbin")) {
          test = true;
        }

        servlet.log("[DEBUG] ViewEditAction at perform(): Action ist " + action);
        XWikiContext context = new XWikiContext();
        context.setServlet(servlet);
        XWiki xwiki = getXWiki(context, test);
        XWikiDocInterface doc;
        doc = xwiki.getDocumentFromPath(request.getPathInfo());
        context.put("doc", doc);
        context.setRequest(request);
        context.setAction(this);
        session.setAttribute("doc", doc);
        session.setAttribute("context", context);
        session.setAttribute("xwiki", xwiki);
        // Determine what to do
        if ( action.equals("view") )
        {
            // forward to view template
            return (mapping.findForward("view"));
        }
        else if ( action.equals("inline")) {
            // Set display context to 'view'
            context.put("display", "edit");

            // forward to view template
            return (mapping.findForward("inline"));
        }
        else if ( action.equals("edit") )
        {
            PrepareEditForm peform = (PrepareEditForm) form;
            String parent = peform.getParent();
            if (parent!=null)
                doc.setParent(parent);

            doc.readFromTemplateForEdit(peform, context);

            // forward to edit template
            return (mapping.findForward("edit"));
        }
        else if ( action.equals("preview") )
        {
            doc.readFromForm((EditForm)form, context);
            return (mapping.findForward("preview"));
        }
        else if (action.equals("save"))
        {
            XWikiDocInterface olddoc = (XWikiDocInterface) doc.clone();
            doc.readFromForm((EditForm)form, context);
            xwiki.saveDocument(doc, olddoc, context);

            // forward to view
            response.sendRedirect(doc.getActionUrl("view",context));
            return null;
        }
        else if (action.equals("propupdate"))
        {
            XWikiDocInterface olddoc = (XWikiDocInterface) doc.clone();
            BaseClass bclass = doc.getxWikiClass();
            Iterator it = bclass.getFields().values().iterator();
            while (it.hasNext()) {
                PropertyClass property = (PropertyClass)it.next();
                Map map = ((EditForm)form).getObject(property.getName());
                property.getxWikiClass().fromMap(map, property);
            }
            xwiki.saveDocument(doc, olddoc, context);

            // forward to edit
            response.sendRedirect(doc.getActionUrl("edit",context));
            return null;
        }
        else if (action.equals("propadd"))
        {
            XWikiDocInterface olddoc = (XWikiDocInterface) doc.clone();
            String propName = ((PropAddForm) form).getPropName();
            String propType = ((PropAddForm) form).getPropType();
            BaseClass bclass = doc.getxWikiClass();
            if (bclass.get(propName)!=null) {
                // TODO: handle the error of the property already existing when we want to add a class property
            } else {
                MetaClass mclass = xwiki.getMetaclass();
                PropertyMetaClass pmclass = (PropertyMetaClass) mclass.get(propType);
                if (pmclass!=null) {
                    PropertyClass pclass = (PropertyClass) pmclass.newObject();
                    pclass.setObject(bclass);
                    pclass.setName(propName);
                    pclass.setPrettyName(propName);
                    bclass.put(propName, pclass);
                    xwiki.saveDocument(doc, olddoc, context);
                }
            }
            // forward to edit
            response.sendRedirect(doc.getActionUrl("edit",context));
            return null;
        }
        else if (action.equals("classadd")) {
            XWikiDocInterface olddoc = (XWikiDocInterface) doc.clone();
            String className = ((ClassAddForm) form).getClassName();
            doc.createNewObject(className, context);
            xwiki.saveDocument(doc, olddoc, context);
            return (mapping.findForward("classadd"));
        }
        // forward to edit
        response.sendRedirect(doc.getActionUrl("edit",context));
        return null;
    }


    public List getClassList() throws XWikiException {
        throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_SEARCH,
                "Exception while searching class list");
    }
}

    