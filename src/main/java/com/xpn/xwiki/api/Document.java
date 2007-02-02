/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.util.TOCGenerator;
import com.xpn.xwiki.util.Util;
import org.apache.commons.fileupload.DefaultFileItem;
import org.apache.commons.io.CopyUtils;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.rcs.Version;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class Document extends Api
{
    protected XWikiDocument olddoc;

    protected XWikiDocument doc;

    protected Object currentObj;

    public Document(XWikiDocument doc, XWikiContext context)
    {
        super(context);
        this.olddoc = doc;
        this.doc = doc;
    }

    /**
     * this function is accessible only if you have the programming rights give access to the
     * priviledged API of the Document
     */
    public XWikiDocument getDocument()
    {
        if (hasProgrammingRights()) {
            return doc;
        } else {
            return null;
        }
    }

    protected XWikiDocument getDoc()
    {
        if (doc == olddoc) {
            doc = (XWikiDocument) doc.clone();
        }
        return doc;
    }

    /**
     * return the ID of the document. this ID is uniq accross the wiki.
     *
     * @return the id of the document
     */
    public long getId()
    {
        return doc.getId();
    }

    /**
     * return the name of a document.
     *
     * for exemple if the fullName of a document is "MySpace.Mydoc", the name is MyDoc
     *
     * @return the name of the document
     */
    public String getName()
    {
        return doc.getName();
    }

    /**
     * return the name of the space of the document
     *
     * for exemple if the fullName of a document is "MySpace.Mydoc", the name is MySpace
     *
     * @return the name of the space of the document
     */
    public String getSpace()
    {
        return doc.getSpace();
    }

    /**
     * return the name of the space of the document
     *
     * for exemple if the fullName of a document is "MySpace.Mydoc", the name is MySpace
     *
     * @deprecated use {@link #getSpace()} instead of this function
     */
    public String getWeb()
    {
        return doc.getSpace();
    }

    /**
     * return the fullName of a doucment
     *
     * if a document has for name "MyDoc" and space "MySpace", the fullname is "MySpace.MyDoc" In a
     * wiki, all the documents have a different fullName.
     */
    public String getFullName()
    {
        return doc.getFullName();
    }

    public Version getRCSVersion()
    {
        return doc.getRCSVersion();
    }

    /**
     * return a String with the version of the document.
     */
    public String getVersion()
    {
        return doc.getVersion();
    }

    /**
     * return the title of a document
     */
    public String getTitle()
    {
        return doc.getTitle();
    }

    /**
     * return the title of the document. If there is no title, return the first title in the
     * document
     */
    public String getDisplayTitle()
    {
        return doc.getDisplayTitle();
    }

    public String getFormat()
    {
        return doc.getFormat();
    }

    /**
     * return the name of the last author of the document
     */
    public String getAuthor()
    {
        return doc.getAuthor();
    }

    /**
     * return the name of the last author of the content of the document
     */
    public String getContentAuthor()
    {
        return doc.getContentAuthor();
    }

    /**
     * return the modification date
     */
    public Date getDate()
    {
        return doc.getDate();
    }

    /**
     * return the date of the last modification of the content
     */
    public Date getContentUpdateDate()
    {
        return doc.getContentUpdateDate();
    }

    /**
     * return the date of the creation of the document
     */
    public Date getCreationDate()
    {
        return doc.getCreationDate();
    }

    /**
     * return the name of the parent document
     */
    public String getParent()
    {
        return doc.getParent();
    }

    /**
     * return the name of the creator of the document
     */
    public String getCreator()
    {
        return doc.getCreator();
    }

    /**
     * return the content of the document
     */
    public String getContent()
    {
        return doc.getContent();
    }

    /**
     * return the language of the document if it's a traduction, otherwise, it return default
     */
    public String getLanguage()
    {
        return doc.getLanguage();
    }

    public String getTemplate()
    {
        return doc.getTemplate();
    }

    /**
     * return the real language of the document
     */
    public String getRealLanguage() throws XWikiException
    {
        return doc.getRealLanguage(getContext());
    }

    /**
     * return the language of the default document
     */
    public String getDefaultLanguage()
    {
        return doc.getDefaultLanguage();
    }

    public String getDefaultTemplate()
    {
        return doc.getDefaultTemplate();
    }

    /**
     * return the list of possible traduction for this document
     */
    public List getTranslationList() throws XWikiException
    {
        return doc.getTranslationList(getContext());
    }

    /**
     * return the tranlated document's content if the wiki is multilingual, the language is first
     * checked in the URL, the cookie, the user profile and finally the wiki configuration if not,
     * the language is the one on the wiki configuration
     */
    public String getTranslatedContent() throws XWikiException
    {
        return doc.getTranslatedContent(getContext());
    }

    /**
     * return the translated content in the given language
     */
    public String getTranslatedContent(String language) throws XWikiException
    {
        return doc.getTranslatedContent(language, getContext());
    }

    /**
     * return the translated document in the given document
     */
    public Document getTranslatedDocument(String language) throws XWikiException
    {
        return doc.getTranslatedDocument(language, getContext()).newDocument(getContext());
    }

    /**
     * return the tranlated Document if the wiki is multilingual, the language is first checked in
     * the URL, the cookie, the user profile and finally the wiki configuration if not, the language
     * is the one on the wiki configuration
     */
    public Document getTranslatedDocument() throws XWikiException
    {
        return doc.getTranslatedDocument(getContext()).newDocument(getContext());
    }

    /**
     * return the content of the document rendererd
     */
    public String getRenderedContent() throws XWikiException
    {
        return doc.getRenderedContent(getContext());
    }

    /**
     * return the given text rendered in the context of this document
     */
    public String getRenderedContent(String text) throws XWikiException
    {
        return doc.getRenderedContent(text, getContext());
    }

    /**
     * return a escaped version of the content of this document
     */
    public String getEscapedContent() throws XWikiException
    {
        return doc.getEscapedContent(getContext());
    }

    /**
     * return the archive of the document in a string format
     */
    public String getArchive() throws XWikiException
    {
        return doc.getDocumentArchive(getContext()).getArchive();
    }

    /**
     * this function is accessible only if you have the programming rights return the archive of the
     * document
     */
    public XWikiDocumentArchive getDocumentArchive() throws XWikiException
    {
        if (hasProgrammingRights()) {
            return doc.getDocumentArchive(getContext());
        }
        return null;
    }

    /**
     * @return true if the document is a new one (ie it has never been saved) or false otherwise
     */
    public boolean isNew()
    {
        return doc.isNew();
    }

    /**
     * return the URL of download for the  the given attachment name
     *
     * @param filename the name of the attachment
     * @return A String with the URL
     */
    public String getAttachmentURL(String filename)
    {
        return doc.getAttachmentURL(filename, "download", getContext());
    }

    /**
     * return the URL of the given action for the  the given attachment name
     *
     * @return A string with the URL
     */
    public String getAttachmentURL(String filename, String action)
    {
        return doc.getAttachmentURL(filename, action, getContext());
    }

    /**
     * return the URL of the given action for the  the given attachment name with "queryString"
     * parameters
     *
     * @param queryString parameters added to the URL
     */
    public String getAttachmentURL(String filename, String action, String queryString)
    {
        return doc.getAttachmentURL(filename, action, queryString, getContext());
    }

    /**
     * return the URL for accessing to the archive of the attachment "filename" at the version
     * "version"
     */
    public String getAttachmentRevisionURL(String filename, String version)
    {
        return doc.getAttachmentRevisionURL(filename, version, getContext());
    }

    /**
     * return the URL for accessing to the archive of the attachment "filename" at the version
     * "version" and  with the given queryString parameters
     */
    public String getAttachmentRevisionURL(String filename, String version, String querystring)
    {
        return doc.getAttachmentRevisionURL(filename, version, querystring, getContext());
    }

    /**
     * return the URL of this document in view mode
     */
    public String getURL()
    {
        return doc.getURL("view", getContext());
    }

    /**
     * return thr URL of this document with the given action
     */
    public String getURL(String action)
    {
        return doc.getURL(action, getContext());
    }

    /**
     * return thr URL of this document with the given action and queryString as parameters
     */
    public String getURL(String action, String querystring)
    {
        return doc.getURL(action, querystring, getContext());
    }

    /**
     * return the full URL of the document
     */
    public String getExternalURL()
    {
        return doc.getExternalURL("view", getContext());
    }

    /**
     * return the full URL of the document for the given action
     */
    public String getExternalURL(String action)
    {
        return doc.getExternalURL(action, getContext());
    }

    public String getExternalURL(String action, String querystring)
    {
        return doc.getExternalURL(action, querystring, getContext());
    }

    public String getParentURL() throws XWikiException
    {
        return doc.getParentURL(getContext());
    }

    public Class getxWikiClass()
    {
        BaseClass bclass = getDoc().getxWikiClass();
        if (bclass == null) {
            return null;
        } else {
            return new Class(bclass, getContext());
        }
    }

    public Class[] getxWikiClasses()
    {
        List list = getDoc().getxWikiClasses(getContext());
        if (list == null) {
            return null;
        }
        Class[] result = new Class[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = new Class((BaseClass) list.get(i), getContext());
        }
        return result;
    }

    public int createNewObject(String classname) throws XWikiException
    {
        return getDoc().createNewObject(classname, getContext());
    }

    public Object newObject(String classname) throws XWikiException
    {
        int nb = createNewObject(classname);
        return getObject(classname, nb);
    }

    public boolean isFromCache()
    {
        return doc.isFromCache();
    }

    public int getObjectNumbers(String classname)
    {
        return getDoc().getObjectNumbers(classname);
    }

    public Map getxWikiObjects()
    {
        Map map = getDoc().getxWikiObjects();
        Map resultmap = new HashMap();
        for (Iterator it = map.keySet().iterator(); it.hasNext();) {
            String name = (String) it.next();
            Vector objects = (Vector) map.get(name);
            if (objects != null) {
                resultmap.put(name, getObjects(objects));
            }
        }
        return resultmap;
    }

    protected Vector getObjects(Vector objects)
    {
        Vector result = new Vector();
        if (objects == null) {
            return result;
        }
        for (int i = 0; i < objects.size(); i++) {
            BaseObject bobj = (BaseObject) objects.get(i);
            if (bobj != null) {
                result.add(newObjectApi(bobj, getContext()));
            }
        }
        return result;
    }

    public Vector getObjects(String classname)
    {
        Vector objects = getDoc().getObjects(classname);
        return getObjects(objects);
    }

    public Object getFirstObject(String fieldname)
    {
        try {
            BaseObject obj = getDoc().getFirstObject(fieldname, getContext());
            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Object getObject(String classname, String key, String value, boolean failover)
    {
        try {
            BaseObject obj = getDoc().getObject(classname, key, value, failover);
            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Object getObject(String classname, String key, String value)
    {
        try {
            BaseObject obj = getDoc().getObject(classname, key, value);
            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Object getObject(String classname)
    {
        return getObject(classname, false);
    }

    /**
     * get the object of the given className. If there is no object of this className and the create
     * parameter at true, the object is created.
     */
    public Object getObject(String classname, boolean create)
    {
        try {
            BaseObject obj = getDoc().getObject(classname, create, getContext());

            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Object getObject(String classname, int nb)
    {
        try {
            BaseObject obj = getDoc().getObject(classname, nb);
            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Object newObjectApi(BaseObject obj, XWikiContext context)
    {
        return obj.newObjectApi(obj, context);
    }

    public String getXMLContent() throws XWikiException
    {
        String xml = doc.getXMLContent(getContext());
        return getContext().getUtil()
            .substitute("s/<password>.*?<\\/password>/<password>********<\\/password>/goi", xml);
    }

    public String toXML() throws XWikiException
    {
        if (hasProgrammingRights()) {
            return doc.toXML(getContext());
        } else {
            return "";
        }
    }

    public org.dom4j.Document toXMLDocument() throws XWikiException
    {
        if (hasProgrammingRights()) {
            return doc.toXMLDocument(getContext());
        } else {
            return null;
        }
    }

    public Version[] getRevisions() throws XWikiException
    {
        return doc.getRevisions(getContext());
    }

    public String[] getRecentRevisions() throws XWikiException
    {
        return doc.getRecentRevisions(5, getContext());
    }

    public String[] getRecentRevisions(int nb) throws XWikiException
    {
        return doc.getRecentRevisions(nb, getContext());
    }

    public List getAttachmentList()
    {
        List list = getDoc().getAttachmentList();
        List list2 = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            list2.add(new Attachment(this, (XWikiAttachment) list.get(i), getContext()));
        }
        return list2;
    }

    public Vector getComments()
    {
        return getComments(true);
    }

    public Vector getComments(boolean asc)
    {
        if (asc) {
            return getObjects("XWiki.XWikiComments");
        } else {
            Vector list = getObjects("XWiki.XWikiComments");
            if (list == null) {
                return list;
            }
            Vector newlist = new Vector();
            for (int i = list.size() - 1; i >= 0; i--) {
                newlist.add(list.get(i));
            }
            return newlist;
        }
    }

    public void use(Object object)
    {
        currentObj = object;
    }

    public void use(String className)
    {
        currentObj = getObject(className);
    }

    public void use(String className, int nb)
    {
        currentObj = getObject(className, nb);
    }

    public String getActiveClass()
    {
        if (currentObj == null) {
            return null;
        } else {
            return currentObj.getName();
        }
    }

    public String displayPrettyName(String fieldname)
    {
        if (currentObj == null) {
            return doc.displayPrettyName(fieldname, getContext());
        } else {
            return doc.displayPrettyName(fieldname, currentObj.getBaseObject(), getContext());
        }
    }

    public String displayPrettyName(String fieldname, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return doc.displayPrettyName(fieldname, obj.getBaseObject(), getContext());
    }

    public String displayPrettyName(String fieldname, boolean showMandatory)
    {
        if (currentObj == null) {
            return doc.displayPrettyName(fieldname, showMandatory, getContext());
        } else {
            return doc.displayPrettyName(fieldname, showMandatory, currentObj.getBaseObject(),
                getContext());
        }
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return doc.displayPrettyName(fieldname, showMandatory, obj.getBaseObject(), getContext());
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, boolean before)
    {
        if (currentObj == null) {
            return doc.displayPrettyName(fieldname, showMandatory, before, getContext());
        } else {
            return doc.displayPrettyName(fieldname, showMandatory, before,
                currentObj.getBaseObject(), getContext());
        }
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, boolean before,
        Object obj)
    {
        if (obj == null) {
            return "";
        }
        return doc
            .displayPrettyName(fieldname, showMandatory, before, obj.getBaseObject(), getContext());
    }

    public String displayTooltip(String fieldname)
    {
        if (currentObj == null) {
            return doc.displayTooltip(fieldname, getContext());
        } else {
            return doc.displayTooltip(fieldname, currentObj.getBaseObject(), getContext());
        }
    }

    public String displayTooltip(String fieldname, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return doc.displayTooltip(fieldname, obj.getBaseObject(), getContext());
    }

    public String display(String fieldname)
    {
        if (currentObj == null) {
            return doc.display(fieldname, getContext());
        } else {
            return doc.display(fieldname, currentObj.getBaseObject(), getContext());
        }
    }

    public String display(String fieldname, String mode)
    {
        if (currentObj == null) {
            return doc.display(fieldname, mode, getContext());
        } else {
            return doc.display(fieldname, mode, currentObj.getBaseObject(), getContext());
        }
    }

    public String display(String fieldname, String mode, String prefix)
    {
        if (currentObj == null) {
            return doc.display(fieldname, mode, prefix, getContext());
        } else {
            return doc.display(fieldname, mode, prefix, currentObj.getBaseObject(), getContext());
        }
    }

    public String display(String fieldname, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return doc.display(fieldname, obj.getBaseObject(), getContext());
    }

    public String display(String fieldname, String mode, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return doc.display(fieldname, mode, obj.getBaseObject(), getContext());
    }

    public String display(String fieldname, String mode, String prefix, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return doc.display(fieldname, mode, prefix, obj.getBaseObject(), getContext());
    }

    public String displayForm(String className, String header, String format)
    {
        return doc.displayForm(className, header, format, getContext());
    }

    public String displayForm(String className, String header, String format, boolean linebreak)
    {
        return doc.displayForm(className, header, format, linebreak, getContext());
    }

    public String displayForm(String className)
    {
        return doc.displayForm(className, getContext());
    }

    public String displayRendered(com.xpn.xwiki.api.PropertyClass pclass, String prefix,
        Collection object) throws XWikiException
    {
        if ((pclass == null) || (object == null)) {
            return "";
        }
        return doc.displayRendered(pclass.getBasePropertyClass(), prefix, object.getCollection(),
            getContext());
    }

    public String displayView(com.xpn.xwiki.api.PropertyClass pclass, String prefix,
        Collection object)
    {
        if ((pclass == null) || (object == null)) {
            return "";
        }
        return doc.displayView(pclass.getBasePropertyClass(), prefix, object.getCollection(),
            getContext());
    }

    public String displayEdit(com.xpn.xwiki.api.PropertyClass pclass, String prefix,
        Collection object)
    {
        if ((pclass == null) || (object == null)) {
            return "";
        }
        return doc.displayEdit(pclass.getBasePropertyClass(), prefix, object.getCollection(),
            getContext());
    }

    public String displayHidden(com.xpn.xwiki.api.PropertyClass pclass, String prefix,
        Collection object)
    {
        if ((pclass == null) || (object == null)) {
            return "";
        }
        return doc.displayHidden(pclass.getBasePropertyClass(), prefix, object.getCollection(),
            getContext());
    }

    public List getIncludedPages()
    {
        return doc.getIncludedPages(getContext());
    }

    public List getIncludedMacros()
    {
        return doc.getIncludedMacros(getContext());
    }

    public List getLinkedPages()
    {
        return doc.getLinkedPages(getContext());
    }

    public Attachment getAttachment(String filename)
    {
        XWikiAttachment attach = getDoc().getAttachment(filename);
        if (attach == null) {
            return null;
        } else {
            return new Attachment(this, attach, getContext());
        }
    }

    public List getContentDiff(Document origdoc, Document newdoc)
        throws XWikiException, DifferentiationFailedException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return new ArrayList();
            }
            if (origdoc == null) {
                return doc.getContentDiff(new XWikiDocument(newdoc.getSpace(), newdoc.getName()),
                    newdoc.getDoc(), getContext());
            }
            if (newdoc == null) {
                return doc.getContentDiff(origdoc.getDoc(),
                    new XWikiDocument(origdoc.getSpace(), origdoc.getName()), getContext());
            }

            return doc.getContentDiff(origdoc.getDoc(), newdoc.getDoc(), getContext());
        } catch (Exception e) {
            java.lang.Object[] args =
                {origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion()};
            List list = new ArrayList();
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_DIFF,
                XWikiException.ERROR_XWIKI_DIFF_CONTENT_ERROR,
                "Error while making content diff of {0} between version {1} and version {2}", e,
                args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getContext());
            list.add(errormsg);
            return list;
        }
    }

    public List getXMLDiff(Document origdoc, Document newdoc)
        throws XWikiException, DifferentiationFailedException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return new ArrayList();
            }
            if (origdoc == null) {
                return doc.getXMLDiff(new XWikiDocument(newdoc.getSpace(), newdoc.getName()),
                    newdoc.getDoc(), getContext());
            }
            if (newdoc == null) {
                return doc.getXMLDiff(origdoc.getDoc(),
                    new XWikiDocument(origdoc.getSpace(), origdoc.getName()), getContext());
            }

            return doc.getXMLDiff(origdoc.getDoc(), newdoc.getDoc(), getContext());
        } catch (Exception e) {
            java.lang.Object[] args =
                {origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion()};
            List list = new ArrayList();
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_DIFF,
                XWikiException.ERROR_XWIKI_DIFF_XML_ERROR,
                "Error while making xml diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getContext());
            list.add(errormsg);
            return list;
        }
    }

    public List getRenderedContentDiff(Document origdoc, Document newdoc)
        throws XWikiException, DifferentiationFailedException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return new ArrayList();
            }
            if (origdoc == null) {
                return doc.getRenderedContentDiff(
                    new XWikiDocument(newdoc.getSpace(), newdoc.getName()), newdoc.getDoc(),
                    getContext());
            }
            if (newdoc == null) {
                return doc.getRenderedContentDiff(origdoc.getDoc(),
                    new XWikiDocument(origdoc.getSpace(), origdoc.getName()), getContext());
            }

            return doc.getRenderedContentDiff(origdoc.getDoc(), newdoc.getDoc(), getContext());
        } catch (Exception e) {
            java.lang.Object[] args =
                {origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion()};
            List list = new ArrayList();
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_DIFF,
                XWikiException.ERROR_XWIKI_DIFF_RENDERED_ERROR,
                "Error while making rendered diff of {0} between version {1} and version {2}", e,
                args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getContext());
            list.add(errormsg);
            return list;
        }
    }

    public List getMetaDataDiff(Document origdoc, Document newdoc) throws XWikiException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return new ArrayList();
            }
            if (origdoc == null) {
                return doc.getMetaDataDiff(new XWikiDocument(newdoc.getSpace(), newdoc.getName()),
                    newdoc.getDoc(), getContext());
            }
            if (newdoc == null) {
                return doc.getMetaDataDiff(origdoc.getDoc(),
                    new XWikiDocument(origdoc.getSpace(), origdoc.getName()), getContext());
            }

            return doc.getMetaDataDiff(origdoc.getDoc(), newdoc.getDoc(), getContext());
        } catch (Exception e) {
            java.lang.Object[] args =
                {origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion()};
            List list = new ArrayList();
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_DIFF,
                XWikiException.ERROR_XWIKI_DIFF_METADATA_ERROR,
                "Error while making meta data diff of {0} between version {1} and version {2}", e,
                args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getContext());
            list.add(errormsg);
            return list;
        }
    }

    public List getObjectDiff(Document origdoc, Document newdoc) throws XWikiException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return new ArrayList();
            }
            if (origdoc == null) {
                return getDoc().getObjectDiff(
                    new XWikiDocument(newdoc.getSpace(), newdoc.getName()), newdoc.getDoc(),
                    getContext());
            }
            if (newdoc == null) {
                return getDoc().getObjectDiff(origdoc.getDoc(),
                    new XWikiDocument(origdoc.getSpace(), origdoc.getName()), getContext());
            }

            return getDoc().getObjectDiff(origdoc.getDoc(), newdoc.getDoc(), getContext());
        } catch (Exception e) {
            java.lang.Object[] args =
                {origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion()};
            List list = new ArrayList();
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_DIFF,
                XWikiException.ERROR_XWIKI_DIFF_OBJECT_ERROR,
                "Error while making meta object diff of {0} between version {1} and version {2}", e,
                args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getContext());
            list.add(errormsg);
            return list;
        }
    }

    public List getClassDiff(Document origdoc, Document newdoc) throws XWikiException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return new ArrayList();
            }
            if (origdoc == null) {
                return doc.getClassDiff(new XWikiDocument(newdoc.getSpace(), newdoc.getName()),
                    newdoc.getDoc(), getContext());
            }
            if (newdoc == null) {
                return doc.getClassDiff(origdoc.getDoc(),
                    new XWikiDocument(origdoc.getSpace(), origdoc.getName()), getContext());
            }

            return doc.getClassDiff(origdoc.getDoc(), newdoc.getDoc(), getContext());
        } catch (Exception e) {
            java.lang.Object[] args =
                {origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion()};
            List list = new ArrayList();
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_DIFF,
                XWikiException.ERROR_XWIKI_DIFF_CLASS_ERROR,
                "Error while making class diff of {0} between version {1} and version {2}", e,
                args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getContext());
            list.add(errormsg);
            return list;
        }
    }

    public List getLastChanges() throws XWikiException, DifferentiationFailedException
    {
        return doc.getLastChanges(getContext());
    }

    public DocumentStats getCurrentMonthPageStats(String action)
    {
        return getContext().getWiki().getStatsService(getContext())
            .getDocMonthStats(doc.getFullName(), action, new Date(), getContext());
    }

    public DocumentStats getCurrentMonthWebStats(String action)
    {
        return getContext().getWiki().getStatsService(getContext())
            .getDocMonthStats(doc.getSpace(), action, new Date(), getContext());
    }

    public List getCurrentMonthRefStats() throws XWikiException
    {
        return getContext().getWiki().getStatsService(getContext())
            .getRefMonthStats(doc.getFullName(), new Date(), getContext());
    }

    public boolean checkAccess(String right)
    {
        try {
            return getContext().getWiki().checkAccess(right, getDoc(), getContext());
        } catch (XWikiException e) {
            return false;
        }
    }

    public boolean hasAccessLevel(String level)
    {
        try {
            return getContext().getWiki().getRightService().hasAccessLevel(level,
                getContext().getUser(), getDoc().getFullName(), getContext());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasAccessLevel(String level, String user)
    {
        try {
            return getContext().getWiki().getRightService()
                .hasAccessLevel(level, user, doc.getFullName(), getContext());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean getLocked()
    {
        try {
            XWikiLock lock = doc.getLock(getContext());
            if (lock != null && !getContext().getUser().equals(lock.getUserName())) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public String getLockingUser()
    {
        try {
            XWikiLock lock = doc.getLock(getContext());
            if (lock != null && !getContext().getUser().equals(lock.getUserName())) {
                return lock.getUserName();
            } else {
                return "";
            }
        } catch (XWikiException e) {
            return "";
        }
    }

    public Date getLockingDate()
    {
        try {
            XWikiLock lock = doc.getLock(getContext());
            if (lock != null && !getContext().getUser().equals(lock.getUserName())) {
                return lock.getDate();
            } else {
                return null;
            }
        } catch (XWikiException e) {
            return null;
        }
    }

    public java.lang.Object get(String classOrFieldName)
    {
        if (currentObj != null) {
            return getDoc().display(classOrFieldName, currentObj.getBaseObject(), getContext());
        }
        BaseObject object = getDoc().getFirstObject(classOrFieldName, getContext());
        if (object != null) {
            return getDoc().display(classOrFieldName, object, getContext());
        }
        return getDoc().getObject(classOrFieldName);
    }

    public java.lang.Object getValue(String fieldName)
    {
        Object object;
        if (currentObj == null) {
            object = new Object(getDoc().getFirstObject(fieldName, getContext()), getContext());
        } else {
            object = currentObj;
        }
        return getValue(fieldName, object);
    }

    public java.lang.Object getValue(String fieldName, Object object)
    {
        if (object != null) {
            try {
                return ((BaseProperty) object.getBaseObject().safeget(fieldName)).getValue();
            }
            catch (NullPointerException e) {
                return null;
            }
        }
        return null;
    }

    public String getTextArea()
    {
        return com.xpn.xwiki.XWiki.getTextArea(doc.getContent(), getContext());
    }

    /**
     * Returns data needed for a generation of Table of Content for this document.
     *
     * @param init an intial level where the TOC generation should start at
     * @param max maximum level TOC is generated for
     * @param numbered if should generate numbering for headings
     * @return a map where an heading (title) ID is the key and value is another map with two keys:
     *         text, level and numbering
     */
    public Map getTOC(int init, int max, boolean numbered)
    {
        return TOCGenerator.generateTOC(getContent(), init, max, numbered, getContext());
    }

    public String getTags()
    {
        return doc.getTags(getContext());
    }

    public List getTagList()
    {
        return doc.getTagsList(getContext());
    }

    public List getTagsPossibleValues()
    {
        return doc.getTagsPossibleValues(getContext());
    }

    public void insertText(String text, String marker) throws XWikiException
    {
        if (hasAccessLevel("edit")) {
            getDoc().insertText(text, marker, getContext());
        }
    }

    public boolean equals(java.lang.Object arg0)
    {
        if (!(arg0 instanceof Document)) {
            return false;
        }
        Document d = (Document) arg0;
        return d.getContext().equals(getContext()) && doc.equals(d.doc);
    }

    public List getBacklinks() throws XWikiException
    {
        return doc.getBacklinks(getContext());
    }

    public List getLinks() throws XWikiException
    {
        return doc.getLinks(getContext());
    }

    public String getDefaultEditURL() throws XWikiException
    {
        return doc.getDefaultEditURL(getContext());
    }

    public String getEditURL(String action, String mode) throws XWikiException
    {
        return doc.getEditURL(action, mode, getContext());
    }

    public String getEditURL(String action, String mode, String language)
    {
        return doc.getEditURL(action, mode, language, getContext());
    }

    public boolean isCurrentUserCreator()
    {
        return doc.isCurrentUserCreator(getContext());
    }

    public boolean isCurrentUserPage()
    {
        return doc.isCurrentUserPage(getContext());
    }

    public boolean isCurrentLocalUserPage()
    {
        return doc.isCurrentLocalUserPage(getContext());
    }

    public boolean isCreator(String username)
    {
        return doc.isCreator(username);
    }

    public void set(String fieldname, java.lang.Object value)
    {
        Object obj;
        if (currentObj != null) {
            obj = currentObj;
        } else {
            obj = getFirstObject(fieldname);
        }
        set(fieldname, value, obj);
    }

    public void set(String fieldname, java.lang.Object value, Object obj)
    {
        if (obj == null) {
            return;
        }
        obj.set(fieldname, value);
    }

    public void setTitle(String title)
    {
        getDoc().setTitle(title);
    }

    public void setCustomClass(String customClass)
    {
        getDoc().setCustomClass(customClass);
    }

    public void setParent(String parent)
    {
        getDoc().setParent(parent);
    }

    public void setContent(String content)
    {
        getDoc().setContent(content);
    }

    public void setDefaultTemplate(String dtemplate)
    {
        getDoc().setDefaultTemplate(dtemplate);
    }

    public void save() throws XWikiException
    {
        if (hasAccessLevel("edit")) {
            saveDocument();
        } else {
            java.lang.Object[] args = {getDoc().getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied in edit mode on document {0}", null, args);
        }
    }

    public void saveWithProgrammingRights() throws XWikiException
    {
        if (hasProgrammingRights()) {
            saveDocument();
        } else {
            java.lang.Object[] args = {getDoc().getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied with no programming rights document {0}", null, args);
        }
    }

    private void saveDocument() throws XWikiException
    {
        getContext().getWiki().saveDocument(getDoc(), olddoc, getContext());
        olddoc = doc;
    }

    public com.xpn.xwiki.api.Object addObjectFromRequest() throws XWikiException
    {
        // Call to getDoc() ensures that we are working on a clone()
        return new com.xpn.xwiki.api.Object(getDoc().addObjectFromRequest(getContext()),
            getContext());
    }

    public com.xpn.xwiki.api.Object addObjectFromRequest(String className) throws XWikiException
    {
        return new com.xpn.xwiki.api.Object(getDoc().addObjectFromRequest(className, getContext()),
            getContext());
    }

    public List addObjectsFromRequest(String className) throws XWikiException
    {
        return addObjectsFromRequest(className, "");
    }

    public com.xpn.xwiki.api.Object addObjectFromRequest(String className, String prefix)
        throws XWikiException
    {
        return new com.xpn.xwiki.api.Object(
            getDoc().addObjectFromRequest(className, prefix, getContext()), getContext());
    }

    public List addObjectsFromRequest(String className, String prefix) throws XWikiException
    {
        List objs = getDoc().addObjectsFromRequest(className, prefix, getContext());
        List wrapped = new ArrayList();
        Iterator it = objs.iterator();
        while (it.hasNext()) {
            wrapped.add(new com.xpn.xwiki.api.Object((BaseObject) it.next(), getContext()));
        }
        return wrapped;
    }

    public com.xpn.xwiki.api.Object updateObjectFromRequest(String className) throws XWikiException
    {
        return new com.xpn.xwiki.api.Object(
            getDoc().updateObjectFromRequest(className, getContext()), getContext());
    }

    public List updateObjectsFromRequest(String className) throws XWikiException
    {
        return updateObjectsFromRequest(className, "");
    }

    public com.xpn.xwiki.api.Object updateObjectFromRequest(String className, String prefix)
        throws XWikiException
    {
        return new com.xpn.xwiki.api.Object(
            getDoc().updateObjectFromRequest(className, prefix, getContext()), getContext());
    }

    public List updateObjectsFromRequest(String className, String prefix) throws XWikiException
    {
        List objs = getDoc().updateObjectsFromRequest(className, prefix, getContext());
        List wrapped = new ArrayList();
        Iterator it = objs.iterator();
        while (it.hasNext()) {
            wrapped.add(new com.xpn.xwiki.api.Object((BaseObject) it.next(), getContext()));
        }
        return wrapped;
    }

    public boolean isAdvancedContent()
    {
        return doc.isAdvancedContent();
    }

    public boolean isProgrammaticContent()
    {
        return doc.isProgrammaticContent();
    }

    public boolean removeObject(Object obj)
    {
        return getDoc().removeObject(obj.getBaseObject());
    }

    public boolean removeObjects(String className)
    {
        return getDoc().removeObjects(className);
    }

    public void delete() throws XWikiException
    {
        if (hasAccessLevel("delete")) {
            getContext().getWiki().deleteDocument(getDocument(), getContext());
        } else {
            java.lang.Object[] args = {doc.getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied in edit mode on document {0}", null, args);
        }
    }

    public void deleteWithProgrammingRights() throws XWikiException
    {
        if (hasProgrammingRights()) {
            getContext().getWiki().deleteDocument(getDocument(), getContext());
        } else {
            java.lang.Object[] args = {doc.getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied with no programming rights document {0}", null, args);
        }
    }

    public String getVersionHashCode()
    {
        return doc.getVersionHashCode(getContext());
    }

    public int addAttachments() throws XWikiException
    {
        return addAttachments(null);
    }

    public int addAttachments(String fieldName) throws XWikiException
    {
        if (!hasAccessLevel("edit")) {
            java.lang.Object[] args = {getDoc().getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied in edit mode on document {0}", null, args);
        }
        XWiki xwiki = getContext().getWiki();
        FileUploadPlugin fileupload =
            (FileUploadPlugin) xwiki.getPlugin("fileupload", getContext());
        List fileuploadlist = fileupload.getFileItems(getContext());
        List attachments = new ArrayList();
        int nb = 0;

        if (fileuploadlist == null) {
            return 0;
        }

        Iterator it = fileuploadlist.iterator();
        while (it.hasNext()) {
            DefaultFileItem item = (DefaultFileItem) it.next();
            String name = item.getFieldName();
            if (fieldName != null && !fieldName.equals(name)) {
                continue;
            }
            if (item.isFormField()) {
                continue;
            }
            byte[] data = fileupload.getFileItemData(name, getContext());
            String filename;
            String fname = fileupload.getFileName(name, getContext());
            int i = fname.lastIndexOf("\\");
            if (i == -1) {
                i = fname.lastIndexOf("/");
            }
            filename = fname.substring(i + 1);
            filename = filename.replaceAll("\\+", " ");

            if ((data != null) && (data.length > 0)) {
                XWikiAttachment attachment = addAttachment(filename, data);
                getDoc().saveAttachmentContent(attachment, getContext());
                getDoc().getAttachmentList().add(attachment);
                attachments.add(attachment);
                nb++;
            }
        }
        if (nb > 0) {
            getContext().getWiki().saveDocument(getDoc(), getContext());
        }
        return nb;
    }

    protected XWikiAttachment addAttachment(String fileName, InputStream iStream)
        throws XWikiException, IOException
    {
        ByteArrayOutputStream bAOut = new ByteArrayOutputStream();
        CopyUtils.copy(iStream, bAOut);
        return addAttachment(fileName, bAOut.toByteArray());
    }

    protected XWikiAttachment addAttachment(String fileName, byte[] data) throws XWikiException
    {
        int i = fileName.indexOf("\\");
        if (i == -1) {
            i = fileName.indexOf("/");
        }
        String filename = fileName.substring(i + 1);
        filename = getContext().getWiki().clearName(filename, false, false, getContext());

        XWikiAttachment attachment = getDoc().getAttachment(filename);
        if (attachment == null) {
            attachment = new XWikiAttachment();
            olddoc.getAttachmentList().add(attachment);
        }

        attachment.setContent(data);
        attachment.setFilename(filename);
        attachment.setAuthor(getContext().getUser());
        // Add the attachment to the document
        attachment.setDoc(getDoc());
        return attachment;
    }

    public boolean validate() throws XWikiException
    {
        return doc.validate(getContext());
    }

    public boolean validate(String[] classNames) throws XWikiException
    {
        return doc.validate(classNames, getContext());
    }
}
