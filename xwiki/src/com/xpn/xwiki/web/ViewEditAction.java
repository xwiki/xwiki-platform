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
        session.setAttribute("doc", doc);
        session.setAttribute("context", context);
        session.setAttribute("xwiki", xwiki);

        // Determine what to do
        if ( action.equals("view") )
        {
            // forward to view template
            return (mapping.findForward("view"));
        }
        else if ( action.equals("edit") )
        {
            PrepareEditForm eform = (PrepareEditForm) form;
            String parent = eform.getParent();
            if (parent!=null)
                doc.setParent(parent);
            String template = eform.getTemplate();
            if ((template!=null)&&(!template.equals(""))) {
                String content = doc.getContent();
                if ((content==null)||(!content.equals(""))) {
                    Object[] args = { doc.getFullName() };
                    throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY,
                            "Cannot add a template to document {0} because it already has content", null, args);
                } else {

                    if (template.indexOf('.')==-1) {
                        template = doc.getWeb() + "." + template;
                    }
                    XWikiDocInterface templatedoc = xwiki.getDocument(template);
                    if (templatedoc.isNew()) {
                        Object[] args = { template, doc.getFullName() };
                        throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_APP_TEMPLATE_DOES_NOT_EXIST,
                                "Template document {0} does not exist when adding to document {1}", null, args);
                    } else {
                        doc.setTemplate(template);
                        doc.setContent(templatedoc.getContent());
                        if ((doc.getParent()==null)||(doc.getParent().equals(""))) {
                            String tparent = templatedoc.getParent();
                            if (tparent!=null)
                                doc.setParent(tparent);
                        }

                        // Merge the external objects
                        // Currently the choice is not to merge the base class and object because it is not
                        // the prefered way of using external classes and objects.
                        doc.mergexWikiObjects(templatedoc);
                    }
                }
            }

            // forward to edit template
            return (mapping.findForward("edit"));
        }
        else if ( action.equals("preview") )
        {
            EditForm eform = (EditForm)form;
            doc.setContent(eform.getContent());
            String parent = eform.getParent();
            if (parent!=null)
                doc.setParent(parent);
            BaseClass bclass = doc.getxWikiClass();
            if (bclass!=null)
                doc.setxWikiObject((BaseObject)bclass.fromMap(eform.getObject("object_")));

            // Get the class from the template
            String template = eform.getTemplate();
            if ((template!=null)&&(!template.equals(""))) {
                if (template.indexOf('.')==-1) {
                    template = doc.getWeb() + "." + template;
                }
                XWikiDocInterface templatedoc = xwiki.getDocument(template);
                if (templatedoc.isNew()) {
                    Object[] args = { template, doc.getFullName() };
                    throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_APP_TEMPLATE_DOES_NOT_EXIST,
                            "Template document {0} does not exist when adding to document {1}", null, args);
                } else {
                    doc.setTemplate(template);
                    doc.mergexWikiObjects(templatedoc);
                }
            }

            Iterator itobj = doc.getxWikiObjects().keySet().iterator();
            while (itobj.hasNext()) {
                String name = (String) itobj.next();
                BaseObject baseobject = (BaseObject)doc.getxWikiObjects().get(name);
                BaseClass baseclass = baseobject.getxWikiClass();
                BaseObject newobject = (BaseObject) baseclass.fromMap(eform.getObject(baseclass.getName() + "_"));
                newobject.setName(name);
                doc.getxWikiObjects().put(name, newobject);
            }
            return (mapping.findForward("preview"));
        }
        else if (action.equals("save"))
        {
            EditForm eform = (EditForm)form;
            doc.setContent(eform.getContent());
            String parent = eform.getParent();
            if (parent!=null)
                doc.setParent(parent);

            BaseClass bclass = doc.getxWikiClass();
            if (bclass!=null)
                doc.setxWikiObject((BaseObject)bclass.fromMap(((EditForm)form).getObject("object_")));

            // Get the class from the template
            String template = eform.getTemplate();
            if ((template!=null)&&(!template.equals(""))) {
                if (template.indexOf('.')==-1) {
                    template = doc.getWeb() + "." + template;
                }
                XWikiDocInterface templatedoc = xwiki.getDocument(template);
                if (templatedoc.isNew()) {
                    Object[] args = { template, doc.getFullName() };
                    throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_APP_TEMPLATE_DOES_NOT_EXIST,
                            "Template document {0} does not exist when adding to document {1}", null, args);
                } else {

                    doc.mergexWikiObjects(templatedoc);
                }
            }

            Iterator itobj = doc.getxWikiObjects().keySet().iterator();
            while (itobj.hasNext()) {
                String name = (String) itobj.next();
                BaseObject baseobject = (BaseObject)doc.getxWikiObjects().get(name);
                BaseClass baseclass = baseobject.getxWikiClass();
                BaseObject newobject = (BaseObject) baseclass.fromMap(eform.getObject(baseclass.getName() + "_"));
                newobject.setName(name);
                doc.getxWikiObjects().put(name, newobject);
            }
            xwiki.saveDocument(doc);
            // forward to list
            return (mapping.findForward("save"));
        }
        else if (action.equals("propupdate"))
        {
            BaseClass bclass = doc.getxWikiClass();
            Iterator it = bclass.getFields().values().iterator();
            while (it.hasNext()) {
                PropertyClass property = (PropertyClass)it.next();
                Map map = ((EditForm)form).getObject(property.getName() + "_");
                property.getxWikiClass().fromMap(map, property);
            }
            xwiki.saveDocument(doc);

            // forward to list
            return (mapping.findForward("propupdate"));
        }
        else if (action.equals("propadd"))
        {
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
                    xwiki.saveDocument(doc);
                }
            }
            return (mapping.findForward("propadd"));
        }
        else if (action.equals("classadd")) {
            String className = ((ClassAddForm) form).getClassName();
            doc.createNewObject(className, context);
            xwiki.saveDocument(doc);
            return (mapping.findForward("classadd"));
        }
        return (mapping.findForward("view"));
    }


    public List getClassList() throws XWikiException {
        throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_SEARCH,
                "Exception while searching class list");
    }
}

