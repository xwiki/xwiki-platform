/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 25 nov. 2003
 * Time: 21:20:04
 */


package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.fileupload.DefaultFileItem;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.velocity.VelocityContext;
import org.apache.log4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
    private static final Log log = LogFactory.getLog(ViewEditAction.class);


    public ViewEditAction() throws Exception {
        super();
    }


    public ActionForward parseTemplate(String template, XWikiContext context) throws IOException {
        HttpServletResponse response = context.getResponse();
        response.setContentType("text/html");
        String content = context.getWiki().parseTemplate(template + ".vm", context);
        response.setContentLength(content.length());
        response.getWriter().write(content);
        return null;
    }

    public String getRedirect(HttpServletRequest request, String defaultRedirect) {
        String redirect;
        redirect = request.getParameter("xredirect");
        if ((redirect == null)||(redirect.equals("")))
            redirect = defaultRedirect;
        return redirect;
    }

    public String getPage(HttpServletRequest request, String defaultpage) {
        String page;
        page = request.getParameter("xpage");
        if ((page == null)||(page.equals("")))
            page = defaultpage;
        return page;
    }


    private String getFileName(List filelist, String name) {
        DefaultFileItem  fileitem = null;
        for (int i=0;i<filelist.size();i++) {
            DefaultFileItem item = (DefaultFileItem) filelist.get(i);
            if (name.equals(item.getFieldName())) {
                fileitem = item;
                break;
            }
        }

        if (fileitem==null)
            return null;

        return fileitem.getName();
    }

    private byte[] getContent(List filelist, String name) throws IOException {
        DefaultFileItem  fileitem = null;
        for (int i=0;i<filelist.size();i++) {
            DefaultFileItem item = (DefaultFileItem) filelist.get(i);
            if (name.equals(item.getFieldName())) {
                fileitem = item;
                break;
            }
        }

        if (fileitem==null)
            return null;

        byte[] data = new byte[(int)fileitem.getSize()];
        InputStream fileis = fileitem.getInputStream();
        fileis.read(data);
        fileis.close();
        return data;
    }


    public List getClassList() throws XWikiException {
        throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_SEARCH,
                "Exception while searching class list");
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

        // fetch action from mapping
        action = mapping.getName();
        // Test works with xwiki-test.cfg instead of xwiki.cfg
        String dbname = "xwiki";
        String url = XWiki.getRequestURL(request);

        try {
            // Push the URL into the Log4j NDC context
            MDC.put("url", url);

            String baseUrl = "";
            if (request.getServletPath().startsWith ("/testbin")) {
                dbname = "xwikitest";
                baseUrl = url.substring(0, url.indexOf("/testbin/")) + "/testbin/";
            } else {
                baseUrl = url.substring(0, url.indexOf("/bin/")) + "/bin/";
            }

            servlet.log("[DEBUG] ViewEditAction at perform(): Action ist " + action);
            XWikiContext context = new XWikiContext();
            context.setBaseUrl(baseUrl);
            context.setServlet(servlet);
            context.setRequest(request);
            context.setResponse(response);
            context.setAction(this);
            context.setDatabase(dbname);

            // We should not go further for the Database Status
            // To make sure we don't have more database connections
            if (action.equals("dbstatus"))
                return executeDatabaseStatus(context);


            XWiki xwiki = XWiki.getXWiki(context);
            // Any error before this will be treated using a redirection to an error page

            VelocityContext vcontext = null;
            // Prepare velocity context
            vcontext = XWikiVelocityRenderer.prepareContext(context);


            try {
                // From there we will try to catch any exceptions and show a nice page

                XWikiDocInterface doc = null;

                doc = xwiki.getDocumentFromPath(request.getPathInfo(), context);
                context.put("doc", doc);

                vcontext.put("doc", new Document(doc, context));
                vcontext.put("cdoc",  vcontext.get("doc"));

                if (xwiki.checkAccess(action, doc, context)==false) {
                    return parseTemplate(getPage(request, "accessdenied"), context);
                }

                String checkactivefield = xwiki.getXWikiPreference("auth_active_check", context);
                if (checkactivefield.equals("1")) {
                    String username = context.getUser();
                    XWikiDocInterface userdoc = xwiki.getDocument(username, context);
                    int active = userdoc.getIntValue("XWiki.XWikiUsers", "active");

                    if (active==0) {
                        return parseTemplate(getPage(request, "userinactive"), context);
                    }
                }


                // Determine what to do
                if (action.equals("view"))
                    return executeView(xwiki, doc, request, context, vcontext);
                else if ( action.equals("inline"))
                    return executeInline(doc, form, request, context);
                else if ( action.equals("edit") )
                    return executeEdit(doc, form, request, context);
                else if ( action.equals("preview"))
                    return executePreview(doc, form, request, context, vcontext);
                else if (action.equals("save"))
                    return executeSave(xwiki, doc, form, request, response, context);
                else if (action.equals("delete"))
                    return executeDelete(xwiki, doc, request, response, context);
                else if (action.equals("propupdate"))
                    return executePropertyUpdate(xwiki, doc, form, request, response, context);
                else if (action.equals("propadd"))
                    return executePropertyAdd(xwiki, doc, form, request, response, context);
                else if (action.equals("objectadd"))
                    return executeObjectAdd(xwiki, doc, form, request, response, context);
                else if (action.equals("objectremove"))
                    return executeObjectRemove(xwiki, doc, form, request, response, context);
                else if (action.equals("download"))
                    return executeDownload(doc, request, response, context);
                else if (action.equals("attach"))
                    return parseTemplate(getPage(request, "attach"), context);
                else if (action.equals("upload"))
                    return executeUpload(xwiki, doc, request, response, context);
                else if (action.equals("delattachment"))
                    return executeDeleteAttachment(doc, request, response, context);
                else if (action.equals("skin"))
                    return executeSkin(xwiki, doc, request, response, context);
                else if (action.equals("login"))
                    return executeLogin(xwiki, doc, request, response, context);
                else if (action.equals("loginerror"))
                    return parseTemplate(getPage(request, "login"), context);
                else if (action.equals("logout"))
                    return executeLogout(xwiki, request, response, context);
            } catch (Throwable e) {
                vcontext.put("exp", e);
                try {
                    if (log.isWarnEnabled()) {
                           log.warn("Uncaught exception: " + e.getMessage(), e);
                    }
                    return parseTemplate(getPage(request, "exception"), context);
                } catch (Exception e2) {
                    // I hope this never happens
                    e.printStackTrace();
                    e2.printStackTrace();
                    return null;
                }
            } finally {
                // Make sure we cleanup database connections
                // There could be cases where we have some
                if ((context!=null)&&(xwiki!=null)) {
                    xwiki.getStore().cleanUp(context);
                }
            }

            // Let's redirect to an error page here..
            return null;
        } finally {
            MDC.remove("url");
        }
    }

    private ActionForward executeDatabaseStatus(XWikiContext context) throws IOException, XWikiException {
        XWiki xwiki = XWiki.getMainXWiki(context);
        VelocityContext vcontext = XWikiVelocityRenderer.prepareContext(context);
        vcontext.put("xwiki", xwiki);
        parseTemplate("dbstatus", context);
        return null;
    }

    private ActionForward executeLogout(XWiki xwiki, HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws IOException, XWikiException {
        String redirect = getRedirect(request, xwiki.getBase(context) + "view/Main/WebHome");
        response.sendRedirect(redirect);
        return null;
    }

    private ActionForward executeLogin(XWiki xwiki, XWikiDocInterface doc, HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws IOException, XWikiException {
        String page = getPage(request, "login");
        parseTemplate(page, context);
        return null;
    }


    private ActionForward executeSkin(XWiki xwiki, XWikiDocInterface doc, HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws IOException, XWikiException {
        String path = request.getPathInfo();
        String filename = path.substring(path.lastIndexOf("/")+1);

        BaseObject object = doc.getObject("XWiki.XWikiSkinClass", 0);
        String content = null;
        if (object!=null) {
            content = object.getStringValue(filename);
        }

        if ((content!=null)&&(!content.equals(""))) {
            // Choose the right content type
            response.setContentType(servlet.getServletContext().getMimeType(filename));
            // Sending the content of the attachment
            response.setContentLength(content.length());
            response.getWriter().write(content);
        }
        else {
            XWikiAttachment attachment = doc.getAttachment(filename);
            if (attachment!=null) {
                // Sending the content of the attachment
                byte[] data = attachment.getContent(context);
                response.setContentType(servlet.getServletContext().getMimeType(filename));
                response.setContentLength(data.length);
                response.getOutputStream().write(data);
            } else {
                // In this case we redirect to the default template file
                response.sendRedirect(xwiki.getBase(context) + "../skins/default/" + filename);
            }
        }
        return null;
    }

    private ActionForward executeDeleteAttachment(XWikiDocInterface doc, HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws XWikiException, IOException {
        String path = request.getPathInfo();
        String filename = path.substring(path.lastIndexOf("/")+1);
        XWikiAttachment attachment = null;

        if (request.getParameter("id")!=null) {
            int id = Integer.parseInt(request.getParameter("id"));
            attachment = (XWikiAttachment) doc.getAttachmentList().get(id);
        }
        else {
            attachment = doc.getAttachment(filename);
        }

        doc.deleteAttachment(attachment, context);
        // forward to attach page
        String redirect = getRedirect(request, doc.getActionUrl("attach",context));
        response.sendRedirect(redirect);
        return null;
    }

    private ActionForward executeUpload(XWiki xwiki, XWikiDocInterface doc, HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws FileUploadException, IOException, XWikiException {
        String username = context.getUser();
        XWikiDocInterface olddoc = (XWikiDocInterface) doc.clone();

        // Get the FileUpload Data
        DiskFileUpload fileupload = new DiskFileUpload();
        fileupload.setSizeMax(10000000);
        fileupload.setSizeThreshold(100000);

        String tempdir = xwiki.Param("xwiki.upload.tempdir");
        if (tempdir!=null) {
            fileupload.setRepositoryPath(tempdir);
            (new File(tempdir)).mkdirs();
        }
        else
            fileupload.setRepositoryPath(".");
        List filelist = fileupload.parseRequest(request);

        // I don't like it.. But this is the way
        // to get form elements..
        byte[] data = getContent(filelist, "filename");
        String filename = null;

        if (data!=null) {
            filename = new String(data);
        }

        // Get the file content
        data = getContent(filelist, "filepath");

        if (filename==null) {
            String fname = getFileName(filelist, "filepath");
            int i = fname.indexOf("\\");
            if (i==-1)
                i = fname.indexOf("/");
            filename = fname.substring(i+1);
        }


        // Read XWikiAttachment
        XWikiAttachment attachment = doc.getAttachment(filename);


        if (attachment==null) {
            attachment = new XWikiAttachment();
            doc.getAttachmentList().add(attachment);
        }
        attachment.setContent(data);
        attachment.setFilename(filename);

        // TODO: handle Author
        attachment.setAuthor(username);

        // Add the attachment to the document
        attachment.setDoc(doc);

        // Save the content and the archive
        doc.saveAttachmentContent(attachment, context);

        // forward to attach page
        response.sendRedirect(doc.getActionUrl("attach",context));
        return null;
    }

    private ActionForward executeDownload(XWikiDocInterface doc, HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws XWikiException, IOException {
        String path = request.getRequestURI();
        String filename = path.substring(path.lastIndexOf("/")+1);

        try {
            filename = (new URLCodec()).decode(filename);
        } catch (DecoderException e) {
        }

        XWikiAttachment attachment = null;

        if (request.getParameter("id")!=null) {
            int id = Integer.parseInt(request.getParameter("id"));
            attachment = (XWikiAttachment) doc.getAttachmentList().get(id);
        }
        else {
            attachment = doc.getAttachment(filename);
        }

        if (attachment==null) {
            Object[] args = { filename };
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND,
                    "Attachment {0} not found", null, args);
        }

        // Choose the right content type
        String mimetype = servlet.getServletContext().getMimeType(filename);
        if (mimetype!=null)
            response.setContentType(mimetype);
        else
            response.setContentType("application/octet-stream");

        // Sending the content of the attachment
        byte[] data = attachment.getContent(context);
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        return null;
    }

    private ActionForward executeObjectRemove(XWiki xwiki, XWikiDocInterface doc, ActionForm form, HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws XWikiException, IOException {
        XWikiDocInterface olddoc = (XWikiDocInterface) doc.clone();
        String className = ((ObjectRemoveForm) form).getClassName();
        int classId = ((ObjectRemoveForm) form).getClassId();
        Vector objects = doc.getObjects(className);
        BaseObject object = (BaseObject)objects.get(classId);
        // Remove it from the object list
        objects.set(classId, null);
        doc.addObjectsToRemove(object);
        xwiki.saveDocument(doc, olddoc, context);

        // forward to edit
        String redirect = getRedirect(request, doc.getActionUrl("edit",context));
        response.sendRedirect(redirect);
        return null;
    }

    private ActionForward executeObjectAdd(XWiki xwiki, XWikiDocInterface doc, ActionForm form, HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws XWikiException, IOException {
        XWikiDocInterface olddoc = (XWikiDocInterface) doc.clone();
        String className = ((ObjectAddForm) form).getClassName();
        doc.createNewObject(className, context);
        xwiki.saveDocument(doc, olddoc, context);

        // forward to edit
        String redirect = getRedirect(request, doc.getActionUrl("edit",context));
        response.sendRedirect(redirect);
        return null;
    }

    private ActionForward executePropertyAdd(XWiki xwiki, XWikiDocInterface doc, ActionForm form, HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws XWikiException, IOException {
        XWikiDocInterface olddoc = (XWikiDocInterface) doc.clone();
        String propName = ((PropAddForm) form).getPropName();
        String propType = ((PropAddForm) form).getPropType();
        BaseClass bclass = doc.getxWikiClass();
        bclass.setName(doc.getFullName());
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
        String redirect = getRedirect(request, doc.getActionUrl("edit",context));
        response.sendRedirect(redirect);
        return null;
    }

    private ActionForward executePropertyUpdate(XWiki xwiki, XWikiDocInterface doc, ActionForm form, HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws XWikiException, IOException {
        XWikiDocInterface olddoc = (XWikiDocInterface) doc.clone();

        // Prepare new class
        BaseClass bclass = doc.getxWikiClass();
        BaseClass bclass2 = (BaseClass)bclass.clone();
        bclass2.setFields(new HashMap());

        doc.setxWikiClass(bclass2);

        // Prepare a Map for field renames
        Map fieldsToRename = new HashMap();

        Iterator it = bclass.getFieldList().iterator();
        while (it.hasNext()) {
            PropertyClass property = (PropertyClass)it.next();
            PropertyClass origproperty = (PropertyClass) property.clone();
            String name = property.getName();
            Map map = ((EditForm)form).getObject(name);
            property.getxWikiClass(context).fromMap(map, property);
            String newname = property.getName();
            bclass2.addField(newname, property);
            if (!newname.equals(name)) {
                fieldsToRename.put(name, newname);
                bclass2.addPropertyForRemoval(origproperty);
            }
        }
        doc.renameProperties(bclass.getName(), fieldsToRename);
        xwiki.saveDocument(doc, olddoc, context);

        // We need to load all documents that use this property and rename it
        if (fieldsToRename.size()>0) {
            List list = xwiki.searchDocuments(", BaseObject as obj where obj.name=CONCAT(XWD_WEB,'.',XWD_NAME) and obj.className='" +
                    bclass.getName() +  "' and CONCAT(XWD_WEB,'.',XWD_NAME)<> '" + bclass.getName() + "'", context);
            for (int i=0;i<list.size();i++) {
                XWikiDocInterface doc2 = xwiki.getDocument((String)list.get(i), context);
                doc2.renameProperties(bclass.getName(), fieldsToRename);
                xwiki.saveDocument(doc2, doc2, context);
            }
        }
        xwiki.flushCache();
        // forward to edit
        String redirect = getRedirect(request, doc.getActionUrl("edit",context));
        response.sendRedirect(redirect);
        return null;
    }

    private ActionForward executeDelete(XWiki xwiki, XWikiDocInterface doc, HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws XWikiException, IOException {
        ActionForward result = null;
        String confirm = request.getParameter("confirm");
        if ((confirm!=null)&&(confirm.equals("1"))) {
            xwiki.deleteDocument(doc, context);
            result = parseTemplate(getPage(request, "deleted"), context);
        } else {
            String redirect = getRedirect(request, null);
            if (redirect==null)
                result = parseTemplate(getPage(request, "delete"), context);
            else
                response.sendRedirect(redirect);
        }
        return result;
    }

    private ActionForward executeSave(XWiki xwiki, XWikiDocInterface doc, ActionForm form, HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws XWikiException, IOException {
        String username = context.getUser();
        XWikiDocInterface olddoc = (XWikiDocInterface) doc.clone();
        doc.readFromForm((EditForm)form, context);

        // TODO: handle Author
        doc.setAuthor(username);

        xwiki.saveDocument(doc, olddoc, context);

        // forward to view
        String redirect = getRedirect(request, doc.getActionUrl("view",context));
        response.sendRedirect(redirect);
        return null;
    }

    private ActionForward executePreview(XWikiDocInterface doc, ActionForm form, HttpServletRequest request, XWikiContext context, VelocityContext vcontext) throws XWikiException, IOException {
        XWikiDocInterface doc2 = (XWikiDocInterface)doc.clone();
        context.put("doc", doc2);
        vcontext.put("doc", new Document(doc2, context));
        vcontext.put("cdoc",  vcontext.get("doc"));
        doc2.readFromForm((EditForm)form, context);
        // forward to view template
        String page = getPage(request, "preview");
        return parseTemplate(page, context);
    }

    private ActionForward executeEdit(XWikiDocInterface doc, ActionForm form, HttpServletRequest request, XWikiContext context) throws XWikiException, IOException {
        PrepareEditForm peform = (PrepareEditForm) form;
        String parent = peform.getParent();
        if (parent!=null)
            doc.setParent(parent);

        doc.readFromTemplateForEdit(peform, context);

        // forward to edit template
        String page = getPage(request, "edit");
        return parseTemplate(page, context);
    }

    private ActionForward executeInline(XWikiDocInterface doc, ActionForm form, HttpServletRequest request, XWikiContext context) throws XWikiException, IOException {
        PrepareEditForm peform = (PrepareEditForm) form;
        String parent = peform.getParent();
        if (parent!=null)
            doc.setParent(parent);

        doc.readFromTemplateForEdit(peform, context);

        // Set display context to 'view'
        context.put("display", "edit");

        // forward to inline template
        String page = getPage(request, "inline");
        return parseTemplate(page, context);
    }

    private ActionForward executeView(XWiki xwiki, XWikiDocInterface doc, HttpServletRequest request, XWikiContext context, VelocityContext vcontext) throws XWikiException, IOException {
        String rev = request.getParameter("rev");
        if (rev!=null) {
            // Let's get the revision
            doc = xwiki.getDocument(doc, rev, context);
            context.put("doc", doc);
            // We need to have the old version doc in the context
            vcontext.put("cdoc", new Document(doc, context));
        }
        // forward to view template
        String page = getPage(request, "view");
        return parseTemplate(page, context);
    }

}

