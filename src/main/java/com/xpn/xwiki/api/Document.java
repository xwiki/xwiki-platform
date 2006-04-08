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
 * @author torcq
 * @author amelentev
 * @author sdumitriu
 * @author thomas
 * @author tepich
 */


package com.xpn.xwiki.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.jrcs.diff.DifferentiationFailedException;
import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.Version;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.util.TOCGenerator;
import com.xpn.xwiki.util.Util;


public class Document extends Api {
    private XWikiDocument olddoc;
    private XWikiDocument doc;
    private Object currentObj;

    public Document(XWikiDocument doc, XWikiContext context) {
        super(context);
        this.olddoc = doc;
        this.doc = (XWikiDocument) doc.clone();
    }

    public XWikiDocument getDocument() {
        if (checkProgrammingRights())
            return doc;
        else
            return null;
    }

    protected XWikiDocument getDoc() {
        return doc;
    }

    public long getId() {
        return doc.getId();
    }

    public String getName() {
        return doc.getName();
    }

    public String getWeb() {
        return doc.getWeb();
    }

    public String getFullName() {
        return doc.getFullName();
    }

    public Version getRCSVersion() {
        return doc.getRCSVersion();
    }

    public String getVersion() {
        return doc.getVersion();
    }

    public String getTitle() {
        return doc.getTitle();
    }

    public String getDisplayTitle() {
        return doc.getDisplayTitle();
    }

    public String getFormat() {
        return doc.getFormat();
    }

    public String getAuthor() {
        return doc.getAuthor();
    }

    public String getContentAuthor() {
        return doc.getContentAuthor();
    }

    public Date getDate() {
        return doc.getDate();
    }

    public Date getContentUpdateDate() {
        return doc.getContentUpdateDate();
    }

    public Date getCreationDate() {
        return doc.getCreationDate();
    }

    public String getParent() {
        return doc.getParent();
    }

    public String getCreator() {
        return doc.getCreator();
    }

    public String getContent() {
        return doc.getContent();
    }

    public String getLanguage() {
        return doc.getLanguage();
    }

    public String getTemplate() {
        return doc.getTemplate();
    }

    public String getRealLanguage() throws XWikiException {
        return doc.getRealLanguage(context);
    }

    public String getDefaultLanguage() {
        return doc.getDefaultLanguage();
    }
    
    public String getDefaultTemplate() {
        return doc.getDefaultTemplate();
    }

    public List getTranslationList() throws XWikiException {
        return doc.getTranslationList(context);
    }

    public String getTranslatedContent() throws XWikiException {
        return doc.getTranslatedContent(context);
    }

    public String getTranslatedContent(String language) throws XWikiException {
        return doc.getTranslatedContent(language, context);
    }

    public Document getTranslatedDocument(String language) throws XWikiException {
        return new Document(doc.getTranslatedDocument(language, context), context);
    }

    public Document getTranslatedDocument() throws XWikiException {
        return new Document(doc.getTranslatedDocument(context), context);
    }

    public String getRenderedContent() throws XWikiException {
        return doc.getRenderedContent(context);
    }

    public String getRenderedContent(String text) {
        return doc.getRenderedContent(text, context);
    }

    public String getEscapedContent() throws XWikiException {
        return doc.getEscapedContent(context);
    }

    public Archive getRCSArchive() {
        return doc.getRCSArchive();
    }

    public String getArchive() throws XWikiException {
        return doc.getArchive();
    }

    public boolean isNew() {
        return doc.isNew();
    }

    public String getAttachmentURL(String filename) {
        return doc.getAttachmentURL(filename, "download", context);
    }

    public String getAttachmentURL(String filename, String action) {
        return doc.getAttachmentURL(filename, action, context);
    }

    public String getAttachmentURL(String filename, String action, String querystring) {
        return doc.getAttachmentURL(filename, action, querystring, context);
    }

    public String getAttachmentRevisionURL(String filename, String version) {
        return doc.getAttachmentRevisionURL(filename, version, context);
    }

    public String getAttachmentRevisionURL(String filename, String version, String querystring) {
        return doc.getAttachmentRevisionURL(filename, version, querystring, context);
    }

    public String getURL() {
        return doc.getURL("view", context);
    }

    public String getURL(String action) {
        return doc.getURL(action, context);
    }

    public String getURL(String action, String querystring) {
        return doc.getURL(action, querystring, context);
    }

    public String getExternalURL() {
        return doc.getExternalURL("view", context);
    }

    public String getExternalURL(String action) {
        return doc.getExternalURL(action, context);
    }

    public String getExternalURL(String action, String querystring) {
        return doc.getExternalURL(action, querystring, context);
    }

    public String getParentURL() throws XWikiException {
        return doc.getParentURL(context);
    }

    public Class getxWikiClass() {
        BaseClass bclass = doc.getxWikiClass();
        if (bclass==null)
            return null;
        else
            return new Class(bclass, context);
    }


    public Class[] getxWikiClasses() {
        List list = doc.getxWikiClasses(context);
        if (list==null)
            return null;
        Class[] result = new Class[list.size()];
        for (int i=0;i<list.size();i++)
            result[i] = new Class((BaseClass) list.get(i), context);
        return result;
    }

    public int createNewObject(String classname) throws XWikiException {
       return doc.createNewObject(classname, context);
    }

    public Object newObject(String classname) throws XWikiException {
       int nb = createNewObject(classname);
       return getObject(classname, nb);
    }

    public boolean isFromCache() {
        return doc.isFromCache();
    }

    public int getObjectNumbers(String classname) {
        return doc.getObjectNumbers(classname);
    }


    public Map getxWikiObjects() {
        Map map = doc.getxWikiObjects();
        Map resultmap = new HashMap();
        for (Iterator it = map.keySet().iterator();it.hasNext();) {
            String name = (String) it.next();
            Vector objects = (Vector)map.get(name);
            if (objects!=null)
                resultmap.put(name, getObjects(objects));
        }
        return resultmap;
    }

    protected Vector getObjects(Vector objects) {
        Vector result = new Vector();
        if (objects==null)
            return result;
        for (int i=0;i<objects.size();i++) {
            BaseObject bobj = (BaseObject) objects.get(i);
            if (bobj!=null) {
                result.add(newObjectApi(bobj, context));
            }
        }
        return result;
    }

    public Vector getObjects(String classname) {
        Vector objects = doc.getObjects(classname);
        return getObjects(objects);
    }

    public Object getFirstObject(String fieldname) {
        try {
            BaseObject obj = doc.getFirstObject(fieldname, context);
            if (obj==null)
                return null;
            else
                return newObjectApi(obj, context);
        } catch (Exception e) {
            return null;
        }
    }

    public Object getObject(String classname, String key, String value, boolean failover) {
        try {
            BaseObject obj = doc.getObject(classname, key, value, failover);
            if (obj==null)
                return null;
            else
                return newObjectApi(obj, context);
        } catch (Exception e) {
            return null;
        }
    }

    public Object getObject(String classname, String key, String value) {
        try {
            BaseObject obj = doc.getObject(classname, key, value);
            if (obj==null)
                return null;
            else
                return newObjectApi(obj, context);
        } catch (Exception e) {
            return null;
        }
    }

    public Object getObject(String classname) {
        return getObject(classname, false);
    }

    public Object getObject(String classname, boolean create) {
        try {
            BaseObject obj = doc.getObject(classname);

            if ((obj==null)&&create) {
              return newObject(classname);
            }

            if (obj==null)
                return null;
            else
                return newObjectApi(obj, context);
        } catch (Exception e) {
            return null;
        }
    }

    public Object getObject(String classname, int nb) {
        try {
            BaseObject obj = doc.getObject(classname, nb);
            if (obj==null)
                return null;
            else
                return newObjectApi(obj, context);
        } catch (Exception e) {
            return null;
        }
    }

    private Object newObjectApi(BaseObject obj, XWikiContext context) {
        return obj.newObjectApi(obj, context);
    }

    public String getXMLContent() throws XWikiException {
        String xml = doc.getXMLContent(context);
        return context.getUtil().substitute("s/<password>.*?<\\/password>/<password>********<\\/password>/goi", xml);
    }
    
    public String toXML() throws XWikiException {
        if (checkProgrammingRights())
            return doc.toXML(context);
        else
            return "";
    }

    public org.dom4j.Document toXMLDocument() throws XWikiException {
        if (checkProgrammingRights())
            return doc.toXMLDocument(context);
        else return null;
    }

    public Version[] getRevisions() throws XWikiException {
        return doc.getRevisions(context);
    }

    public String[] getRecentRevisions() throws XWikiException {
        return doc.getRecentRevisions(5, context);
    }

    public String[] getRecentRevisions(int nb) throws XWikiException {
        return doc.getRecentRevisions(nb, context);
    }

    public List getAttachmentList() {
        List list = doc.getAttachmentList();
        List list2 = new ArrayList();
        for (int i=0;i<list.size();i++) {
            list2.add(new Attachment(this, (XWikiAttachment)list.get(i), context));
        }
        return list2;
    }

    public Vector getComments() {
        return getComments(true);
    }

    public Vector getComments(boolean asc) {
        if (asc)
            return getObjects("XWiki.XWikiComments");
        else {
            Vector list = getObjects("XWiki.XWikiComments");
            if (list==null)
                return list;
            Vector newlist = new Vector();
            for (int i=list.size()-1;i>=0;i--) {
                newlist.add(list.get(i));
            }
            return newlist;
        }
    }

    public void use(Object object) {
        currentObj = object;
    }

    public void use(String className) {
        currentObj = getObject(className);
    }

    public void use(String className, int nb) {
        currentObj = getObject(className, nb);
    }

    public String display(String fieldname) {
        if (currentObj==null)
         return doc.display(fieldname, context);
        else
         return doc.display(fieldname, currentObj.getBaseObject(), context);
    }

    public String display(String fieldname, String mode) {
        if (currentObj==null)
         return doc.display(fieldname, mode, context);
        else
         return doc.display(fieldname, mode, currentObj.getBaseObject(), context);
    }

    public String display(String fieldname, Object obj) {
        if (obj==null)
            return "";
        return doc.display(fieldname, obj.getBaseObject(), context);
    }

    public String display(String fieldname, String mode, Object obj) {
        if (obj==null)
            return "";
        return doc.display(fieldname, mode, obj.getBaseObject(), context);
    }

    public String displayForm(String className,String header, String format) {
        return doc.displayForm(className, header, format, context);
    }

    public String displayForm(String className,String header, String format, boolean linebreak) {
        return doc.displayForm(className, header, format, linebreak, context);
    }

    public String displayForm(String className) {
        return doc.displayForm(className, context);
    }

    public String displayRendered(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object) {
        if ((pclass==null)||(object==null))
            return "";
        return doc.displayRendered(pclass.getBasePropertyClass(), prefix, object.getCollection(), context);
    }

    public String displayView(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object) {
        if ((pclass==null)||(object==null))
            return "";
        return doc.displayView(pclass.getBasePropertyClass(), prefix, object.getCollection(), context);
    }

    public String displayEdit(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object) {
        if ((pclass==null)||(object==null))
            return "";
        return doc.displayEdit(pclass.getBasePropertyClass(), prefix, object.getCollection(), context);
    }

    public String displayHidden(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object) {
        if ((pclass==null)||(object==null))
            return "";
        return doc.displayHidden(pclass.getBasePropertyClass(), prefix, object.getCollection(), context);
    }

    public String displaySearch(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object) {
        if ((pclass==null)||(object==null))
            return "";
        return doc.displaySearch(pclass.getBasePropertyClass(), prefix, object.getCollection(), context);
    }

    public List getIncludedPages() {
        return doc.getIncludedPages(context);
    }

    public List getIncludedMacros() {
        return doc.getIncludedMacros(context);
    }

    public List getLinkedPages() {
        return doc.getLinkedPages(context);
    }

    public Attachment getAttachment(String filename) {
        XWikiAttachment attach = doc.getAttachment(filename);
        if (attach==null)
            return null;
        else
            return new Attachment(this, attach, context);
    }

    public List getContentDiff(Document origdoc, Document newdoc) throws XWikiException, DifferentiationFailedException {
        try {
            if ((origdoc==null)&&(newdoc==null))
                return new ArrayList();
            if (origdoc==null)
                return doc.getContentDiff(new XWikiDocument(newdoc.getWeb(), newdoc.getName()), newdoc.getDoc(), context);
            if (newdoc==null)
                return doc.getContentDiff(origdoc.getDoc(), new XWikiDocument(origdoc.getWeb(), origdoc.getName()), context);

            return doc.getContentDiff(origdoc.getDoc(), newdoc.getDoc(), context);
        } catch (Exception e) {
            java.lang.Object[] args = { origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion() };
            List list = new ArrayList();
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_CONTENT_ERROR,
                    "Error while making content diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, context);
            list.add(errormsg);
            return list;
        }
    }

    public List getXMLDiff(Document origdoc, Document newdoc) throws XWikiException, DifferentiationFailedException {
        try {
            if ((origdoc==null)&&(newdoc==null))
                return new ArrayList();
            if (origdoc==null)
                return doc.getXMLDiff(new XWikiDocument(newdoc.getWeb(), newdoc.getName()), newdoc.getDoc(), context);
            if (newdoc==null)
                return doc.getXMLDiff(origdoc.getDoc(), new XWikiDocument(origdoc.getWeb(), origdoc.getName()), context);

            return doc.getXMLDiff(origdoc.getDoc(), newdoc.getDoc(), context);
        } catch (Exception e) {
            java.lang.Object[] args = { origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion() };
            List list = new ArrayList();
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_XML_ERROR,
                    "Error while making xml diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, context);
            list.add(errormsg);
            return list;
        }
    }

    public List getRenderedContentDiff(Document origdoc, Document newdoc) throws XWikiException, DifferentiationFailedException {
        try {
            if ((origdoc==null)&&(newdoc==null))
                return new ArrayList();
            if (origdoc==null)
                return doc.getRenderedContentDiff(new XWikiDocument(newdoc.getWeb(), newdoc.getName()), newdoc.getDoc(), context);
            if (newdoc==null)
                return doc.getRenderedContentDiff(origdoc.getDoc(), new XWikiDocument(origdoc.getWeb(), origdoc.getName()), context);

            return doc.getRenderedContentDiff(origdoc.getDoc(), newdoc.getDoc(), context);
        } catch (Exception e) {
            java.lang.Object[] args = { origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion() };
            List list = new ArrayList();
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_RENDERED_ERROR,
                    "Error while making rendered diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, context);
            list.add(errormsg);
            return list;
        }
    }

    public List getMetaDataDiff(Document origdoc, Document newdoc) throws XWikiException {
        try {
            if ((origdoc==null)&&(newdoc==null))
                return new ArrayList();
            if (origdoc==null)
                return doc.getMetaDataDiff(new XWikiDocument(newdoc.getWeb(), newdoc.getName()), newdoc.getDoc(), context);
            if (newdoc==null)
                return doc.getMetaDataDiff(origdoc.getDoc(), new XWikiDocument(origdoc.getWeb(), origdoc.getName()), context);

            return doc.getMetaDataDiff(origdoc.getDoc(), newdoc.getDoc(), context);
        } catch (Exception e) {
            java.lang.Object[] args = { origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion() };
            List list = new ArrayList();
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_METADATA_ERROR,
                    "Error while making meta data diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, context);
            list.add(errormsg);
            return list;
        }
    }

    public List getObjectDiff(Document origdoc, Document newdoc) throws XWikiException {
        try {
            if ((origdoc==null)&&(newdoc==null))
                return new ArrayList();
            if (origdoc==null)
                return doc.getObjectDiff(new XWikiDocument(newdoc.getWeb(), newdoc.getName()), newdoc.getDoc(), context);
            if (newdoc==null)
                return doc.getObjectDiff(origdoc.getDoc(), new XWikiDocument(origdoc.getWeb(), origdoc.getName()), context);

            return doc.getObjectDiff(origdoc.getDoc(), newdoc.getDoc(), context);
        } catch (Exception e) {
            java.lang.Object[] args = { origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion() };
            List list = new ArrayList();
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_OBJECT_ERROR,
                    "Error while making meta object diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, context);
            list.add(errormsg);
            return list;
        }
    }

    public List getClassDiff(Document origdoc, Document newdoc) throws XWikiException {
        try {
            if ((origdoc==null)&&(newdoc==null))
                return new ArrayList();
            if (origdoc==null)
                return doc.getClassDiff(new XWikiDocument(newdoc.getWeb(), newdoc.getName()), newdoc.getDoc(), context);
            if (newdoc==null)
                return doc.getClassDiff(origdoc.getDoc(), new XWikiDocument(origdoc.getWeb(), origdoc.getName()), context);

            return doc.getClassDiff(origdoc.getDoc(), newdoc.getDoc(), context);
        } catch (Exception e) {
            java.lang.Object[] args = { origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion() };
            List list = new ArrayList();
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_CLASS_ERROR,
                    "Error while making class diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, context);
            list.add(errormsg);
            return list;
        }
    }

    public List getLastChanges() throws XWikiException, DifferentiationFailedException {
        return doc.getLastChanges(context);
    }

    public DocumentStats getCurrentMonthPageStats(String action) {
        return context.getWiki().getStatsService(context).getDocMonthStats(doc.getFullName(), action, new Date(), context);
    }

    public DocumentStats getCurrentMonthWebStats(String action) {
        return context.getWiki().getStatsService(context).getDocMonthStats(doc.getWeb(), action, new Date(), context);
    }

    public List getCurrentMonthRefStats() throws XWikiException {
        return context.getWiki().getStatsService(context).getRefMonthStats(doc.getFullName(), new Date(), context);
    }

    public boolean checkAccess(String right) {
        try {
            return context.getWiki().checkAccess(right, doc, context);
        } catch (XWikiException e) {
            return false;
        }
    }

    public boolean hasAccessLevel(String level) {
        try {
            return context.getWiki().getRightService().hasAccessLevel(level, context.getUser(), doc.getFullName(), context);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasAccessLevel(String level, String user) {
        try {
            return context.getWiki().getRightService().hasAccessLevel(level, user, doc.getFullName(), context);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean getLocked() {
        try{
            XWikiLock lock = doc.getLock(context);
            if (lock != null && !context.getUser().equals(lock.getUserName()))
                return true;
            else
                return false;
        } catch(Exception e) {
            return false;
        }
    }

    public String getLockingUser() {
        try {
            XWikiLock lock = doc.getLock(context);
            if (lock != null && !context.getUser().equals(lock.getUserName()))
                return lock.getUserName();
            else
                return "";
        } catch (XWikiException e) {
            return "";
        }
    }

    public Date getLockingDate() {
        try {
            XWikiLock lock = doc.getLock(context);
            if (lock != null && !context.getUser().equals(lock.getUserName()))
                return lock.getDate();
            else
                return null;
        } catch (XWikiException e) {
            return null;
        }
    }

    public java.lang.Object get(String classOrFieldName) {
        if (currentObj!=null)
            return doc.display(classOrFieldName, currentObj.getBaseObject(), context);
        BaseObject object = doc.getFirstObject(classOrFieldName, context);
        if (object!=null) {
            return doc.display(classOrFieldName, object, context);
        }
        return doc.getObject(classOrFieldName);
    }


    public String getTextArea() {
        return com.xpn.xwiki.XWiki.getTextArea(doc.getContent(), context);
    }


    /**
     * Returns data needed for a generation of Table of Content for this document.
     * @param init an intial level where the TOC generation should start at
     * @param max maximum level TOC is generated for
     * @param numbered if should generate numbering for headings
     * @return a map where an heading (title) ID is the key and 
     *          value is another map with two keys: text, level and numbering 
     */
    public Map getTOC(int init, int max, boolean numbered) {
        return TOCGenerator.generateTOC(getContent(), init, max, numbered, context);
    }

    public void saveDocument() throws XWikiException {
        if (hasAccessLevel("edit"))
            context.getWiki().saveDocument(doc, context);
    }

    public com.xpn.xwiki.api.Object addObjectFromRequest() throws XWikiException {
        if (hasAccessLevel("edit"))
            return new com.xpn.xwiki.api.Object(doc.addObjectFromRequest(context), context);
        else
            return null;
    }

    public void insertText(String text, String marker) throws XWikiException {
        if (hasAccessLevel("edit"))
            doc.insertText(text, marker, context);
    }

    public boolean equals(java.lang.Object arg0) {
    	if (!(arg0 instanceof Document)) return false;
    	Document d = (Document) arg0;
    	return d.context.equals(context) && doc.equals(d.doc);
    }

    public List getBacklinks () throws XWikiException {
        return doc.getBacklinks(context);
    }

    public List getLinks () throws XWikiException {
        return doc.getLinks(context);
    }

    public String getDefaultEditURL() throws XWikiException {
        return doc.getDefaultEditURL(context);
    }

    public String getEditURL(String action, String mode) throws XWikiException {
        return doc.getEditURL(action, mode, context);
    }

    public String getEditURL(String action, String mode, String language) {
        return doc.getEditURL(action, mode, language, context);
    }

    public boolean isCurrentUserCreator() {
        return doc.isCurrentUserCreator(context);
    }

    public boolean isCurrentUserPage() {
        return doc.isCurrentUserPage(context);
    }

    public boolean isCurrentLocalUserPage() {
        return doc.isCurrentLocalUserPage(context);
    }

    public boolean isCreator(String username) {
        return doc.isCreator(username);
    }

    public void set(String fieldname, java.lang.Object value) {
        Object obj;
        if (currentObj!=null)
         obj = currentObj;
        else
         obj = getFirstObject(fieldname);
        if (obj==null)
         return;

        obj.set(fieldname, value);
    }

    public void setTitle(String title) {
        doc.setTitle(title);
    }

    public void setParent(String parent) {
        doc.setParent(parent);
    }

    public void setContent(String content) {
        doc.setContent(content);
    }

    public void setDefaultTemplate(String dtemplate) {
        doc.setDefaultTemplate(dtemplate);
    }

    public void save() throws XWikiException {
        if (hasAccessLevel("edit"))
         context.getWiki().saveDocument(doc, olddoc, context);
        else {
            java.lang.Object[] args = { doc.getFullName() };
         throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                                  "Access denied in edit mode on document {0}", null, args);
        }
    }
}
