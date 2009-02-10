/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */
package com.xpn.xwiki.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.delta.Delta;
import org.suigeneris.jrcs.rcs.Version;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.doc.AttachmentDiff;
import com.xpn.xwiki.doc.MetaDataDiff;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.util.TOCGenerator;
import com.xpn.xwiki.util.Util;

public class Document extends Api
{
    /**
     * The XWikiDocument object wrapped by this API.
     */
    protected XWikiDocument doc;

    /**
     * Indicates if this API wraps a cloned XWikiDocument. 
     */
    protected boolean cloned = false;

    /**
     * Convenience object used by object related methods.
     */
    protected Object currentObj;

    /**
     * Document constructor.
     * 
     * @param doc The XWikiDocument object to wrap.
     * @param context The current request context.
     */
    public Document(XWikiDocument doc, XWikiContext context)
    {
        super(context);
        this.doc = doc;
    }

    /**
     * Get the XWikiDocument wrapped by this API.
     * This function is accessible only if you have the programming rights give access to the priviledged API of the
     * Document.
     * 
     * @return The XWikiDocument wrapped by this API.
     */
    public XWikiDocument getDocument()
    {
        if (hasProgrammingRights()) {
            return this.doc;
        } else {
            return null;
        }
    }

    /**
     * Get a clone of the XWikiDocument wrapped by this API.
     * 
     * @return A clone of the XWikiDocument wrapped by this API.
     */
    protected XWikiDocument getDoc()
    {
        if (!this.cloned) {
            this.doc = (XWikiDocument) this.doc.clone();
            this.cloned = true;
        }

        return this.doc;
    }

    /**
     * return the ID of the document. this ID is unique across the wiki.
     * 
     * @return the id of the document.
     */
    public long getId()
    {
        return this.doc.getId();
    }

    /**
     * return the name of a document. for exemple if the fullName of a document is "MySpace.Mydoc", the name is MyDoc.
     * 
     * @return the name of the document
     */
    public String getName()
    {
        return this.doc.getName();
    }

    /**
     * return the name of the space of the document for example if the fullName of a document is "MySpace.Mydoc", the
     * name is MySpace.
     * 
     * @return the name of the space of the document
     */
    public String getSpace()
    {
        return this.doc.getSpace();
    }

    /**
     * Get the name wiki where the document is stored.
     * 
     * @return The name of the wiki where this document is stored.
     * @since XWiki Core 1.1.2, XWiki Core 1.2M2
     */
    public String getWiki()
    {
        return this.doc.getDatabase();
    }

    /**
     * Get the name of the space of the document for exemple if the fullName of a document is "MySpace.Mydoc", the
     * name is MySpace.
     * 
     * @return The name of the space of the document.
     * @deprecated use {@link #getSpace()} instead of this function.
     */
    @Deprecated
    public String getWeb()
    {
        return this.doc.getSpace();
    }

    /**
     * Get the fullName of the document. 
     * If a document is named "MyDoc" in space "MySpace", the fullname is
     * "MySpace.MyDoc". In a wiki, all the documents have a different fullName.
     * 
     * @return fullName of the document.
     */
    public String getFullName()
    {
        return this.doc.getFullName();
    }

    /**
     * Get the complete fullName of the document.
     * The real full name of the document containing the name of the wiki where the document is stored. 
     * For a document stored in the wiki "xwiki", in space "MySpace", named "MyDoc", its complete full 
     * name is "xwiki:MySpace.MyDoc".
     * 
     * @return The complete fullName of the document.
     * @since XWiki Core 1.1.2, XWiki Core 1.2M2
     */
    public String getPrefixedFullName()
    {
        return (getWiki() != null ? getWiki() + ":" : "") + getFullName();
    }

    /**
     * Get a Version object representing the current version of the document. 
     * 
     * @return A Version object representing the current version of the document
     */
    public Version getRCSVersion()
    {
        return this.doc.getRCSVersion();
    }

    /**
     * Get a string representing the current version of the document.
     * 
     * @return A string representing the current version of the document.
     */
    public String getVersion()
    {
        return this.doc.getVersion();
    }

    /**
     * Get a string representing the previous version of the document.
     * 
     * @return A string representing the previous version of the document.
     */
    public String getPreviousVersion()
    {
        return this.doc.getPreviousVersion();
    }

    /**
     * Get the value of the title field of the document.
     * 
     * @return The value of the title field of the document.
     */
    public String getTitle()
    {
        return this.doc.getTitle();
    }

    /**
     * Get document title.
     * If a title has not been provided through the title field, it looks for a section title in 
     * the document's content and if not found return the page name. The returned title is also 
     * interpreted which means it's allowed to use Velocity, Groovy, etc syntax within a title.
     * 
     * @return The document title.
     */
    public String getDisplayTitle()
    {
        return this.doc.getDisplayTitle(getXWikiContext());
    }

    /**
     * TODO: document this.
     */
    public String getFormat()
    {
        return this.doc.getFormat();
    }

    /**
     * Get fullName of the profile document of the author of the current version of the document.
     * Example: XWiki.Admin.
     * 
     * @return The  fullName of the profile document of the author of the current version of the document.
     */
    public String getAuthor()
    {
        return this.doc.getAuthor();
    }

    /**
     * Get fullName of the profile document of the author of the last content modification in the document.
     * Example: XWiki.Admin.
     * 
     * @return The fullName of the profile document of the author of the last content modification in the document.
     */
    public String getContentAuthor()
    {
        return this.doc.getContentAuthor();
    }

    /**
     * Get the date when the current document version has been created, i.e. the date of the last 
     * modification of the document.
     * 
     * @return The date where the current document version has been created.
     */
    public Date getDate()
    {
        return this.doc.getDate();
    }

    /**
     * Get the date when the last content modification has been done on the document.
     * 
     * @return The date where the last content modification has been done on the document.
     */
    public Date getContentUpdateDate()
    {
        return this.doc.getContentUpdateDate();
    }

    /**
     * Get the date when the document has been created.
     * return The date when the document has been created.
     */
    public Date getCreationDate()
    {
        return this.doc.getCreationDate();
    }

    /**
     * Get the name of the parent of this document.
     * 
     * @return The name of the parent of this document.
     */
    public String getParent()
    {
        return this.doc.getParent();
    }

    /**
     * Get fullName of the profile document of the document creator.
     * 
     * @return The fullName of the profile document of the document creator.
     */
    public String getCreator()
    {
        return this.doc.getCreator();
    }

    /**
     * Get raw content of the document, i.e. the content that is visible through the wiki editor.
     * 
     * @return The raw content of the document.
     */
    public String getContent()
    {
        return this.doc.getContent();
    }

    /**
     * Get the Syntax id representing the syntax used for the document. For example "xwiki/1.0" represents
     * the first version XWiki syntax while "xwiki/2.0" represents version 2.0 of the XWiki Syntax. 
     *
     * @return The syntax id representing the syntax used for the document. 
     */
    public String getSyntaxId()
    {
        return this.doc.getSyntaxId();
    }

    /**
     * Get the language of the document.
     * If the document is a translation it returns the language set for it, otherwise, it returns the default
     * language in the wiki where the document is stored.
     * 
     * @return The language of the document. 
     */
    public String getLanguage()
    {
        return this.doc.getLanguage();
    }

    
    public String getTemplate()
    {
        return this.doc.getTemplate();
    }

    /**
     * return the real language of the document
     */
    public String getRealLanguage() throws XWikiException
    {
        return this.doc.getRealLanguage(getXWikiContext());
    }

    /**
     * return the language of the default document
     */
    public String getDefaultLanguage()
    {
        return this.doc.getDefaultLanguage();
    }

    public String getDefaultTemplate()
    {
        return this.doc.getDefaultTemplate();
    }

    /**
     * return the comment of the latest update of the document
     */
    public String getComment()
    {
        return this.doc.getComment();
    }

    public boolean isMinorEdit()
    {
        return this.doc.isMinorEdit();
    }

    /**
     * Return the list of existing translations for this document.
     */
    public List<String> getTranslationList() throws XWikiException
    {
        return this.doc.getTranslationList(getXWikiContext());
    }

    /**
     * return the translated document's content if the wiki is multilingual, the language is first checked in the URL,
     * the cookie, the user profile and finally the wiki configuration if not, the language is the one on the wiki
     * configuration
     */
    public String getTranslatedContent() throws XWikiException
    {
        return this.doc.getTranslatedContent(getXWikiContext());
    }

    /**
     * return the translated content in the given language
     */
    public String getTranslatedContent(String language) throws XWikiException
    {
        return this.doc.getTranslatedContent(language, getXWikiContext());
    }

    /**
     * return the translated document in the given document
     */
    public Document getTranslatedDocument(String language) throws XWikiException
    {
        return this.doc.getTranslatedDocument(language, getXWikiContext()).newDocument(getXWikiContext());
    }

    /**
     * return the tranlated Document if the wiki is multilingual, the language is first checked in the URL, the cookie,
     * the user profile and finally the wiki configuration if not, the language is the one on the wiki configuration
     */
    public Document getTranslatedDocument() throws XWikiException
    {
        return this.doc.getTranslatedDocument(getXWikiContext()).newDocument(getXWikiContext());
    }

    /**
     * return the content of the document rendererd
     */
    public String getRenderedContent() throws XWikiException
    {
        return this.doc.getRenderedContent(getXWikiContext());
    }

    /**
     * @param text the text to render
     * @return the given text rendered in the context of this document
     * @deprecated since 1.6M1 use {@link #getRenderedContent(String, String)}
     */
    @Deprecated
    public String getRenderedContent(String text) throws XWikiException
    {
        return this.doc.getRenderedContent(text, getXWikiContext());
    }

    /**
     * @param text the text to render
     * @param syntaxId the id of the Syntax used by the passed text (for example: "xwiki/1.0")
     * @return the given text rendered in the context of this document using the passed Syntax
     * @since 1.6M1
     */
    public String getRenderedContent(String text, String syntaxId) throws XWikiException
    {
        return this.doc.getRenderedContent(text, syntaxId, getXWikiContext());
    }

    /**
     * return a escaped version of the content of this document
     */
    public String getEscapedContent() throws XWikiException
    {
        return this.doc.getEscapedContent(getXWikiContext());
    }

    /**
     * return the archive of the document in a string format
     */
    public String getArchive() throws XWikiException
    {
        return this.doc.getDocumentArchive(getXWikiContext()).getArchive(getXWikiContext());
    }

    /**
     * this function is accessible only if you have the programming rights return the archive of the document
     */
    public XWikiDocumentArchive getDocumentArchive() throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.doc.getDocumentArchive(getXWikiContext());
        }
        return null;
    }

    /**
     * @return true if the document is a new one (ie it has never been saved) or false otherwise
     */
    public boolean isNew()
    {
        return this.doc.isNew();
    }

    /**
     * return the URL of download for the the given attachment name
     * 
     * @param filename the name of the attachment
     * @return A String with the URL
     */
    public String getAttachmentURL(String filename)
    {
        return this.doc.getAttachmentURL(filename, "download", getXWikiContext());
    }

    /**
     * return the URL of the given action for the the given attachment name
     * 
     * @return A string with the URL
     */
    public String getAttachmentURL(String filename, String action)
    {
        return this.doc.getAttachmentURL(filename, action, getXWikiContext());
    }

    /**
     * return the URL of the given action for the the given attachment name with "queryString" parameters
     * 
     * @param queryString parameters added to the URL
     */
    public String getAttachmentURL(String filename, String action, String queryString)
    {
        return this.doc.getAttachmentURL(filename, action, queryString, getXWikiContext());
    }

    /**
     * return the URL for accessing to the archive of the attachment "filename" at the version "version"
     */
    public String getAttachmentRevisionURL(String filename, String version)
    {
        return this.doc.getAttachmentRevisionURL(filename, version, getXWikiContext());
    }

    /**
     * return the URL for accessing to the archive of the attachment "filename" at the version "version" and with the
     * given queryString parameters
     */
    public String getAttachmentRevisionURL(String filename, String version, String querystring)
    {
        return this.doc.getAttachmentRevisionURL(filename, version, querystring, getXWikiContext());
    }

    /**
     * return the URL of this document in view mode
     */
    public String getURL()
    {
        return this.doc.getURL("view", getXWikiContext());
    }

    /**
     * return the URL of this document with the given action
     */
    public String getURL(String action)
    {
        return this.doc.getURL(action, getXWikiContext());
    }

    /**
     * return the URL of this document with the given action and queryString as parameters
     */
    public String getURL(String action, String querystring)
    {
        return this.doc.getURL(action, querystring, getXWikiContext());
    }

    /**
     * return the full URL of the document
     */
    public String getExternalURL()
    {
        return this.doc.getExternalURL("view", getXWikiContext());
    }

    /**
     * return the full URL of the document for the given action
     */
    public String getExternalURL(String action)
    {
        return this.doc.getExternalURL(action, getXWikiContext());
    }

    public String getExternalURL(String action, String querystring)
    {
        return this.doc.getExternalURL(action, querystring, getXWikiContext());
    }

    public String getParentURL() throws XWikiException
    {
        return this.doc.getParentURL(getXWikiContext());
    }

    public Class getxWikiClass()
    {
        BaseClass bclass = this.doc.getxWikiClass();
        if (bclass == null) {
            return null;
        } else {
            return new Class(bclass, getXWikiContext());
        }
    }

    public Class[] getxWikiClasses()
    {
        List<BaseClass> list = this.doc.getxWikiClasses(getXWikiContext());
        if (list == null) {
            return null;
        }
        Class[] result = new Class[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = new Class(list.get(i), getXWikiContext());
        }
        return result;
    }

    public int createNewObject(String classname) throws XWikiException
    {
        return getDoc().createNewObject(classname, getXWikiContext());
    }

    public Object newObject(String classname) throws XWikiException
    {
        int nb = createNewObject(classname);
        return getObject(classname, nb);
    }

    public boolean isFromCache()
    {
        return this.doc.isFromCache();
    }

    public int getObjectNumbers(String classname)
    {
        return this.doc.getObjectNumbers(classname);
    }

    public Map<String, Vector<Object>> getxWikiObjects()
    {
        Map<String, Vector<BaseObject>> map = this.doc.getxWikiObjects();
        Map<String, Vector<Object>> resultmap = new HashMap<String, Vector<Object>>();
        for (String name : map.keySet()) {
            Vector<BaseObject> objects = map.get(name);
            if (objects != null) {
                resultmap.put(name, getObjects(objects));
            }
        }
        return resultmap;
    }

    protected Vector<Object> getObjects(Vector<BaseObject> objects)
    {
        Vector<Object> result = new Vector<Object>();
        if (objects == null) {
            return result;
        }
        for (BaseObject bobj : objects) {
            if (bobj != null) {
                result.add(newObjectApi(bobj, getXWikiContext()));
            }
        }
        return result;
    }

    public Vector<Object> getObjects(String classname)
    {
        Vector<BaseObject> objects = this.doc.getObjects(classname);
        return getObjects(objects);
    }

    public Object getFirstObject(String fieldname)
    {
        try {
            BaseObject obj = this.doc.getFirstObject(fieldname, getXWikiContext());
            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getXWikiContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Object getObject(String classname, String key, String value, boolean failover)
    {
        try {
            BaseObject obj = this.doc.getObject(classname, key, value, failover);
            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getXWikiContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Select a subset of objects from a given class, filtered on a "key = value" criteria.
     * 
     * @param classname The type of objects to return.
     * @param key The name of the property used for filtering.
     * @param value The required value.
     * @return A Vector of {@link Object objects} matching the criteria. If no objects are found, or if the key is an
     *         empty String, then an empty vector is returned.
     */
    public Vector<Object> getObjects(String classname, String key, String value)
    {
        Vector<Object> result = new Vector<Object>();
        if (StringUtils.isBlank(key) || value == null) {
            return getObjects(classname);
        }
        try {
            Vector<BaseObject> allObjects = this.doc.getObjects(classname);
            if (allObjects == null || allObjects.size() == 0) {
                return result;
            } else {
                for (BaseObject obj : allObjects) {
                    if (obj != null) {
                        BaseProperty prop = (BaseProperty) obj.get(key);
                        if (prop == null || prop.getValue() == null) {
                            continue;
                        }
                        if (value.equals(prop.getValue().toString())) {
                            result.add(newObjectApi(obj, getXWikiContext()));
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return result;
    }

    public Object getObject(String classname, String key, String value)
    {
        try {
            BaseObject obj = this.doc.getObject(classname, key, value);
            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getXWikiContext());
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
     * get the object of the given className. If there is no object of this className and the create parameter at true,
     * the object is created.
     */
    public Object getObject(String classname, boolean create)
    {
        try {
            BaseObject obj = this.doc.getObject(classname, create, getXWikiContext());

            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getXWikiContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Object getObject(String classname, int nb)
    {
        try {
            BaseObject obj = this.doc.getObject(classname, nb);
            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getXWikiContext());
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
        String xml = this.doc.getXMLContent(getXWikiContext());
        return getXWikiContext().getUtil().substitute(
            "s/<email>.*?<\\/email>/<email>********<\\/email>/goi",
            getXWikiContext().getUtil().substitute("s/<password>.*?<\\/password>/<password>********<\\/password>/goi",
                xml));
    }

    public String toXML() throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.doc.toXML(getXWikiContext());
        } else {
            return "";
        }
    }

    public org.dom4j.Document toXMLDocument() throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.doc.toXMLDocument(getXWikiContext());
        } else {
            return null;
        }
    }

    public Version[] getRevisions() throws XWikiException
    {
        return this.doc.getRevisions(getXWikiContext());
    }

    public String[] getRecentRevisions() throws XWikiException
    {
        return this.doc.getRecentRevisions(5, getXWikiContext());
    }

    public String[] getRecentRevisions(int nb) throws XWikiException
    {
        return this.doc.getRecentRevisions(nb, getXWikiContext());
    }

    /**
     * Get document versions matching criterias like author, minimum creation date, etc.
     * 
     * @param criteria criteria used to match versions
     * @return a list of matching versions
     */
    public List<String> getRevisions(RevisionCriteria criteria) throws XWikiException
    {
        return this.doc.getRevisions(criteria, this.context);
    }

    /**
     * Get information about a document version : author, date, etc.
     * 
     * @param version the version you want to get information about
     * @return a new RevisionInfo object
     */
    public RevisionInfo getRevisionInfo(String version) throws XWikiException
    {
        return new RevisionInfo(this.doc.getRevisionInfo(version, getXWikiContext()), getXWikiContext());
    }

    public List<Attachment> getAttachmentList()
    {
        List<Attachment> apis = new ArrayList<Attachment>();
        for (XWikiAttachment attachment : this.doc.getAttachmentList()) {
            apis.add(new Attachment(this, attachment, getXWikiContext()));
        }
        return apis;
    }

    public Vector<Object> getComments()
    {
        return getComments(true);
    }

    public Vector<Object> getComments(boolean asc)
    {
        return getObjects(this.doc.getComments(asc));
    }

    public void use(Object object)
    {
        this.currentObj = object;
    }

    public void use(String className)
    {
        this.currentObj = getObject(className);
    }

    public void use(String className, int nb)
    {
        this.currentObj = getObject(className, nb);
    }

    public String getActiveClass()
    {
        if (this.currentObj == null) {
            return null;
        } else {
            return this.currentObj.getName();
        }
    }

    public String displayPrettyName(String fieldname)
    {
        if (this.currentObj == null) {
            return this.doc.displayPrettyName(fieldname, getXWikiContext());
        } else {
            return this.doc.displayPrettyName(fieldname, this.currentObj.getBaseObject(), getXWikiContext());
        }
    }

    public String displayPrettyName(String fieldname, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.displayPrettyName(fieldname, obj.getBaseObject(), getXWikiContext());
    }

    public String displayPrettyName(String fieldname, boolean showMandatory)
    {
        if (this.currentObj == null) {
            return this.doc.displayPrettyName(fieldname, showMandatory, getXWikiContext());
        } else {
            return this.doc.displayPrettyName(fieldname, showMandatory, this.currentObj.getBaseObject(),
                getXWikiContext());
        }
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.displayPrettyName(fieldname, showMandatory, obj.getBaseObject(), getXWikiContext());
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, boolean before)
    {
        if (this.currentObj == null) {
            return this.doc.displayPrettyName(fieldname, showMandatory, before, getXWikiContext());
        } else {
            return this.doc.displayPrettyName(fieldname, showMandatory, before, this.currentObj.getBaseObject(),
                getXWikiContext());
        }
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, boolean before, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.displayPrettyName(fieldname, showMandatory, before, obj.getBaseObject(), getXWikiContext());
    }

    public String displayTooltip(String fieldname)
    {
        if (this.currentObj == null) {
            return this.doc.displayTooltip(fieldname, getXWikiContext());
        } else {
            return this.doc.displayTooltip(fieldname, this.currentObj.getBaseObject(), getXWikiContext());
        }
    }

    public String displayTooltip(String fieldname, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.displayTooltip(fieldname, obj.getBaseObject(), getXWikiContext());
    }

    public String display(String fieldname)
    {
        if (this.currentObj == null) {
            return this.doc.display(fieldname, getXWikiContext());
        } else {
            return this.doc.display(fieldname, this.currentObj.getBaseObject(), getXWikiContext());
        }
    }

    public String display(String fieldname, String mode)
    {
        if (this.currentObj == null) {
            return this.doc.display(fieldname, mode, getXWikiContext());
        } else {
            return this.doc.display(fieldname, mode, this.currentObj.getBaseObject(), getXWikiContext());
        }
    }

    public String display(String fieldname, String mode, String prefix)
    {
        if (this.currentObj == null) {
            return this.doc.display(fieldname, mode, prefix, getXWikiContext());
        } else {
            return this.doc.display(fieldname, mode, prefix, this.currentObj.getBaseObject(), getXWikiContext());
        }
    }

    public String display(String fieldname, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.display(fieldname, obj.getBaseObject(), getXWikiContext());
    }

    public String display(String fieldname, String mode, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.display(fieldname, mode, obj.getBaseObject(), getXWikiContext());
    }

    public String display(String fieldname, String mode, String prefix, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.display(fieldname, mode, prefix, obj.getBaseObject(), getXWikiContext());
    }

    public String displayForm(String className, String header, String format)
    {
        return this.doc.displayForm(className, header, format, getXWikiContext());
    }

    public String displayForm(String className, String header, String format, boolean linebreak)
    {
        return this.doc.displayForm(className, header, format, linebreak, getXWikiContext());
    }

    public String displayForm(String className)
    {
        return this.doc.displayForm(className, getXWikiContext());
    }

    public String displayRendered(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object)
        throws XWikiException
    {
        if ((pclass == null) || (object == null)) {
            return "";
        }
        return this.doc.displayRendered(pclass.getBasePropertyClass(), prefix, object.getCollection(),
            getXWikiContext());
    }

    public String displayView(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object)
    {
        if ((pclass == null) || (object == null)) {
            return "";
        }
        return this.doc.displayView(pclass.getBasePropertyClass(), prefix, object.getCollection(), getXWikiContext());
    }

    public String displayEdit(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object)
    {
        if ((pclass == null) || (object == null)) {
            return "";
        }
        return this.doc.displayEdit(pclass.getBasePropertyClass(), prefix, object.getCollection(), getXWikiContext());
    }

    public String displayHidden(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object)
    {
        if ((pclass == null) || (object == null)) {
            return "";
        }
        return this.doc.displayHidden(pclass.getBasePropertyClass(), prefix, object.getCollection(), getXWikiContext());
    }

    public List<String> getIncludedPages()
    {
        return this.doc.getIncludedPages(getXWikiContext());
    }

    public List<String> getIncludedMacros()
    {
        return this.doc.getIncludedMacros(getXWikiContext());
    }

    public List<String> getLinkedPages()
    {
        return this.doc.getLinkedPages(getXWikiContext());
    }

    public Attachment getAttachment(String filename)
    {
        XWikiAttachment attach = this.doc.getAttachment(filename);
        if (attach == null) {
            return null;
        } else {
            return new Attachment(this, attach, getXWikiContext());
        }
    }

    public List<Delta> getContentDiff(Document origdoc, Document newdoc) throws XWikiException,
        DifferentiationFailedException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getContentDiff(new XWikiDocument(newdoc.getSpace(), newdoc.getName()), newdoc.doc,
                    getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getContentDiff(origdoc.doc, new XWikiDocument(origdoc.getSpace(), origdoc.getName()),
                    getXWikiContext());
            }

            return this.doc.getContentDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args = {origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion()};
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_CONTENT_ERROR,
                    "Error while making content diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<Delta> getXMLDiff(Document origdoc, Document newdoc) throws XWikiException,
        DifferentiationFailedException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getXMLDiff(new XWikiDocument(newdoc.getSpace(), newdoc.getName()), newdoc.doc,
                    getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getXMLDiff(origdoc.doc, new XWikiDocument(origdoc.getSpace(), origdoc.getName()),
                    getXWikiContext());
            }

            return this.doc.getXMLDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args = {origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion()};
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_XML_ERROR,
                    "Error while making xml diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<Delta> getRenderedContentDiff(Document origdoc, Document newdoc) throws XWikiException,
        DifferentiationFailedException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getRenderedContentDiff(new XWikiDocument(newdoc.getSpace(), newdoc.getName()),
                    newdoc.doc, getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getRenderedContentDiff(origdoc.doc, new XWikiDocument(origdoc.getSpace(), origdoc
                    .getName()), getXWikiContext());
            }

            return this.doc.getRenderedContentDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args = {origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion()};
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_RENDERED_ERROR,
                    "Error while making rendered diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<MetaDataDiff> getMetaDataDiff(Document origdoc, Document newdoc) throws XWikiException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getMetaDataDiff(new XWikiDocument(newdoc.getSpace(), newdoc.getName()), newdoc.doc,
                    getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getMetaDataDiff(origdoc.doc, new XWikiDocument(origdoc.getSpace(), origdoc.getName()),
                    getXWikiContext());
            }

            return this.doc.getMetaDataDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args = {origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion()};
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_METADATA_ERROR,
                    "Error while making meta data diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<List<ObjectDiff>> getObjectDiff(Document origdoc, Document newdoc) throws XWikiException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getObjectDiff(new XWikiDocument(newdoc.getSpace(), newdoc.getName()), newdoc.doc,
                    getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getObjectDiff(origdoc.doc, new XWikiDocument(origdoc.getSpace(), origdoc.getName()),
                    getXWikiContext());
            }

            return this.doc.getObjectDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args = {origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion()};
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_OBJECT_ERROR,
                    "Error while making meta object diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<List<ObjectDiff>> getClassDiff(Document origdoc, Document newdoc) throws XWikiException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getClassDiff(new XWikiDocument(newdoc.getSpace(), newdoc.getName()), newdoc.doc,
                    getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getClassDiff(origdoc.doc, new XWikiDocument(origdoc.getSpace(), origdoc.getName()),
                    getXWikiContext());
            }

            return this.doc.getClassDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args = {origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion()};
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_CLASS_ERROR,
                    "Error while making class diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<AttachmentDiff> getAttachmentDiff(Document origdoc, Document newdoc) throws XWikiException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getAttachmentDiff(new XWikiDocument(newdoc.getSpace(), newdoc.getName()), newdoc.doc,
                    getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getAttachmentDiff(origdoc.doc,
                    new XWikiDocument(origdoc.getSpace(), origdoc.getName()), getXWikiContext());
            }

            return this.doc.getAttachmentDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args =
                {(origdoc != null) ? origdoc.getFullName() : null, (origdoc != null) ? origdoc.getVersion() : null,
                (newdoc != null) ? newdoc.getVersion() : null};
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_ATTACHMENT_ERROR,
                    "Error while making attachment diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<Delta> getLastChanges() throws XWikiException, DifferentiationFailedException
    {
        return this.doc.getLastChanges(getXWikiContext());
    }

    public DocumentStats getCurrentMonthPageStats(String action)
    {
        return getXWikiContext().getWiki().getStatsService(getXWikiContext()).getDocMonthStats(this.doc.getFullName(),
            action, new Date(), getXWikiContext());
    }

    public DocumentStats getCurrentMonthWebStats(String action)
    {
        return getXWikiContext().getWiki().getStatsService(getXWikiContext()).getDocMonthStats(this.doc.getSpace(),
            action, new Date(), getXWikiContext());
    }

    public List getCurrentMonthRefStats() throws XWikiException
    {
        return getXWikiContext().getWiki().getStatsService(getXWikiContext()).getRefMonthStats(this.doc.getFullName(),
            new Date(), getXWikiContext());
    }

    public boolean checkAccess(String right)
    {
        try {
            return getXWikiContext().getWiki().checkAccess(right, this.doc, getXWikiContext());
        } catch (XWikiException e) {
            return false;
        }
    }

    public boolean hasAccessLevel(String level)
    {
        try {
            return getXWikiContext().getWiki().getRightService().hasAccessLevel(level, getXWikiContext().getUser(),
                this.doc.getFullName(), getXWikiContext());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasAccessLevel(String level, String user)
    {
        try {
            return getXWikiContext().getWiki().getRightService().hasAccessLevel(level, user, this.doc.getFullName(),
                getXWikiContext());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean getLocked()
    {
        try {
            XWikiLock lock = this.doc.getLock(getXWikiContext());
            if (lock != null && !getXWikiContext().getUser().equals(lock.getUserName())) {
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
            XWikiLock lock = this.doc.getLock(getXWikiContext());
            if (lock != null && !getXWikiContext().getUser().equals(lock.getUserName())) {
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
            XWikiLock lock = this.doc.getLock(getXWikiContext());
            if (lock != null && !getXWikiContext().getUser().equals(lock.getUserName())) {
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
        if (this.currentObj != null) {
            return this.doc.display(classOrFieldName, this.currentObj.getBaseObject(), getXWikiContext());
        }
        BaseObject object = this.doc.getFirstObject(classOrFieldName, getXWikiContext());
        if (object != null) {
            return this.doc.display(classOrFieldName, object, getXWikiContext());
        }
        return this.doc.getObject(classOrFieldName);
    }

    public java.lang.Object getValue(String fieldName)
    {
        Object object;
        if (this.currentObj == null) {
            object = new Object(this.doc.getFirstObject(fieldName, getXWikiContext()), getXWikiContext());
        } else {
            object = this.currentObj;
        }
        return getValue(fieldName, object);
    }

    public java.lang.Object getValue(String fieldName, Object object)
    {
        if (object != null) {
            try {
                return ((BaseProperty) object.getBaseObject().safeget(fieldName)).getValue();
            } catch (NullPointerException e) {
                return null;
            }
        }
        return null;
    }

    public String getTextArea()
    {
        return com.xpn.xwiki.XWiki.getTextArea(this.doc.getContent(), getXWikiContext());
    }

    /**
     * Returns data needed for a generation of Table of Content for this document.
     * 
     * @param init an intial level where the TOC generation should start at
     * @param max maximum level TOC is generated for
     * @param numbered if should generate numbering for headings
     * @return a map where an heading (title) ID is the key and value is another map with two keys: text, level and
     *         numbering
     */
    public Map getTOC(int init, int max, boolean numbered)
    {
        getXWikiContext().put("tocNumbered", new Boolean(numbered));
        return TOCGenerator.generateTOC(getContent(), init, max, numbered, getXWikiContext());
    }

    public String getTags()
    {
        return this.doc.getTags(getXWikiContext());
    }

    public List<String> getTagList()
    {
        return this.doc.getTagsList(getXWikiContext());
    }

    public List getTagsPossibleValues()
    {
        return this.doc.getTagsPossibleValues(getXWikiContext());
    }

    public void insertText(String text, String marker) throws XWikiException
    {
        if (hasAccessLevel("edit")) {
            getDoc().insertText(text, marker, getXWikiContext());
        }
    }

    @Override
    public boolean equals(java.lang.Object arg0)
    {
        if (!(arg0 instanceof Document)) {
            return false;
        }
        Document d = (Document) arg0;
        return d.getXWikiContext().equals(getXWikiContext()) && this.doc.equals(d.doc);
    }

    public List<String> getBacklinks() throws XWikiException
    {
        return this.doc.getBackLinkedPages(getXWikiContext());
    }

    public List<XWikiLink> getLinks() throws XWikiException
    {
        return this.doc.getWikiLinkedPages(getXWikiContext());
    }
    
    /**
     * Get document children. Children are document with the current document as parent.
     * 
     * @return The list of children for the current document.
     * @since 1.8 Milestone 2
     */
    public List<String> getChildren() throws XWikiException
    {
        return this.doc.getChildren(getXWikiContext());
    }

    public String getDefaultEditURL() throws XWikiException
    {
        return this.doc.getDefaultEditURL(getXWikiContext());
    }

    public String getEditURL(String action, String mode) throws XWikiException
    {
        return this.doc.getEditURL(action, mode, getXWikiContext());
    }

    public String getEditURL(String action, String mode, String language)
    {
        return this.doc.getEditURL(action, mode, language, getXWikiContext());
    }

    public boolean isCurrentUserCreator()
    {
        return this.doc.isCurrentUserCreator(getXWikiContext());
    }

    public boolean isCurrentUserPage()
    {
        return this.doc.isCurrentUserPage(getXWikiContext());
    }

    public boolean isCurrentLocalUserPage()
    {
        return this.doc.isCurrentLocalUserPage(getXWikiContext());
    }

    public boolean isCreator(String username)
    {
        return this.doc.isCreator(username);
    }

    public void set(String fieldname, java.lang.Object value)
    {
        Object obj;
        if (this.currentObj != null) {
            obj = this.currentObj;
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

    /**
     * @param syntaxId the Syntax id representing the syntax used for the current document. For example "xwiki/1.0" represents
     *         the first version XWiki syntax while "xwiki/2.0" represents version 2.0 of the XWiki Syntax.
     */
    public void setSyntaxId(String syntaxId)
    {
        getDoc().setSyntaxId(syntaxId);
    }

    public void setDefaultTemplate(String dtemplate)
    {
        getDoc().setDefaultTemplate(dtemplate);
    }

    public void setComment(String comment)
    {
        getDoc().setComment(comment);
    }

    public void setMinorEdit(boolean isMinor)
    {
        getDoc().setMinorEdit(isMinor);
    }

    public void save() throws XWikiException
    {
        save("", false);
    }

    public void save(String comment) throws XWikiException
    {
        save(comment, false);
    }

    public void save(String comment, boolean minorEdit) throws XWikiException
    {
        if (hasAccessLevel("edit")) {
            saveDocument(comment, minorEdit);
        } else {
            java.lang.Object[] args = {this.doc.getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied in edit mode on document {0}", null, args);
        }
    }

    public void saveWithProgrammingRights() throws XWikiException
    {
        saveWithProgrammingRights("", false);
    }

    public void saveWithProgrammingRights(String comment) throws XWikiException
    {
        saveWithProgrammingRights(comment, false);
    }

    public void saveWithProgrammingRights(String comment, boolean minorEdit) throws XWikiException
    {
        if (hasProgrammingRights()) {
            saveDocument(comment, minorEdit);
        } else {
            java.lang.Object[] args = {this.doc.getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied with no programming rights document {0}", null, args);
        }
    }

    protected void saveDocument(String comment, boolean minorEdit) throws XWikiException
    {
        XWikiDocument doc = getDoc();
        doc.setAuthor(this.context.getUser());
        if (doc.isNew()) {
            doc.setCreator(this.context.getUser());
        }
        getXWikiContext().getWiki().saveDocument(doc, comment, minorEdit, getXWikiContext());
        this.cloned = false;
    }

    public com.xpn.xwiki.api.Object addObjectFromRequest() throws XWikiException
    {
        // Call to getDoc() ensures that we are working on a clone()
        return new com.xpn.xwiki.api.Object(getDoc().addObjectFromRequest(getXWikiContext()), getXWikiContext());
    }

    public com.xpn.xwiki.api.Object addObjectFromRequest(String className) throws XWikiException
    {
        return new com.xpn.xwiki.api.Object(getDoc().addObjectFromRequest(className, getXWikiContext()),
            getXWikiContext());
    }

    public List<Object> addObjectsFromRequest(String className) throws XWikiException
    {
        return addObjectsFromRequest(className, "");
    }

    public com.xpn.xwiki.api.Object addObjectFromRequest(String className, String prefix) throws XWikiException
    {
        return new com.xpn.xwiki.api.Object(getDoc().addObjectFromRequest(className, prefix, getXWikiContext()),
            getXWikiContext());
    }

    public List<Object> addObjectsFromRequest(String className, String prefix) throws XWikiException
    {
        List<BaseObject> objs = getDoc().addObjectsFromRequest(className, prefix, getXWikiContext());
        List<Object> wrapped = new ArrayList<Object>();
        for (BaseObject object : objs) {
            wrapped.add(new com.xpn.xwiki.api.Object(object, getXWikiContext()));
        }
        return wrapped;
    }

    public com.xpn.xwiki.api.Object updateObjectFromRequest(String className) throws XWikiException
    {
        return new com.xpn.xwiki.api.Object(getDoc().updateObjectFromRequest(className, getXWikiContext()),
            getXWikiContext());
    }

    public List<Object> updateObjectsFromRequest(String className) throws XWikiException
    {
        return updateObjectsFromRequest(className, "");
    }

    public com.xpn.xwiki.api.Object updateObjectFromRequest(String className, String prefix) throws XWikiException
    {
        return new com.xpn.xwiki.api.Object(getDoc().updateObjectFromRequest(className, prefix, getXWikiContext()),
            getXWikiContext());
    }

    public List<Object> updateObjectsFromRequest(String className, String prefix) throws XWikiException
    {
        List<BaseObject> objs = getDoc().updateObjectsFromRequest(className, prefix, getXWikiContext());
        List<Object> wrapped = new ArrayList<Object>();
        for (BaseObject object : objs) {
            wrapped.add(new com.xpn.xwiki.api.Object(object, getXWikiContext()));
        }
        return wrapped;
    }

    public boolean isAdvancedContent()
    {
        return this.doc.isAdvancedContent();
    }

    public boolean isProgrammaticContent()
    {
        return this.doc.isProgrammaticContent();
    }

    public boolean removeObject(Object obj)
    {
        return getDoc().removeObject(obj.getBaseObject());
    }

    public boolean removeObjects(String className)
    {
        return getDoc().removeObjects(className);
    }

    /**
     * Remove document from the wiki. Reinit <code>cloned</code>.
     * 
     * @throws XWikiException
     */
    protected void deleteDocument() throws XWikiException
    {
        getXWikiContext().getWiki().deleteDocument(this.doc, getXWikiContext());
        this.cloned = false;
    }

    public void delete() throws XWikiException
    {
        if (hasAccessLevel("delete")) {
            deleteDocument();
        } else {
            java.lang.Object[] args = {this.doc.getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied in edit mode on document {0}", null, args);
        }
    }

    public void deleteWithProgrammingRights() throws XWikiException
    {
        if (hasProgrammingRights()) {
            deleteDocument();
        } else {
            java.lang.Object[] args = {this.doc.getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied with no programming rights document {0}", null, args);
        }
    }

    public String getVersionHashCode()
    {
        return this.doc.getVersionHashCode(getXWikiContext());
    }

    public int addAttachments() throws XWikiException
    {
        return addAttachments(null);
    }

    public int addAttachments(String fieldName) throws XWikiException
    {
        if (!hasAccessLevel("edit")) {
            java.lang.Object[] args = {this.doc.getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied in edit mode on document {0}", null, args);
        }
        XWiki xwiki = getXWikiContext().getWiki();
        FileUploadPlugin fileupload = (FileUploadPlugin) xwiki.getPlugin("fileupload", getXWikiContext());
        List<FileItem> fileuploadlist = fileupload.getFileItems(getXWikiContext());
        List<XWikiAttachment> attachments = new ArrayList<XWikiAttachment>();
        // adding attachment list to context so we find the names
        this.context.put("addedAttachments", attachments);
        int nb = 0;

        if (fileuploadlist == null) {
            return 0;
        }

        for (FileItem item : fileuploadlist) {
            String name = item.getFieldName();
            if (fieldName != null && !fieldName.equals(name)) {
                continue;
            }
            if (item.isFormField()) {
                continue;
            }
            byte[] data = fileupload.getFileItemData(name, getXWikiContext());
            String filename;
            String fname = fileupload.getFileName(name, getXWikiContext());
            int i = fname.lastIndexOf("\\");
            if (i == -1) {
                i = fname.lastIndexOf("/");
            }
            filename = fname.substring(i + 1);
            filename = filename.replaceAll("\\+", " ");

            if ((data != null) && (data.length > 0)) {
                XWikiAttachment attachment = this.doc.addAttachment(filename, data, getXWikiContext());
                getDoc().saveAttachmentContent(attachment, getXWikiContext());
                // commenting because this was already done by addAttachment
                // getDoc().getAttachmentList().add(attachment);
                attachments.add(attachment);
                nb++;
            }
        }
        if (nb > 0) {
            getXWikiContext().getWiki().saveDocument(getDoc(), getXWikiContext());
            this.cloned = false;
        }
        return nb;
    }

    public Attachment addAttachment(String fileName, InputStream iStream)
    {
        try {
            return new Attachment(this, this.doc.addAttachment(fileName, iStream, getXWikiContext()), getXWikiContext());
        } catch (XWikiException e) {
            // TODO Log the error and let the user know about it
        } catch (IOException e) {
            // TODO Log the error and let the user know about it
        }
        return null;
    }

    public Attachment addAttachment(String fileName, byte[] data)
    {
        try {
            return new Attachment(this, this.doc.addAttachment(fileName, data, getXWikiContext()), getXWikiContext());
        } catch (XWikiException e) {
            // TODO Log the error and let the user know about it
        }
        return null;
    }

    public boolean validate() throws XWikiException
    {
        return this.doc.validate(getXWikiContext());
    }

    public boolean validate(String[] classNames) throws XWikiException
    {
        return this.doc.validate(classNames, getXWikiContext());
    }

    /**
     * Retrieves the validation script associated with this document, a Velocity script that is executed when validating
     * the document data.
     * 
     * @return A <code>String</code> representation of the validation script, or an empty string if there is no such
     *         script.
     */
    public String getValidationScript()
    {
        return getDoc().getValidationScript();
    }

    /**
     * Sets a new validation script for this document, a Velocity script that is executed when validating the document
     * data.
     * 
     * @param validationScript The new validation script, which can be an empty string or <code>null</code> if the
     *            script should be removed.
     */
    public void setValidationScript(String validationScript)
    {
        getDoc().setValidationScript(validationScript);
    }

    /**
     * @deprecated use {@link #rename(String)} instead
     */
    @Deprecated
    public void renameDocument(String newDocumentName) throws XWikiException
    {
        rename(newDocumentName);
    }

    /**
     * Rename the current document and all the backlinks leading to it. See
     * {@link #renameDocument(String, java.util.List)} for more details.
     * 
     * @param newDocumentName the new document name. If the space is not specified then defaults to the current space.
     * @throws XWikiException in case of an error
     */
    public void rename(String newDocumentName) throws XWikiException
    {
        this.doc.rename(newDocumentName, getXWikiContext());
    }

    /**
     * @deprecated use {@link #rename(String, java.util.List)} instead
     */
    @Deprecated
    public void renameDocument(String newDocumentName, List<String> backlinkDocumentNames) throws XWikiException
    {
        rename(newDocumentName, backlinkDocumentNames);
    }

    /**
     * Rename the current document and all the links pointing to it in the list of passed backlink documents. The
     * renaming algorithm takes into account the fact that there are several ways to write a link to a given page and
     * all those forms need to be renamed. For example the following links all point to the same page:
     * <ul>
     * <li>[Page]</li>
     * <li>[Page?param=1]</li>
     * <li>[currentwiki:Page]</li>
     * <li>[CurrentSpace.Page]</li>
     * </ul>
     * <p>
     * Note: links without a space are renamed with the space added.
     * </p>
     * 
     * @param newDocumentName the new document name. If the space is not specified then defaults to the current space.
     * @param backlinkDocumentNames the list of documents to parse and for which links will be modified to point to the
     *            new renamed document.
     * @throws XWikiException in case of an error
     */
    public void rename(String newDocumentName, List<String> backlinkDocumentNames) throws XWikiException
    {
        this.doc.rename(newDocumentName, backlinkDocumentNames, getXWikiContext());
    }

    /**
     * Allow to easily access any revision of a document
     * 
     * @param revision version to access
     * @return Document object
     * @throws XWikiException
     */
    public Document getDocumentRevision(String revision) throws XWikiException
    {
        return new Document(this.context.getWiki().getDocument(this.doc, revision, this.context), this.context);
    }

    /**
     * Allow to easily access the previous revision of a document
     * 
     * @return Document
     * @throws XWikiException
     */
    public Document getPreviousDocument() throws XWikiException
    {
        return getDocumentRevision(getPreviousVersion());
    }

    /**
     * @return is document most recent. false if and only if there are older versions of this document.
     */
    public boolean isMostRecent()
    {
        return this.doc.isMostRecent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return this.doc.toString();
    }

    /**
     * Convert the current document content from its current syntax to the new syntax passed as parameter.
     * 
     * @param targetSyntaxId the syntax to convert to (eg "xwiki/2.0", "xhtml/1.0", etc)
     * @throws XWikiException if an exception occurred during the conversion process
     */
    public boolean convertSyntax(String targetSyntaxId) throws XWikiException
    {
        try {
            getDoc().convertSyntax(targetSyntaxId);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
}
