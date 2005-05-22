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
 * Date: 27 mai 2004
 * Time: 09:48:22
 */
package com.xpn.xwiki;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.web.*;
import com.xpn.xwiki.pdf.impl.PdfExportImpl;
import com.xpn.xwiki.pdf.impl.PdfURLFactory;
import com.xpn.xwiki.pdf.api.PdfExport;
import com.xpn.xwiki.plugin.graphviz.GraphVizPlugin;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.ecs.html.P;

import java.io.*;
import java.util.*;

public class XWikiService {

    private static final Log log = LogFactory.getLog(XWikiService.class);
    private static final long UPLOAD_DEFAULT_MAXSIZE = 10000000L;
    private static final long UPLOAD_DEFAULT_SIZETHRESHOLD = 100000L;

    private void sendRedirect(XWikiResponse response, String page) throws XWikiException {
        try {
            if (page!=null)
             response.sendRedirect(page);
        } catch (IOException e) {
            Object[] args = { page };
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_REDIRECT_EXCEPTION,
                    "Exception while sending redirect to page {0}", e, args);
        }
    }

    /*
    *  Actions
    */

    public boolean actionLogout(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();

        String redirect;
        redirect = context.getRequest().getParameter("xredirect");
        if ((redirect == null)||(redirect.equals("")))
            redirect = context.getURLFactory().createURL("Main", "WebHome", "view", context).toString();
        sendRedirect(response, redirect);
        return false;
    }

    public boolean actionDelattachment(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String path = request.getPathInfo();
        String filename = Utils.decode(path.substring(path.lastIndexOf("/")+1),context);
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
        String redirect = Utils.getRedirect("attach", context);
        sendRedirect(response, redirect);
        return false;
    }

    public boolean actionUpload(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String username = context.getUser();
        // Get the FileUpload Data
        DiskFileUpload fileupload = new DiskFileUpload();
        fileupload.setSizeMax(xwiki.getXWikiPreferenceAsLong("upload_maxsize", UPLOAD_DEFAULT_MAXSIZE, context));
        fileupload.setSizeThreshold((int)xwiki.getXWikiPreferenceAsLong("upload_sizethreshold", UPLOAD_DEFAULT_SIZETHRESHOLD, context));

        String tempdir = xwiki.Param("xwiki.upload.tempdir");
        if (tempdir!=null) {
            fileupload.setRepositoryPath(tempdir);
            (new File(tempdir)).mkdirs();
        }
        else
            fileupload.setRepositoryPath(".");
        List filelist = null;
        try {
            filelist = fileupload.parseRequest(request.getHttpServletRequest());
        } catch (FileUploadException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_UPLOAD_PARSE_EXCEPTION,
                    "Exception while parsing uploaded file", e);
        }

        // I don't like it.. But this is the way
        // to get form elements..
        byte[] data = Utils.getContent(filelist, "filename");
        String filename = null;

        if (data!=null) {
            filename = new String(data);
        }

        // Get the file content
        data = Utils.getContent(filelist, "filepath");

        if (filename==null) {
            String fname = Utils.getFileName(filelist, "filepath");
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
        sendRedirect(response, doc.getURL("attach", true, context));
        return false;
    }

    public boolean actionObjectremove(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

        XWikiDocument olddoc = (XWikiDocument) doc.clone();
        String className = ((ObjectRemoveForm) form).getClassName();
        int classId = ((ObjectRemoveForm) form).getClassId();
        Vector objects = doc.getObjects(className);
        BaseObject object = (BaseObject)objects.get(classId);
        // Remove it from the object list
        objects.set(classId, null);
        doc.addObjectsToRemove(object);
        xwiki.saveDocument(doc, olddoc, context);

        // forward to edit
        String redirect = Utils.getRedirect("edit", context);
        sendRedirect(response, redirect);
        return false;
    }

    public boolean actionObjectadd(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        ObjectAddForm oform = (ObjectAddForm) context.getForm();

        XWikiDocument olddoc = (XWikiDocument) doc.clone();
        String className = oform.getClassName();
        int nb = doc.createNewObject(className, context);

        BaseObject oldobject = doc.getObject(className, nb);
        BaseClass baseclass = oldobject.getxWikiClass(context);
        BaseObject newobject = (BaseObject) baseclass.fromMap(oform.getObject(className), oldobject);
        newobject.setNumber(oldobject.getNumber());
        newobject.setName(doc.getFullName());
        doc.setObject(className, nb, newobject);
        xwiki.saveDocument(doc, olddoc, context);

        // forward to edit
        String redirect = Utils.getRedirect("edit", context);
        sendRedirect(response, redirect);
        return false;
    }

    public boolean actionCommentadd(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        ObjectAddForm oform = (ObjectAddForm) context.getForm();

        XWikiDocument olddoc = (XWikiDocument) doc.clone();
        String className = "XWiki.XWikiComments";
        int nb = doc.createNewObject(className, context);

        BaseObject oldobject = doc.getObject(className, nb);
        BaseClass baseclass = oldobject.getxWikiClass(context);
        BaseObject newobject = (BaseObject) baseclass.fromMap(oform.getObject(className), oldobject);
        newobject.setNumber(oldobject.getNumber());
        newobject.setName(doc.getFullName());
        doc.setObject(className, nb, newobject);
        xwiki.saveDocument(doc, olddoc, context);

        // forward to edit
        String redirect = Utils.getRedirect("edit", context);
        sendRedirect(response, redirect);
        return false;
    }

    public boolean actionPropadd(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

        XWikiDocument olddoc = (XWikiDocument) doc.clone();
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
        String redirect = Utils.getRedirect("edit", context);
        sendRedirect(response, redirect);
        return false;
    }

    public boolean actionPropupdate(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();
        XWikiDocument olddoc = (XWikiDocument) doc.clone();

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
            if (newname.indexOf(" ")!=-1) {
                newname = newname.replaceAll(" ","");
                property.setName(newname);
            }
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
            List list = xwiki.getStore().searchDocumentsNames(", BaseObject as obj where obj.name="
                                                              + xwiki.getFullNameSQL() + " and obj.className='"
                                                              + Utils.SQLFilter(bclass.getName()) +  "' and " + xwiki.getFullNameSQL() + "<> '"
                                                              + Utils.SQLFilter(bclass.getName()) + "'", context);
            for (int i=0;i<list.size();i++) {
                XWikiDocument doc2 = xwiki.getDocument((String)list.get(i), context);
                doc2.renameProperties(bclass.getName(), fieldsToRename);
                xwiki.saveDocument(doc2, doc2, context);
            }
        }
        xwiki.flushCache();
        // forward to edit
        String redirect = Utils.getRedirect("edit", context);
        sendRedirect(response, redirect);
        return false;
    }

    public boolean actionDelete(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String confirm = request.getParameter("confirm");
        if ((confirm!=null)&&(confirm.equals("1"))) {
            String language = xwiki.getLanguagePreference(context);
            if ((language==null)||(language.equals(""))||language.equals(doc.getDefaultLanguage())) {
                // Delete all documents
                List list = doc.getTranslationList(context);
                for (int i=0;i<list.size();i++) {
                    String lang = (String) list.get(i);
                    XWikiDocument tdoc = doc.getTranslatedDocument(lang, context);
                    xwiki.deleteDocument(tdoc, context);
                }
                xwiki.deleteDocument(doc, context);
            } else {
                // Only delete the translation
                XWikiDocument tdoc = doc.getTranslatedDocument(language, context);
                xwiki.deleteDocument(tdoc, context);
            }
            return true;
        } else {
            String redirect = Utils.getRedirect(request, null);
            if (redirect==null)
                return true;
            else {
                sendRedirect(response, redirect);
                return false;
            }
        }
    }

    public boolean actionRegister(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String register = request.getParameter("register");
        if ((register!=null)&&(register.equals("1"))) {
            int useemail = xwiki.getXWikiPreferenceAsInt("use_email_verification", 0, context);
            int result;
            if (useemail==1)
             result = xwiki.createUser(true, "edit", context);
            else
             result = xwiki.createUser(context);
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            vcontext.put("reg", new Integer(result));
        }

        String redirect = Utils.getRedirect(request, null);
        if (redirect==null)
            return true;
        else {
            sendRedirect(response, redirect);
            return false;
        }
    }

    public boolean actionSave(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

        String language = ((EditForm)form).getLanguage();
        String defaultLanguage = ((EditForm)form).getDefaultLanguage();
        XWikiDocument tdoc;

        if ((language==null)||(language.equals(""))||(language.equals("default"))||(language.equals(doc.getDefaultLanguage()))) {
            // Need to save parent and defaultLanguage if they have changed
            tdoc = doc;
        } else {
            tdoc = doc.getTranslatedDocument(language, context);
            if (tdoc == doc) {
                tdoc = new XWikiDocument(doc.getWeb(), doc.getName());
                tdoc.setLanguage(language);
                tdoc.setStore(doc.getStore());
            }
            tdoc.setTranslation(1);
        }

        XWikiDocument olddoc = (XWikiDocument) tdoc.clone();
        tdoc.readFromTemplate(((EditForm)form).getTemplate(), context);
        tdoc.readFromForm((EditForm)form, context);

        // TODO: handle Author
        String username = context.getUser();
        tdoc.setAuthor(username);
        if (tdoc.isNew())
         tdoc.setCreator(username);

        xwiki.saveDocument(tdoc, olddoc, context);
        XWikiLock lock = tdoc.getLock(context);
        if (lock != null && lock.getUserName().equals(username))
            tdoc.removeLock(context);
        // forward to view
        String redirect = Utils.getRedirect("view", context);
        sendRedirect(response, redirect);
        return false;
    }

    public boolean actionRollback(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        RollbackForm form = (RollbackForm) context.getForm();

        String rev = form.getRev();
        String language = form.getLanguage();
        XWikiDocument tdoc;

        if ((language==null)||(language.equals(""))||(language.equals("default"))||(language.equals(doc.getDefaultLanguage()))) {
            // Need to save parent and defaultLanguage if they have changed
            tdoc = doc;
        } else {
            tdoc = doc.getTranslatedDocument(language, context);
            if (tdoc == doc) {
                tdoc = new XWikiDocument(doc.getWeb(), doc.getName());
                tdoc.setLanguage(language);
            }
            tdoc.setTranslation(1);
        }

        XWikiDocument olddoc = (XWikiDocument) tdoc.clone();
        XWikiDocument newdoc = xwiki.getDocument(tdoc, rev, context);

        String username = context.getUser();
        newdoc.setAuthor(username);
        newdoc.setRCSVersion(tdoc.getRCSVersion());
        xwiki.saveDocument(newdoc, olddoc, context);

        // forward to view
        String redirect = Utils.getRedirect("view", context);
        sendRedirect(response, redirect);
        return false;
    }

    public boolean actionCancel(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

        String language = ((EditForm)form).getLanguage();
        String defaultLanguage = ((EditForm)form).getDefaultLanguage();
        XWikiDocument tdoc;

        if ((language==null)||(language.equals(""))||(language.equals("default"))||(language.equals(doc.getDefaultLanguage()))) {
            // Need to save parent and defaultLanguage if they have changed
            tdoc = doc;
        } else {
            tdoc = doc.getTranslatedDocument(language, context);
            if (tdoc == doc) {
                tdoc = new XWikiDocument(doc.getWeb(), doc.getName());
                tdoc.setLanguage(language);
                tdoc.setStore(doc.getStore());
            }
            tdoc.setTranslation(1);
        }

        String username = context.getUser();

        XWikiLock lock = tdoc.getLock(context);
        if (lock != null && lock.getUserName().equals(username))
            tdoc.removeLock(context);

        // forward to view
        String redirect = Utils.getRedirect("view", context);
        sendRedirect(response, redirect);
        return false;
    }

    public boolean actionSkin(XWikiContext context) throws XWikiException {
                return true;
    }


    /*
    *  Rendering of pages
    */
    public String renderStatus(XWikiContext context) throws XWikiException {
        XWiki xwiki = XWiki.getMainXWiki(context);
        VelocityContext vcontext = XWikiVelocityRenderer.prepareContext(context);
        vcontext.put("xwiki", xwiki);
        return "status";
    }


    public String renderLogin(XWikiContext context) throws XWikiException {
        return "login";
    }

    public String renderLoginerror(XWikiContext context) throws XWikiException {
        return "login";
    }

    public String renderSkin(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String path = request.getPathInfo();
        String filename = Utils.decode(path.substring(path.lastIndexOf("/")+1),context);

        if (renderSkin(filename, doc, context))
            return null;

        String baseskin = xwiki.getBaseSkin(context, true);
        if (renderSkin(filename, baseskin, context))
            return null;

        XWikiDocument baseskindoc = xwiki.getDocument(baseskin, context);
        if (renderSkin(filename, baseskindoc, context))
                    return null;

        String defaultbaseskin = xwiki.getDefaultBaseSkin(context);
        renderSkin(filename, defaultbaseskin, context);
        return null;
    }

    private boolean renderSkin(String filename, XWikiDocument doc, XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();

        BaseObject object = doc.getObject("XWiki.XWikiSkins", 0);
        String content = null;
        if (object!=null) {
            content = object.getStringValue(filename);
        }

        if ((content!=null)&&(!content.equals(""))) {
            // Choose the right content type
            response.setContentType(xwiki.getEngineContext().getMimeType(filename.toLowerCase()));
            response.setDateHeader("Last-Modified", doc.getDate().getTime());
            // Sending the content of the attachment
            response.setContentLength(content.length());
            try {
                response.getWriter().write(content);
                return true;
            } catch (IOException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                        XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                        "Exception while sending response", e);
            }
        }
        else {
            XWikiAttachment attachment = doc.getAttachment(filename);
            if (attachment!=null) {
                // Sending the content of the attachment
                byte[] data = attachment.getContent(context);
                response.setContentType(xwiki.getEngineContext().getMimeType(filename.toLowerCase()));
                response.setDateHeader("Last-Modified", attachment.getDate().getTime());
                response.setContentLength(data.length);
                try {
                    response.getOutputStream().write(data);
                    return true;
                } catch (IOException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                            XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                            "Exception while sending response", e);
                }
            }
        }
        return false;
    }

    private boolean renderSkin(String filename, String skin, XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        try {
            response.setDateHeader("Expires", (new Date()).getTime() + 30*24*3600*1000L);
            String path = "/skins/" + skin + "/" + filename;
            // Choose the right content type
            String mimetype = context.getEngineContext().getMimeType(filename.toLowerCase());
            if (mimetype!=null)
                response.setContentType(mimetype);
            else
                response.setContentType("application/octet-stream");

            // Sending the content of the file
            InputStream is  = context.getWiki().getResourceAsStream(path);
            if (is==null)
             return false;

            byte[] data = new byte[65535];
            while (is.read(data)!=-1) {
                response.getOutputStream().write(data);
            }
            return true;
        } catch (IOException e) {
            if (skin.equals(xwiki.getDefaultBaseSkin(context)))
             throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                    "Exception while sending response", e);
            else
             return false;
        }
    }

    public String renderAttach(XWikiContext context) throws XWikiException {
        return "attach";
    }

    public String renderDownload(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String path = request.getRequestURI();
        String filename = Utils.decode(path.substring(path.lastIndexOf("/")+1),context);
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
        String mimetype = attachment.getMimeType(context);
        response.setContentType(mimetype);

        response.setDateHeader("Last-Modified", attachment.getDate().getTime());
        // Sending the content of the attachment
        byte[] data = attachment.getContent(context);
        response.setContentLength(data.length);
        try {
            response.getOutputStream().write(data);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                    "Exception while sending response", e);
        }
        return null;
    }

    public String renderDot(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        String path = request.getRequestURI();
        String filename = Utils.decode(path.substring(path.lastIndexOf("/")+1),context);
        try {
           ((GraphVizPlugin)context.getWiki().getPlugin("graphviz",context)).outputDotImageFromFile(filename, context);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                    "Exception while sending response", e);
        }
        return null;
    }

    public String renderDelete(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String confirm = request.getParameter("confirm");
        if ((confirm!=null)&&(confirm.equals("1"))) {
            return "deleted";
        } else {
            return "delete";
        }
    }

    public String renderRegister(XWikiContext context) throws XWikiException {
        return "register";
    }

    public String renderPreview(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");

        String language = ((EditForm)form).getLanguage();
        XWikiDocument tdoc;

        // Make sure it is not considered as new
        XWikiDocument doc2 = (XWikiDocument)doc.clone();
        context.put("doc", doc2);

        if ((language==null)||(language.equals(""))||(language.equals("default"))||(language.equals(doc.getDefaultLanguage()))) {
            tdoc = doc2;
            context.put("tdoc", doc2);
            vcontext.put("doc", new Document(doc2, context));
            vcontext.put("tdoc", vcontext.get("doc"));
            vcontext.put("cdoc",  vcontext.get("doc"));
            doc2.readFromTemplate(((EditForm)form).getTemplate(), context);
            doc2.readFromForm((EditForm)form, context);
        } else {
            // Need to save parent and defaultLanguage if they have changed
            tdoc = doc.getTranslatedDocument(language, context);
            tdoc.setLanguage(language);
            tdoc.setTranslation(1);
            XWikiDocument tdoc2 = (XWikiDocument)tdoc.clone();
            context.put("tdoc", tdoc2);
            vcontext.put("tdoc", new Document(tdoc2, context));
            vcontext.put("cdoc",  vcontext.get("tdoc"));
            tdoc2.readFromTemplate(((EditForm)form).getTemplate(), context);
            tdoc2.readFromForm((EditForm)form, context);
        }
        return "preview";
    }

    public String renderEdit(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");

        XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
        PrepareEditForm peform = (PrepareEditForm) form;
        String parent = peform.getParent();
        if (parent!=null)
            doc.setParent(parent);
        String creator = peform.getCreator();
        if (creator!=null)
            doc.setCreator(creator);
        String defaultLanguage = peform.getDefaultLanguage();
        if ((defaultLanguage!=null)&&!defaultLanguage.equals(""))
            doc.setDefaultLanguage(defaultLanguage);
        if (doc.getDefaultLanguage().equals(""))
            doc.setDefaultLanguage(context.getWiki().getLanguagePreference(context));

        String language = context.getWiki().getLanguagePreference(context);
        String languagefromrequest = context.getRequest().getParameter("language");
        String languagetoedit = ((languagefromrequest==null)||(languagefromrequest.equals(""))) ?
                language : languagefromrequest;

        if ((languagetoedit==null)||(languagetoedit.equals("default")))
            languagetoedit = "";
        if (doc.isNew()||(doc.getDefaultLanguage().equals(languagetoedit)))
            languagetoedit = "";

        if (languagetoedit.equals("")) {
            // In this case the created document is going to be the default document
            tdoc = doc;
            context.put("tdoc", doc);
            vcontext.put("tdoc", vcontext.get("doc"));
            if (doc.isNew()) {
                doc.setDefaultLanguage(language);
                doc.setLanguage("");
            }
        } else {
            // If the translated doc object is the same as the doc object
            // this means the translated doc did not exists so we need to create it
            if ((tdoc==doc)) {
                tdoc = new XWikiDocument(doc.getWeb(), doc.getName());
                tdoc.setLanguage(languagetoedit);
                tdoc.setContent(doc.getContent());
                tdoc.setAuthor(context.getUser());
                tdoc.setStore(doc.getStore());
                context.put("tdoc", tdoc);
                vcontext.put("tdoc", new Document(tdoc, context));
            }
        }

        /* Setup a lock */
        try {
        XWikiLock lock = tdoc.getLock(context);
        if (lock == null || lock.getUserName().equals(context.getUser()) || peform.isLockForce())
            tdoc.setLock(context.getUser(),context);
        } catch (Exception e) {
            // Lock should never make XWiki fail
            // But we should log any related information
            log.error("Exception while setting up lock", e);
        }

        XWikiDocument tdoc2 = (XWikiDocument) tdoc.clone();
        context.put("tdoc", tdoc2);
        vcontext.put("tdoc", new Document(tdoc2, context));
        tdoc2.readFromTemplate(peform, context);
        return "edit";
    }

    public String renderInline(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

        PrepareEditForm peform = (PrepareEditForm) form;
        String parent = peform.getParent();
        if (parent!=null)
            doc.setParent(parent);
        String creator = peform.getCreator();
        if (creator!=null)
            doc.setCreator(creator);
        String defaultLanguage = peform.getDefaultLanguage();
        if ((defaultLanguage!=null)&&!defaultLanguage.equals(""))
            doc.setDefaultLanguage(defaultLanguage);
        if (doc.getDefaultLanguage().equals(""))
            doc.setDefaultLanguage(context.getWiki().getLanguagePreference(context));

        doc.readFromTemplate(peform, context);

        // Set display context to 'view'
        context.put("display", "edit");
        return "inline";
    }

    public String renderView(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        String rev = request.getParameter("rev");

        if (rev!=null) {
            context.put("rev", rev);
            XWikiDocument doc = (XWikiDocument) context.get("doc");
            XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
            XWikiDocument rdoc = context.getWiki().getDocument(doc, rev, context);
            XWikiDocument rtdoc = context.getWiki().getDocument(tdoc, rev, context);
            context.put("tdoc", rtdoc);
            context.put("cdoc", rdoc);
            context.put("doc", rdoc);
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            vcontext.put("doc", new Document(rdoc, context));
            vcontext.put("cdoc", vcontext.get("doc"));
            vcontext.put("tdoc", new Document(rtdoc, context));
        }
        return "view";
    }

    public String renderPDF(XWikiContext context) throws XWikiException {
        context.setURLFactory(new PdfURLFactory(context));
        PdfExportImpl pdfexport = new PdfExportImpl();
        XWikiDocument doc = context.getDoc();
        try {
         context.getResponse().setContentType("application/pdf");
         context.getResponse().addHeader("Content-disposition", "attachment; filename=" + doc.getWeb() + "_" + doc.getName() + ".pdf");

         pdfexport.exportToPDF(doc, context.getResponse().getOutputStream(), context);
        } catch (IOException e) {
           throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                "Exception while sending response", e);
        }
        return null;
    }
}
