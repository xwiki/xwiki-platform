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
 * @author jeremi
 * @author sdumitriu
 * @author thomas
 */

package com.xpn.xwiki.doc;


import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.EditForm;
import com.xpn.xwiki.web.ObjectAddForm;
import com.xpn.xwiki.web.PrepareEditForm;
import com.xpn.xwiki.web.Utils;
import org.apache.commons.jrcs.diff.Diff;
import org.apache.commons.jrcs.diff.DifferentiationFailedException;
import org.apache.commons.jrcs.diff.Revision;
import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.Lines;
import org.apache.commons.jrcs.rcs.Version;
import org.apache.commons.jrcs.util.ToString;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ecs.filter.CharacterFilter;
import org.apache.tools.ant.filters.StringInputStream;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.tools.VelocityFormatter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class XWikiDocument {
    private static final Log log = LogFactory.getLog(XWikiDocument.class);

    private String title;
    private String parent;
    private String web;
    private String name;
    private String content;
    private String meta;
    private String format;
    private String creator;
    private String author;
    private String contentAuthor;
    private Archive archive;
    private Date contentUpdateDate;
    private Date updateDate;
    private Date creationDate;
    private Version version;
    private long id = 0;
    private boolean mostRecent = false;
    private boolean isNew = true;
    private String template;
    private String language;
    private String defaultLanguage;
    private int translation;
    private String database;

    // Used to make sure the MetaData String is regenerated
    private boolean isContentDirty = false;
    // Used to make sure the MetaData String is regenerated
    private boolean isMetaDataDirty = false;

    public static final int HAS_ATTACHMENTS = 1;
    public static final int HAS_OBJECTS = 2;
    private static final int HAS_CLASS = 4;

    private int elements = HAS_OBJECTS | HAS_ATTACHMENTS;

    // Meta Data
    private BaseClass xWikiClass;
    private String xWikiClassXML;

    private Map xWikiObjects = new HashMap();

    private List attachmentList;

    // Caching
    private boolean fromCache = false;
    private ArrayList objectsToRemove = new ArrayList();

    // Template by default assign to a view
    private String defaultTemplate;

    private Object wikiNode;

    private XWikiStoreInterface store;

    public XWikiStoreInterface getStore() {
        return store;
    }

    public void setStore(XWikiStoreInterface store) {
        this.store = store;
    }


    public long getId() {
        if ((language == null) || language.trim().equals(""))
            id = getFullName().hashCode();
        else
            id = (getFullName() + ":" + language).hashCode();

        //if (log.isDebugEnabled())
        //    log.debug("ID: " + getFullName() + " " + language + ": " + id);
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public String getVersion() {
        return getRCSVersion().toString();
    }

    public void setVersion(String version) {
        this.version = new Version(version);
    }

    public Version getRCSVersion() {
        if (version == null) {
            version = new Version("1.1");
        }
        return version;
    }

    public void setRCSVersion(Version version) {
        this.version = version;
    }

    public XWikiDocument() {
        this("Main", "WebHome");
    }

    public XWikiDocument(String web, String name) {
        this.web = web;

        int i1 = name.indexOf(".");
        if (i1 == -1) {
            this.name = name;
        } else {
            this.web = name.substring(0, i1);
            this.name = name.substring(i1 + 1);
        }
        this.updateDate = new Date();
        this.creationDate = new Date();
        this.parent = "";
        this.content = "\n";
        this.format = "";
        this.author = "";
        this.language = "";
        this.defaultLanguage = "";
        this.archive = null;
        this.attachmentList = new ArrayList();
    }

    public XWikiDocument getParentDoc() {
        return new XWikiDocument(web, getParent());
    }

    public String getParent() {
        return parent.trim();
    }

    public void setParent(String parent) {
        if (!parent.equals(this.parent)) {
            setMetaDataDirty(true);
        }
        this.parent = parent;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        if (!content.equals(this.content)) {
            setContentDirty(true);
            setWikiNode(null);
        }
        this.content = content;
    }

    public String getRenderedContent(XWikiContext context) throws XWikiException {
        return context.getWiki().getRenderingEngine().renderDocument(this, context);
    }

    public String getRenderedContent(String text, XWikiContext context) {
        return context.getWiki().getRenderingEngine().renderText(text, this, context);
    }

    public String getEscapedContent(XWikiContext context) throws XWikiException {
        CharacterFilter filter = new CharacterFilter();
        return filter.process(getTranslatedContent(context));
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        StringBuffer buf = new StringBuffer();
        buf.append(getWeb());
        buf.append(".");
        buf.append(getName());
        return buf.toString();
    }

    public void setFullName(String name) {
        setFullName(name, null);
    }

    public String getTitle() {
        if (title == null)
            return "";
        else
            return title;
    }

    public String getDisplayTitle() {
        String title = getTitle();
        if (title.equals("")) {
            title = extractTitle();
        }
        if (title.equals(""))
            return getName();
        else
            return title;
    }

    public String extractTitle() {
        try {
            String content = getContent();
            int i1 = 0;
            int i2;

            while (true) {
                i2 = content.indexOf("\n", i1);
                String title = "";
                if (i2 != -1)
                    title = content.substring(i1, i2).trim();
                else
                    title = content.substring(i1).trim();
                if ((!title.equals("")) && (title.matches("1(\\.1)?\\s+.+")))
                    return title.substring(title.indexOf(" ")).trim();
                if (i2 == -1)
                    break;
                i1 = i2 + 1;
            }
        } catch (Exception e) {
        }
        return "";
    }

    public void setTitle(String title) {
        if (title == null)
            title = "";
        this.title = title;
        if (!title.equals(this.title)) {
            setMetaDataDirty(true);
        }
    }

    public String getFormat() {
        if (format == null)
            return "";
        else
            return format;
    }

    public void setFormat(String format) {
        this.format = format;
        if (!format.equals(this.format)) {
            setMetaDataDirty(true);
        }
    }

    public String getAuthor() {
        if (author == null)
            return "";
        else
            return author.trim();
    }

    public String getContentAuthor() {
        if (contentAuthor == null)
            return "";
        else
            return contentAuthor.trim();
    }

    public void setAuthor(String author) {
        if (!getAuthor().equals(this.author)) {
            setMetaDataDirty(true);
        }
        this.author = author;
    }

    public void setContentAuthor(String contentAuthor) {
        if (!getContentAuthor().equals(this.contentAuthor)) {
            setMetaDataDirty(true);
        }
        this.contentAuthor = contentAuthor;
    }

    public String getCreator() {
        if (creator == null)
            return "";
        else
            return creator.trim();
    }

    public void setCreator(String creator) {
        if (!getCreator().equals(this.creator)) {
            setMetaDataDirty(true);
        }
        this.creator = creator;
    }

    public Date getDate() {
        if (updateDate == null)
            return new Date();
        else
            return updateDate;
    }

    public void setDate(Date date) {
        if ((date != null) && (!date.equals(this.updateDate))) {
            setMetaDataDirty(true);
        }
        this.updateDate = date;
    }

    public Date getCreationDate() {
        if (creationDate == null)
            return new Date();
        else
            return creationDate;
    }

    public void setCreationDate(Date date) {
        if ((date != null) && (!creationDate.equals(this.creationDate))) {
            setMetaDataDirty(true);
        }
        this.creationDate = date;
    }

    public Date getContentUpdateDate() {
        if (contentUpdateDate == null)
            return new Date();
        else
            return contentUpdateDate;
    }

    public void setContentUpdateDate(Date date) {
        if ((date != null) && (!date.equals(this.contentUpdateDate))) {
            setMetaDataDirty(true);
        }
        this.contentUpdateDate = date;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        if (meta == null) {
            if (this.meta != null)
                setMetaDataDirty(true);
        } else if (!meta.equals(this.meta)) {
            setMetaDataDirty(true);
        }
        this.meta = meta;
    }

    public void appendMeta(String meta) {
        StringBuffer buf = new StringBuffer(this.meta);
        buf.append(meta);
        buf.append("\n");
        this.meta = buf.toString();
        setMetaDataDirty(true);
    }

    public boolean isContentDirty() {
        return isContentDirty;
    }

    public void incrementVersion() {
        if (version == null)
            version = new Version("1.1");
        else {
            version = version.next();
        }
    }

    public void setContentDirty(boolean contentDirty) {
        isContentDirty = contentDirty;
    }

    public boolean isMetaDataDirty() {
        return isMetaDataDirty;
    }

    public void setMetaDataDirty(boolean metaDataDirty) {
        isMetaDataDirty = metaDataDirty;
    }

    public String getAttachmentURL(String filename, XWikiContext context) {
        return getAttachmentURL(filename, "download", context);
    }

    public String getAttachmentURL(String filename, String action, XWikiContext context) {
        URL url = context.getURLFactory().createAttachmentURL(filename, getWeb(), getName(), action, context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getAttachmentRevisionURL(String filename, String revision, XWikiContext context) {
        URL url = context.getURLFactory().createAttachmentRevisionURL(filename, getWeb(), getName(), revision, context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getURL(String action, boolean redirect, XWikiContext context) {
        URL url = context.getURLFactory().createURL(getWeb(), getName(), action, redirect, context);
        if (redirect) {
            if (url == null)
                return null;
            else
                return url.toString();
        } else
            return context.getURLFactory().getURL(url, context);
    }

    public String getURL(String action, XWikiContext context) {
        return getURL(action, false, context);
    }

    public String getURL(String action, String querystring, XWikiContext context) {
        URL url = context.getURLFactory().createURL(getWeb(), getName(), action,
                querystring, null, context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getExternalURL(String action, XWikiContext context) {
        URL url = context.getURLFactory().createExternalURL(getWeb(), getName(), action,
                null, null, context);
        return url.toString();
    }

    public String getExternalURL(String action, String querystring, XWikiContext context) {
        URL url = context.getURLFactory().createExternalURL(getWeb(), getName(), action,
                querystring, null, context);
        return url.toString();
    }


    public String getParentURL(XWikiContext context) throws XWikiException {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(getParent(), context);
        URL url = context.getURLFactory().createURL(doc.getWeb(), doc.getName(), context);
        return context.getURLFactory().getURL(url, context);
    }

    public Version[] getRevisions(XWikiContext context) throws XWikiException {
        return getStore().getXWikiDocVersions(this, context);
    }

    public String[] getRecentRevisions(int nb, XWikiContext context) throws XWikiException {
        try {
            Version[] revisions = getStore().getXWikiDocVersions(this, context);
            int length = nb;

            // 0 means all revisions
            if (nb == 0)
                length = revisions.length;

            if (revisions.length < length)
                length = revisions.length;

            String[] recentrevs = new String[length];
            for (int i = 1; i <= length; i++)
                recentrevs[i - 1
                        ] = revisions[revisions.length - i].toString();
            return recentrevs;
        } catch (Exception e) {
            return new String[0];
        }
    }

    public Archive getRCSArchive() {
        return archive;
    }

    public void setRCSArchive(Archive archive) {
        this.archive = archive;
    }

    public String getArchive() throws XWikiException {
        return getArchive(null);
    }

    public String getArchive(XWikiContext context) throws XWikiException {
        if ((content == null) || (content.equals("")))
            setContent("\n");
        if (archive == null)
            updateArchive(toXML(context));
        if (archive == null)
            return "";
        else {
            StringBuffer buffer = new StringBuffer();
            archive.toString(buffer);
            return buffer.toString();
        }
    }

    public void setArchive(String text) throws XWikiException {
        try {
            StringInputStream is = new StringInputStream(text);
            archive = new Archive(getFullName(), is);
        }
        catch (Exception e) {
            Object[] args = {getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ARCHIVEFORMAT,
                    "Exception while manipulating the archive for doc {0}", e, args);
        }
    }

    public void updateArchive(String text) throws XWikiException {
        try {
            Lines lines = new Lines(text);
            if (archive != null)
                archive.addRevision(lines.toArray(), "");
            else
                archive = new Archive(lines.toArray(), getFullName(), getVersion());
        }
        catch (Exception e) {
            Object[] args = {getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ARCHIVEFORMAT,
                    "Exception while manipulating the archive for doc {0}", e, args);
        }
    }

    public boolean isMostRecent() {
        return mostRecent;
    }

    public void setMostRecent(boolean mostRecent) {
        this.mostRecent = mostRecent;
    }

    public BaseClass getxWikiClass() {
        if (xWikiClass == null) {
            xWikiClass = new BaseClass();
            xWikiClass.setName(getFullName());
        }
        return xWikiClass;
    }

    public void setxWikiClass(BaseClass xWikiClass) {
        this.xWikiClass = xWikiClass;
    }

    public Map getxWikiObjects() {
        return xWikiObjects;
    }

    public void setxWikiObjects(Map xWikiObjects) {
        this.xWikiObjects = xWikiObjects;
    }

    public BaseObject getxWikiObject() {
        return getObject(getFullName());
    }

    public List getxWikiClasses(XWikiContext context) {
        List list = new ArrayList();
        for (Iterator it = getxWikiObjects().keySet().iterator(); it.hasNext();) {
            String classname = (String) it.next();
            BaseClass bclass = null;
            Vector objects = getObjects(classname);
            for (int i = 0; i < objects.size(); i++) {
                BaseObject obj = (BaseObject) objects.get(i);
                if (obj != null) {
                    bclass = obj.getxWikiClass(context);
                    if (bclass != null)
                        break;
                }
            }
            if (bclass != null)
                list.add(bclass);
        }
        return list;
    }

    public int createNewObject(String classname, XWikiContext context) throws XWikiException {
        BaseObject object = BaseClass.newCustomClassInstance(classname, context);
        object.setName(getFullName());
        object.setClassName(classname);
        Vector objects = getObjects(classname);
        if (objects == null) {
            objects = new Vector();
            setObjects(classname, objects);
        }
        objects.add(object);
        int nb = objects.size() - 1;
        object.setNumber(nb);
        return nb;
    }

    public int getObjectNumbers(String classname) {
        try {
            return ((Vector) getxWikiObjects().get(classname)).size();
        } catch (Exception e) {
            return 0;
        }
    }

    public Vector getObjects(String classname) {
        return (Vector) getxWikiObjects().get(classname);
    }

    public void setObjects(String classname, Vector objects) {
        getxWikiObjects().put(classname, objects);
    }

    public BaseObject getObject(String classname) {
        Vector objects = (Vector) getxWikiObjects().get(classname);
        if (objects == null)
            return null;
        for (int i = 0; i < objects.size(); i++) {
            BaseObject obj = (BaseObject) objects.get(i);
            if (obj != null)
                return obj;
        }
        return null;
    }

    public BaseObject getObject(String classname, int nb) {
        try {
            return (BaseObject) ((Vector) getxWikiObjects().get(classname)).get(nb);
        } catch (Exception e) {
            return null;
        }
    }

    public BaseObject getObject(String classname, String key, String value) {
        return getObject(classname, key, value, false);
    }

    public BaseObject getObject(String classname, String key, String value, boolean failover) {
        try {
            if (value == null) {
                if (failover)
                    return getObject(classname);
                else
                    return null;
            }

            Vector objects = (Vector) getxWikiObjects().get(classname);
            if ((objects == null) || (objects.size() == 0))
                return null;
            for (int i = 0; i < objects.size(); i++) {
                BaseObject obj = (BaseObject) objects.get(i);
                if (obj != null) {
                    if (value.equals(obj.getStringValue(key)))
                        return obj;
                }
            }

            if (failover)
                return getObject(classname);
            else
                return null;
        } catch (Exception e) {
            if (failover)
                return getObject(classname);

            e.printStackTrace();
            return null;
        }
    }


    public void addObject(String classname, BaseObject object) {
        Vector vobj = getObjects(classname);
        if (vobj == null)
            setObject(classname, 0, object);
        else
            setObject(classname, vobj.size(), object);
    }

    public void setObject(String classname, int nb, BaseObject object) {
        Vector objects = null;
        objects = getObjects(classname);
        if (objects == null) {
            objects = new Vector();
            setObjects(classname, objects);
        }
        if (nb >= objects.size()) {
            objects.setSize(nb + 1);
        }
        objects.set(nb, object);
        object.setNumber(nb);
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public void mergexWikiClass(XWikiDocument templatedoc) {
        BaseClass bclass = getxWikiClass();
        BaseClass tbclass = templatedoc.getxWikiClass();
        if (tbclass != null) {
            if (bclass == null) {
                setxWikiClass((BaseClass) tbclass.clone());
            } else {
                getxWikiClass().merge((BaseClass) tbclass.clone());
            }
        }
    }

    public void mergexWikiObjects(XWikiDocument templatedoc) {
        // TODO: look for each object if it already exist and add it if it doesn't
        Iterator itobjects = templatedoc.getxWikiObjects().keySet().iterator();
        while (itobjects.hasNext()) {
            String name = (String) itobjects.next();
            Vector objects = (Vector) getxWikiObjects().get(name);

            if (objects != null) {
                Vector tobjects = (Vector) templatedoc.getxWikiObjects().get(name);
                for (int i = 0; i < tobjects.size(); i++) {
                    {
                        BaseObject bobj = (BaseObject) ((BaseObject) tobjects.get(i)).clone();
                        objects.add(bobj);
                        bobj.setNumber(objects.size() - 1);
                    }
                }
            } else {
                Vector tobjects = (Vector) templatedoc.getObjects(name);
                objects = new Vector();
                for (int i = 0; i < tobjects.size(); i++) {
                    BaseObject bobj1 = (BaseObject) tobjects.get(i);
                    if (bobj1 != null) {
                        BaseObject bobj = (BaseObject) bobj1.clone();
                        objects.add(bobj);
                        bobj.setNumber(objects.size() - 1);
                    }
                }
                getxWikiObjects().put(name, objects);
            }
        }
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String display(String fieldname, String type, BaseObject obj, XWikiContext context) {
        try {
            type = type.toLowerCase();
            StringBuffer result = new StringBuffer();
            PropertyClass pclass = (PropertyClass) obj.getxWikiClass(context).get(fieldname);
            String prefix = obj.getxWikiClass(context).getName() + "_" + obj.getNumber() + "_";

            if (type.equals("view")) {
                pclass.displayView(result, fieldname, prefix, obj, context);
            } else if (type.equals("rendered")) {
                String fcontent = pclass.displayView(fieldname, prefix, obj, context);
                result.append(getRenderedContent(fcontent, context));
            } else if (type.equals("edit")) {
                result.append("{pre}");
                pclass.displayEdit(result, fieldname, prefix, obj, context);
                result.append("{/pre}");
            } else if (type.equals("hidden")) {
                result.append("{pre}");
                pclass.displayHidden(result, fieldname, prefix, obj, context);
                result.append("{/pre}");
            } else if (type.equals("search")) {
                result.append("{pre}");
                pclass.displaySearch(result, fieldname, prefix, obj, context);
                result.append("{/pre}");
            } else {
                pclass.displayView(result, fieldname, prefix, obj, context);
            }
            return result.toString();
        }
        catch (Exception e) {
            return "";
            // return "||Exception showing field " + fieldname + ": " + e.getMessage() + "||";
        }
    }

    public String display(String fieldname, BaseObject obj, XWikiContext context) {
        String type = null;
        try {
            type = (String) context.get("display");
        }
        catch (Exception e) {
        }
        ;
        if (type == null)
            type = "view";
        return display(fieldname, type, obj, context);
    }

    public String display(String fieldname, XWikiContext context) {
        try {
            BaseObject object = getxWikiObject();
            if (object == null)
                object = getFirstObject(fieldname);
            return display(fieldname, object, context);
        } catch (Exception e) {
            return "";
        }
    }

    public String display(String fieldname, String mode, XWikiContext context) {
        try {
            BaseObject object = getxWikiObject();
            if (object == null)
                object = getFirstObject(fieldname);
            if (object == null)
                return "";
            else
                return display(fieldname, mode, object, context);
        } catch (Exception e) {
            return "";
        }
    }

    public String displayForm(String className, String header, String format, XWikiContext context) {
        return displayForm(className, header, format, true, context);
    }

    public String displayForm(String className, String header, String format, boolean linebreak, XWikiContext context) {
        Vector objects = getObjects(className);
        if (format.endsWith("\\n"))
            linebreak = true;

        BaseObject firstobject = null;
        Iterator foit = objects.iterator();
        while ((firstobject == null) && foit.hasNext()) {
            firstobject = (BaseObject) foit.next();
        }

        if (firstobject == null)
            return "";

        BaseClass bclass = firstobject.getxWikiClass(context);
        Collection fields = bclass.getFieldList();
        if (fields.size() == 0)
            return "";

        StringBuffer result = new StringBuffer();
        XWikiVelocityRenderer renderer = new XWikiVelocityRenderer();
        VelocityContext vcontext = new VelocityContext();
        vcontext.put("formatter", new VelocityFormatter(vcontext));
        for (Iterator it = fields.iterator(); it.hasNext();) {
            PropertyClass pclass = (PropertyClass) it.next();
            vcontext.put(pclass.getName(), pclass.getPrettyName());
        }
        result.append(XWikiVelocityRenderer.evaluate(header, context.getDoc().getFullName(), vcontext, context));
        if (linebreak)
            result.append("\n");

        // display each line
        for (int i = 0; i < objects.size(); i++) {
            vcontext.put("id", new Integer(i + 1));
            BaseObject object = (BaseObject) objects.get(i);
            if (object != null) {
                for (Iterator it = bclass.getPropertyList().iterator(); it.hasNext();) {
                    String name = (String) it.next();
                    vcontext.put(name, display(name, object, context));
                }
                result.append(XWikiVelocityRenderer.evaluate(format, context.getDoc().getFullName(), vcontext, context));
                if (linebreak)
                    result.append("\n");
            }
        }
        return result.toString();
    }

    public String displayForm(String className, XWikiContext context) {
        Vector objects = getObjects(className);
        if (objects == null)
            return "";

        BaseObject firstobject = null;
        Iterator foit = objects.iterator();
        while ((firstobject == null) && foit.hasNext()) {
            firstobject = (BaseObject) foit.next();
        }

        if (firstobject == null)
            return "";

        BaseClass bclass = firstobject.getxWikiClass(context);
        Collection fields = bclass.getFieldList();
        if (fields.size() == 0)
            return "";

        StringBuffer result = new StringBuffer();
        result.append("{table}\n");
        boolean first = true;
        for (Iterator it = fields.iterator(); it.hasNext();) {
            if (first == true)
                first = false;
            else
                result.append("|");
            PropertyClass pclass = (PropertyClass) it.next();
            result.append(pclass.getPrettyName());
        }
        result.append("\n");
        for (int i = 0; i < objects.size(); i++) {
            BaseObject object = (BaseObject) objects.get(i);
            if (object != null) {
                first = true;
                for (Iterator it = bclass.getPropertyList().iterator(); it.hasNext();) {
                    if (first == true)
                        first = false;
                    else
                        result.append("|");
                    String data = display((String) it.next(), object, context);
                    if (data.trim().equals(""))
                        result.append("&nbsp;");
                    else
                        result.append(data);
                }
                result.append("\n");
            }
        }
        result.append("{table}\n");
        return result.toString();
    }


    public boolean isFromCache() {
        return fromCache;
    }

    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }

    public void readFromForm(EditForm eform, XWikiContext context) throws XWikiException {
        String content = eform.getContent();
        if ((content != null) && (!content.equals(""))) {
            // Cleanup in case we use HTMLAREA
            // content = context.getUtil().substitute("s/<br class=\\\"htmlarea\\\"\\/>/\\r\\n/g", content);
            content = context.getUtil().substitute("s/<br class=\"htmlarea\" \\/>/\r\n/g", content);
            setContent(content);
        }
        String parent = eform.getParent();
        if (parent != null)
            setParent(parent);

        String title = eform.getTitle();
        if (title != null)
            setTitle(title);

        String creator = eform.getCreator();
        if ((creator != null) && (!creator.equals(getCreator()))) {
            if ((getCreator().equals(context.getUser()))
                    || (context.getWiki().getRightService().hasAdminRights(context)))
                setCreator(creator);
        }

        String defaultLanguage = eform.getDefaultLanguage();
        if (defaultLanguage != null)
            setDefaultLanguage(defaultLanguage);

        String defaultTemplate = eform.getDefaultTemplate();
        if (defaultTemplate != null)
            setDefaultTemplate(defaultTemplate);

        // This is now done before
        // readFromTemplate(eform, context);

        Iterator itobj = getxWikiObjects().keySet().iterator();
        while (itobj.hasNext()) {
            String name = (String) itobj.next();
            Vector bobjects = getObjects(name);
            Vector newobjects = new Vector();
            newobjects.setSize(bobjects.size());
            for (int i = 0; i < bobjects.size(); i++) {
                BaseObject oldobject = (BaseObject) getObject(name, i);
                if (oldobject != null) {
                    BaseClass baseclass = oldobject.getxWikiClass(context);
                    BaseObject newobject = (BaseObject) baseclass.fromMap(eform.getObject(baseclass.getName() + "_" + i), oldobject);
                    newobject.setNumber(oldobject.getNumber());
                    newobject.setName(getFullName());
                    newobjects.set(newobject.getNumber(), newobject);
                }
            }
            getxWikiObjects().put(name, newobjects);
        }
    }

    /*
    public void readFromTemplate(EditForm eform, XWikiContext context) throws XWikiException {
    // Get the class from the template
    String template = eform.getTemplate();
    if ((template!=null)&&(!template.equals(""))) {
    if (template.indexOf('.')==-1) {
    template = getWeb() + "." + template;
    }
    XWiki xwiki = context.getWiki();
    XWikiDocument templatedoc = xwiki.getDocument(template, context);
    if (templatedoc.isNew()) {
    Object[] args = { template, getFullName() };
    throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_APP_TEMPLATE_DOES_NOT_EXIST,
    "Template document {0} does not exist when adding to document {1}", null, args);
    } else {
    setTemplate(template);
    mergexWikiObjects(templatedoc);
    }
    }
    }
    */

    public void readFromTemplate(PrepareEditForm eform, XWikiContext context) throws XWikiException {
        String template = eform.getTemplate();
        readFromTemplate(template, context);
    }

    public void readFromTemplate(String template, XWikiContext context) throws XWikiException {
        if ((template != null) && (!template.equals(""))) {
            String content = getContent();
            if ((!content.equals("\n")) && (!content.equals("")) && !isNew()) {
                Object[] args = {getFullName()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY,
                        "Cannot add a template to document {0} because it already has content", null, args);
            } else {

                if (template.indexOf('.') == -1) {
                    template = getWeb() + "." + template;
                }
                XWiki xwiki = context.getWiki();
                XWikiDocument templatedoc = xwiki.getDocument(template, context);
                if (templatedoc.isNew()) {
                    Object[] args = {template, getFullName()};
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_APP_TEMPLATE_DOES_NOT_EXIST,
                            "Template document {0} does not exist when adding to document {1}", null, args);
                } else {
                    setTemplate(template);
                    setContent(templatedoc.getContent());
                    if ((getParent() == null) || (getParent().equals(""))) {
                        String tparent = templatedoc.getParent();
                        if (tparent != null)
                            setParent(tparent);
                    }

                    if (isNew()) {
                        // We might have received the object from the cache
                        // and the templace objects might have been copied already
                        // we need to remove them
                        setxWikiObjects(new HashMap());
                    }
                    // Merge the external objects
                    // Currently the choice is not to merge the base class and object because it is not
                    // the prefered way of using external classes and objects.
                    mergexWikiObjects(templatedoc);
                }
            }
        }
    }

    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event, XWikiContext context) {
        // Do nothing for the moment..
        // A usefull thing here would be to look at any instances of a Notification Object
        // with email addresses and send an email to warn that the document has been modified..

    }

    public Object clone() {
        XWikiDocument doc = null;
        try {
            doc = (XWikiDocument) getClass().newInstance();
        } catch (Exception e) {
            // This should not happen
        }

        doc.setRCSArchive(getRCSArchive());
        doc.setRCSVersion(getRCSVersion());
        doc.setAuthor(getAuthor());
        doc.setContentAuthor(getContentAuthor());
        doc.setContent(getContent());
        doc.setContentDirty(isContentDirty());
        doc.setDate(getDate());
        doc.setContentUpdateDate(getContentUpdateDate());
        doc.setTitle(getTitle());
        doc.setFormat(getFormat());
        doc.setFromCache(isFromCache());
        doc.setElements(getElements());
        doc.setId(getId());
        doc.setMeta(getMeta());
        doc.setMetaDataDirty(isMetaDataDirty());
        doc.setMostRecent(isMostRecent());
        doc.setName(getName());
        doc.setNew(isNew());
        doc.setStore(getStore());
        doc.setTemplate(getTemplate());
        doc.setWeb(getWeb());
        doc.setParent(getParent());
        doc.setCreator(getCreator());
        doc.setDefaultLanguage(getDefaultLanguage());
        doc.setDefaultTemplate(getDefaultTemplate());
        doc.setLanguage(getLanguage());
        doc.setTranslation(getTranslation());
        doc.setxWikiClass((BaseClass) getxWikiClass().clone());
        doc.setxWikiClassXML(getxWikiClassXML());
        doc.mergexWikiObjects(this);
        doc.copyAttachments(this);
        return doc;
    }

    public void copyAttachments(XWikiDocument xWikiSourceDocument) {
        Iterator attit = xWikiSourceDocument.getAttachmentList().iterator();
        while (attit.hasNext()) {
            XWikiAttachment attachment = (XWikiAttachment) attit.next();
            XWikiAttachment newattachment = (XWikiAttachment) attachment.clone();
            newattachment.setDoc(this);
            getAttachmentList().add(newattachment);
        }
    }

    public void loadAttachments(XWikiContext context) throws XWikiException {
        Iterator attit = getAttachmentList().iterator();
        while (attit.hasNext()) {
            XWikiAttachment attachment = (XWikiAttachment) attit.next();
            attachment.loadContent(context);
            attachment.loadArchive(context);
        }
    }

    public boolean equals(Object object) {
        XWikiDocument doc = (XWikiDocument) object;
        if (!getName().equals(doc.getName()))
            return false;

        if (!getWeb().equals(doc.getWeb()))
            return false;

        if (!getAuthor().equals(doc.getAuthor()))
            return false;

        if (!getContentAuthor().equals(doc.getContentAuthor()))
            return false;

        if (!getParent().equals(doc.getParent()))
            return false;

        if (!getCreator().equals(doc.getCreator()))
            return false;

        if (!getDefaultLanguage().equals(doc.getDefaultLanguage()))
            return false;

        if (!getLanguage().equals(doc.getLanguage()))
            return false;

        if (getTranslation() != doc.getTranslation())
            return false;

        if (getDate().getTime() != doc.getDate().getTime())
            return false;

        if (getContentUpdateDate().getTime() != doc.getContentUpdateDate().getTime())
            return false;

        if (getCreationDate().getTime() != doc.getCreationDate().getTime())
            return false;

        if (!getFormat().equals(doc.getFormat()))
            return false;

        if (!getTitle().equals(doc.getTitle()))
            return false;

        if (!getContent().equals(doc.getContent()))
            return false;

        if (!getVersion().equals(doc.getVersion()))
            return false;

        try {
            if (!getArchive().equals(doc.getArchive()))
                return false;
        } catch (XWikiException e) {
            return false;
        }

        if (!getxWikiClass().equals(doc.getxWikiClass()))
            return false;

        Set list1 = getxWikiObjects().keySet();
        Set list2 = doc.getxWikiObjects().keySet();
        if (!list1.equals(list2))
            return false;

        for (Iterator it = list1.iterator(); it.hasNext();) {
            String name = (String) it.next();
            Vector v1 = getObjects(name);
            Vector v2 = doc.getObjects(name);
            if (v1.size() != v2.size())
                return false;
            for (int i = 0; i < v1.size(); i++) {
                if ((v1.get(i) == null) && (v2.get(i) != null))
                    return false;
                if (!v1.get(i).equals(v2.get(i)))
                    return false;
            }
        }

        return true;
    }

    public String toXML(Document doc, XWikiContext context) {
        OutputFormat outputFormat = new OutputFormat("", true);
        if ((context == null) || (context.getWiki() == null))
            outputFormat.setEncoding("UTF-8");
        else
            outputFormat.setEncoding(context.getWiki().getEncoding());
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter(out, outputFormat);
        try {
            writer.write(doc);
            return out.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getXMLContent(XWikiContext context) throws XWikiException {
        XWikiDocument tdoc = getTranslatedDocument(context);
        Document doc = tdoc.toXMLDocument(true, true, false, false, context);
        return toXML(doc, context);
    }

    public String toXML(XWikiContext context) throws XWikiException {
        Document doc = toXMLDocument(context);
        return toXML(doc, context);
    }

    public String toFullXML(XWikiContext context) throws XWikiException {
        return toXML(true, false, true, true, context);
    }

    public void addToZip(ZipOutputStream zos, boolean withVersions, XWikiContext context) throws IOException {
        try {
            String zipname = getWeb() + "/" + getName();
            String language = getLanguage();
            if ((language != null) && (!language.equals("")))
                zipname += "." + language;
            ZipEntry zipentry = new ZipEntry(zipname);
            zos.putNextEntry(zipentry);
            zos.write(toXML(true, false, true, withVersions, context).getBytes());
            zos.closeEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addToZip(ZipOutputStream zos, XWikiContext context) throws IOException {
        addToZip(zos, true, context);
    }


    public String toXML(boolean bWithObjects, boolean bWithRendering,
                        boolean bWithAttachmentContent,
                        boolean bWithVersions,
                        XWikiContext context) throws XWikiException {
        Document doc = toXMLDocument(bWithObjects, bWithRendering,
                bWithAttachmentContent, bWithVersions, context);
        return toXML(doc, context);
    }

    public Document toXMLDocument(XWikiContext context) throws XWikiException {
        return toXMLDocument(true, false, false, false, context);
    }

    public Document toXMLDocument(boolean bWithObjects, boolean bWithRendering,
                                  boolean bWithAttachmentContent,
                                  boolean bWithVersions,
                                  XWikiContext context) throws XWikiException {
        Document doc = new DOMDocument();
        Element docel = new DOMElement("xwikidoc");
        doc.setRootElement(docel);

        Element el = new DOMElement("web");
        el.addText(getWeb());
        docel.add(el);

        el = new DOMElement("name");
        el.addText(getName());
        docel.add(el);

        el = new DOMElement("language");
        el.addText(getLanguage());
        docel.add(el);

        el = new DOMElement("defaultLanguage");
        el.addText(getDefaultLanguage());
        docel.add(el);

        el = new DOMElement("translation");
        el.addText("" + getTranslation());
        docel.add(el);

        el = new DOMElement("parent");
        el.addText(getParent());
        docel.add(el);

        el = new DOMElement("author");
        el.addText(getAuthor());
        docel.add(el);

        long d = getCreationDate().getTime();
        el = new DOMElement("creationDate");
        el.addText("" + d);
        docel.add(el);

        d = getDate().getTime();
        el = new DOMElement("date");
        el.addText("" + d);
        docel.add(el);

        el = new DOMElement("version");
        el.addText(getVersion());
        docel.add(el);

        List alist = getAttachmentList();
        for (int ai = 0; ai < alist.size(); ai++) {
            XWikiAttachment attach = (XWikiAttachment) alist.get(ai);
            docel.add(attach.toXML(bWithAttachmentContent, bWithVersions, context));
        }

        if (bWithObjects) {
            // Add Class
            BaseClass bclass = getxWikiClass();
            if (bclass.getFieldList().size() > 0) {
                docel.add(bclass.toXML(null));
            }

            // Add Objects
            Iterator it = getxWikiObjects().values().iterator();
            while (it.hasNext()) {
                Vector objects = (Vector) it.next();
                for (int i = 0; i < objects.size(); i++) {
                    BaseObject obj = (BaseObject) objects.get(i);
                    if (obj != null) {
                        BaseClass objclass = null;
                        if (obj.getName().equals(obj.getClassName()))
                            objclass = bclass;
                        else
                            objclass = obj.getxWikiClass(context);
                        docel.add(obj.toXML(objclass));
                    }
                }
            }
        }

        // Add Content
        el = new DOMElement("content");

        //Filter filter = new CharacterFilter();
        //String newcontent = filter.process(getContent());
        //String newcontent = encodedXMLStringAsUTF8(getContent());
        String newcontent = content;
        el.addText(newcontent);
        docel.add(el);

        if (bWithRendering) {
            el = new DOMElement("renderedcontent");
            try {
                el.addText(getRenderedContent(context));
            } catch (XWikiException e) {
                el.addText("Exception with rendering content: " + e.getFullMessage());
            }
            docel.add(el);
        }

        if (bWithVersions) {
            el = new DOMElement("versions");
            try {
                el.addText(getArchive());
            } catch (XWikiException e) {
                return null;
            }
            docel.add(el);
        }

        return doc;
    }

    protected String encodedXMLStringAsUTF8(String xmlString) {
        if (xmlString == null) {
            return "";
        }
        int length = xmlString.length();
        char character;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < length; i++) {
            character = xmlString.charAt(i);
            switch (character) {
                case '&':
                    result.append("&amp;");
                    break;
                case '"':
                    result.append("&quot;");
                    break;
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '\n':
                    result.append("\n");
                    break;
                case '\r':
                    result.append("\r");
                    break;
                case '\t':
                    result.append("\t");
                    break;
                default:
                    if (character < 0x20) {
                    } else if (character > 0x7F) {
                        result.append("&#x");
                        result.append(Integer.toHexString(character).toUpperCase());
                        result.append(";");
                    } else {
                        result.append(character);
                    }
                    break;
            }
        }
        return result.toString();
    }

    protected String getElement(Element docel, String name) {
        Element el = docel.element(name);
        if (el == null)
            return "";
        else
            return el.getText();
    }

    public void fromXML(String xml) throws XWikiException {
        fromXML(xml, false);
    }


    public void fromXML(String xml, boolean withArchive) throws XWikiException {
        SAXReader reader = new SAXReader();
        Document domdoc;

        try {
            StringReader in = new StringReader(xml);
            domdoc = reader.read(in);
        } catch (DocumentException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING, "Error parsing xml", e, null);
        }

        Element docel = domdoc.getRootElement();
        setName(getElement(docel, "name"));
        setWeb(getElement(docel, "web"));
        setParent(getElement(docel, "parent"));
        setCreator(getElement(docel, "creator"));
        setAuthor(getElement(docel, "author"));
        setVersion(getElement(docel, "version"));
        setContent(getElement(docel, "content"));
        setLanguage(getElement(docel, "language"));
        setDefaultLanguage(getElement(docel, "defaultLanguage"));

        String strans = getElement(docel, "translation");
        if ((strans == null) || strans.equals(""))
            setTranslation(0);
        else
            setTranslation(Integer.parseInt(strans));

        String archive = getElement(docel, "versions");
        if (withArchive && archive != null && archive.length() > 0) {
            setArchive(archive);
        }


        String sdate = getElement(docel, "date");
        if (!sdate.equals("")) {
            Date date = new Date(Long.parseLong(sdate));
            setDate(date);
        }

        String scdate = getElement(docel, "creationDate");
        if (!scdate.equals("")) {
            Date cdate = new Date(Long.parseLong(scdate));
            setCreationDate(cdate);
        }

        List atels = docel.elements("attachment");
        for (int i = 0; i < atels.size(); i++) {
            Element atel = (Element) atels.get(i);
            XWikiAttachment attach = new XWikiAttachment();
            attach.setDoc(this);
            attach.fromXML(atel);
            getAttachmentList().add(attach);
        }

        Element cel = docel.element("class");
        BaseClass bclass = new BaseClass();
        if (cel != null) {
            bclass.fromXML(cel);
            setxWikiClass(bclass);
        }

        List objels = docel.elements("object");
        for (int i = 0; i < objels.size(); i++) {
            Element objel = (Element) objels.get(i);
            BaseObject bobject = new BaseObject();
            bobject.fromXML(objel);
            addObject(bobject.getClassName(), bobject);
        }
    }

    public void setAttachmentList(List list) {
        attachmentList = list;
    }

    public List getAttachmentList() {
        return attachmentList;
    }

    public void saveAllAttachments(XWikiContext context) throws XWikiException {
        for (int i = 0; i < attachmentList.size(); i++) {
            saveAttachmentContent((XWikiAttachment) attachmentList.get(i), context);
        }
    }

    public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context) throws XWikiException {
        String database = context.getDatabase();
        try {
            // We might need to switch database to
            // get the translated content
            if (getDatabase() != null)
                context.setDatabase(getDatabase());

            getStore().saveAttachmentContent(attachment, context, true);
        } finally {
            if (database != null)
                context.setDatabase(database);
        }
    }

    public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context) throws XWikiException {
        String database = context.getDatabase();
        try {
            // We might need to switch database to
            // get the translated content
            if (getDatabase() != null)
                context.setDatabase(getDatabase());

            getStore().loadAttachmentContent(attachment, context, true);
        } finally {
            if (database != null)
                context.setDatabase(database);
        }
    }

    public void deleteAttachment(XWikiAttachment attachment, XWikiContext context) throws XWikiException {
        String database = context.getDatabase();
        try {
            // We might need to switch database to
            // get the translated content
            if (getDatabase() != null)
                context.setDatabase(getDatabase());

            getStore().deleteXWikiAttachment(attachment, context, true);
        } finally {
            if (database != null)
                context.setDatabase(database);
        }
    }

    public List getBacklinks(XWikiContext context) throws XWikiException {
        return getStore().loadBacklinks(getFullName(), context, true);
    }

    public List getLinks(XWikiContext context) throws XWikiException {
        return getStore().loadLinks(getId(), context, true);
    }

    public void renameProperties(String className, Map fieldsToRename) {
        Vector objects = getObjects(className);
        if (objects == null)
            return;
        for (int j = 0; j < objects.size(); j++) {
            BaseObject bobject = (BaseObject) objects.get(j);
            if (bobject == null)
                continue;
            for (Iterator renameit = fieldsToRename.keySet().iterator(); renameit.hasNext();) {
                String origname = (String) renameit.next();
                String newname = (String) fieldsToRename.get(origname);
                BaseProperty origprop = (BaseProperty) bobject.safeget(origname);
                if (origprop != null) {
                    BaseProperty prop = (BaseProperty) origprop.clone();
                    bobject.removeField(origname);
                    prop.setName(newname);
                    bobject.addField(newname, prop);
                }
            }
        }
    }

    public void addObjectsToRemove(BaseObject object) {
        getObjectsToRemove().add(object);
    }

    public ArrayList getObjectsToRemove() {
        return objectsToRemove;
    }

    public void setObjectsToRemove(ArrayList objectsToRemove) {
        this.objectsToRemove = objectsToRemove;
    }

    public List getIncludedPages(XWikiContext context) {
        try {
            String pattern = "#include(Topic|Form|Macros)\\(\"(.*?)\"\\)";
            List list = context.getUtil().getMatches(getContent(), pattern, 2);
            for (int i = 0; i < list.size(); i++) {
                try {
                    String name = (String) list.get(i);
                    if (name.indexOf(".") == -1) {
                        list.set(i, getWeb() + "." + name);
                    }
                } catch (Exception e) {
                    // This should never happen
                    e.printStackTrace();
                    return null;
                }
            }

            return list;
        } catch (Exception e) {
            // This should never happen
            e.printStackTrace();
            return null;
        }
    }

    public List getIncludedMacros(XWikiContext context) {
        return context.getWiki().getIncludedMacros(getWeb(), getContent(), context);
    }

    public List getLinkedPages(XWikiContext context) {
        try {
            String pattern = "\\[(.*?)\\]";
            List newlist = new ArrayList();
            List list = context.getUtil().getMatches(getContent(), pattern, 1);
            for (int i = 0; i < list.size(); i++) {
                try {
                    String name = (String) list.get(i);
                    int i1 = name.indexOf(">");
                    if (i1 != -1) {
                        name = name.substring(i1 + 1);
                    }
                    i1 = name.indexOf("&gt;");
                    if (i1 != -1) {
                        name = name.substring(i1 + 4);
                    }
                    i1 = name.indexOf("#");
                    if (i1 != -1) {
                        name = name.substring(0, i1);
                    }
                    i1 = name.indexOf("?");
                    if (i1 != -1) {
                        name = name.substring(0, i1);
                    }

                    // Let's get rid of anything that's not a real link
                    if (name.trim().equals("") || (name.indexOf("$") != -1) || (name.indexOf("://") != -1)
                            || (name.indexOf("\"") != -1) || (name.indexOf("\'") != -1)
                            || (name.indexOf("..") != -1) || (name.indexOf(":") != -1) || (name.indexOf("=") != -1))
                        continue;

                    // generate the link
                    Util util = context.getUtil();
                    String newname = StringUtils.replace(util.noaccents(name), " ", "");

                    // If it is a local link let's add the space
                    if (newname.indexOf(".") == -1) {
                        newname = getWeb() + "." + name;
                    }
                    if (context.getWiki().exists(newname, context)) {
                        name = newname;
                    } else {
                        // If it is a local link let's add the space
                        if (name.indexOf(".") == -1) {
                            name = getWeb() + "." + name;
                        }
                    }

                    // Let's finally ignore the autolinks
                    if (!name.equals(getFullName()))
                        newlist.add(name);

                } catch (Exception e) {
                    // This should never happen
                    e.printStackTrace();
                    return null;
                }
            }

            return newlist;
        } catch (Exception e) {
            // This should never happen
            e.printStackTrace();
            return null;
        }
    }

    public String displayRendered(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context) {
        String result = pclass.displayView(pclass.getName(), prefix, object, context);
        return getRenderedContent(result, context);
    }

    public String displayView(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context) {
        return (pclass == null) ? "" : pclass.displayView(pclass.getName(), prefix, object, context);
    }

    public String displayEdit(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context) {
        return (pclass == null) ? "" : pclass.displayEdit(pclass.getName(), prefix, object, context);
    }

    public String displayHidden(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context) {
        return (pclass == null) ? "" : pclass.displayHidden(pclass.getName(), prefix, object, context);
    }

    public String displaySearch(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context) {
        return (pclass == null) ? "" : pclass.displaySearch(pclass.getName(), prefix, object, context);
    }

    public XWikiAttachment getAttachment(String filename) {
        List list = getAttachmentList();
        for (int i = 0; i < list.size(); i++) {
            XWikiAttachment attach = (XWikiAttachment) list.get(i);
            if (attach.getFilename().equals(filename)) {
                return attach;
            }
        }
        for (int i = 0; i < list.size(); i++) {
            XWikiAttachment attach = (XWikiAttachment) list.get(i);
            if (attach.getFilename().startsWith(filename + ".")) {
                return attach;
            }
        }
        return null;
    }

    public BaseObject getFirstObject(String fieldname) {
        Collection objectscoll = getxWikiObjects().values();
        if (objectscoll == null)
            return null;

        for (Iterator itobjs = objectscoll.iterator(); itobjs.hasNext();) {
            Vector objects = (Vector) itobjs.next();
            for (Iterator itobjs2 = objects.iterator(); itobjs2.hasNext();) {
                BaseObject obj = (BaseObject) itobjs2.next();
                if (obj != null) {
                    Set set = obj.getPropertyList();
                    if ((set != null) && set.contains(fieldname))
                        return obj;
                }
            }
        }
        return null;
    }

    public int getIntValue(String className, String fieldName) {
        BaseObject obj = getObject(className, 0);
        if (obj == null)
            return 0;
        return obj.getIntValue(fieldName);
    }

    public long getLongValue(String className, String fieldName) {
        BaseObject obj = getObject(className, 0);
        if (obj == null)
            return 0;
        return obj.getLongValue(fieldName);
    }

    public String getStringValue(String className, String fieldName) {
        BaseObject obj = getObject(className);
        if (obj == null)
            return "";
        String result = obj.getStringValue(fieldName);
        if (result.equals(" "))
            return "";
        else
            return result;
    }

    public int getIntValue(String fieldName) {
        BaseObject object = getFirstObject(fieldName);
        if (object == null)
            return 0;
        else
            return object.getIntValue(fieldName);
    }

    public long getLongValue(String fieldName) {
        BaseObject object = getFirstObject(fieldName);
        if (object == null)
            return 0;
        else
            return object.getLongValue(fieldName);
    }

    public String getStringValue(String fieldName) {
        BaseObject object = getFirstObject(fieldName);
        if (object == null)
            return "";

        String result = object.getStringValue(fieldName);
        if (result.equals(" "))
            return "";
        else
            return result;
    }


    public void setStringValue(String className, String fieldName, String value) {
        BaseObject bobject = getObject(className);
        if (bobject == null) {
            bobject = new BaseObject();
            addObject(className, bobject);
        }
        bobject.setName(getFullName());
        bobject.setClassName(className);
        bobject.setStringValue(fieldName, value);
    }

    public void setLargeStringValue(String className, String fieldName, String value) {
        BaseObject bobject = getObject(className);
        if (bobject == null) {
            bobject = new BaseObject();
            addObject(className, bobject);
        }
        bobject.setName(getFullName());
        bobject.setClassName(className);
        bobject.setLargeStringValue(fieldName, value);
    }

    public void setIntValue(String className, String fieldName, int value) {
        BaseObject bobject = getObject(className);
        if (bobject == null) {
            bobject = new BaseObject();
            addObject(className, bobject);
        }
        bobject.setName(getFullName());
        bobject.setClassName(className);
        bobject.setIntValue(fieldName, value);
    }


    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }


    public void setFullName(String fullname, XWikiContext context) {
        if (fullname == null)
            return;

        int i0 = fullname.lastIndexOf(":");
        int i1 = fullname.lastIndexOf(".");

        if (i0 != -1) {
            database = fullname.substring(0, i0);
            web = fullname.substring(i0 + 1, i1);
            name = fullname.substring(i1 + 1);
        } else {
            if (i1 == -1) {
                try {
                    web = context.getDoc().getWeb();
                } catch (Exception e) {
                    web = "XWiki";
                }
                name = fullname;
            } else {
                web = fullname.substring(0, i1);
                name = fullname.substring(i1 + 1);
            }
        }

        if (name.equals(""))
            name = "WebHome";
    }

    public String getLanguage() {
        if (language == null)
            return "";
        else
            return language.trim();
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDefaultLanguage() {
        if (defaultLanguage == null)
            return "";
        else
            return defaultLanguage.trim();
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public int getTranslation() {
        return translation;
    }

    public void setTranslation(int translation) {
        this.translation = translation;
    }

    public String getTranslatedContent(XWikiContext context) throws XWikiException {
        String language = context.getWiki().getLanguagePreference(context);
        return getTranslatedContent(language, context);
    }

    public String getTranslatedContent(String language, XWikiContext context) throws XWikiException {
        XWikiDocument tdoc = getTranslatedDocument(language, context);
        String rev = (String) context.get("rev");
        if ((rev == null) || (rev.length() == 0))
            return tdoc.getContent();

        XWikiDocument cdoc = context.getWiki().getDocument(tdoc, rev, context);
        return cdoc.getContent();
    }

    public XWikiDocument getTranslatedDocument(XWikiContext context) throws XWikiException {
        String language = context.getWiki().getLanguagePreference(context);
        return getTranslatedDocument(language, context);
    }

    public XWikiDocument getTranslatedDocument(String language, XWikiContext context) throws XWikiException {
        XWikiDocument tdoc = this;

        if (!((language == null) || (language.equals("")) || language.equals(defaultLanguage))) {
            tdoc = new XWikiDocument(getWeb(), getName());
            tdoc.setLanguage(language);
            String database = context.getDatabase();
            try {
                // We might need to switch database to
                // get the translated content
                if (getDatabase() != null)
                    context.setDatabase(getDatabase());

                tdoc = context.getWiki().getStore().loadXWikiDoc(tdoc, context);

                if (tdoc.isNew())
                    tdoc = this;
            } catch (Exception e) {
                tdoc = this;
            } finally {
                context.setDatabase(database);
            }
        }
        return tdoc;
    }


    public String getRealLanguage(XWikiContext context) throws XWikiException {
        String lang = getLanguage();
        if ((lang.equals("") || lang.equals("default")))
            return getDefaultLanguage();
        else
            return lang;
    }

    public List getTranslationList(XWikiContext context) throws XWikiException {
        List result = new ArrayList();
        String hql = "select doc.language from XWikiDocument as doc where doc.web = '"
                + Utils.SQLFilter(getWeb()) + "' and doc.name = '" + Utils.SQLFilter(getName()) + "' and doc.language <> ''";

        List list = context.getWiki().search(hql, context);
        if ((list == null) || (list.size() == 0)) {
            return result;
        }

        for (int i = 0; i < list.size(); i++) {
            result.add(list.get(i));
        }
        return result;
    }

    public List getXMLDiff(XWikiDocument origdoc, XWikiDocument newdoc, XWikiContext context) throws XWikiException, DifferentiationFailedException {
        return getDeltas(Diff.diff(ToString.stringToArray(origdoc.toXML(context)), ToString.stringToArray(newdoc.toXML(context))));
    }

    public List getContentDiff(XWikiDocument origdoc, XWikiDocument newdoc, XWikiContext context) throws XWikiException, DifferentiationFailedException {
        return getDeltas(Diff.diff(ToString.stringToArray(origdoc.getContent()), ToString.stringToArray(newdoc.getContent())));
    }

    public List getContentDiff(String origrev, String newrev, XWikiContext context) throws XWikiException, DifferentiationFailedException {
        XWikiDocument origdoc = context.getWiki().getDocument(this, origrev, context);
        XWikiDocument newdoc = context.getWiki().getDocument(this, newrev, context);
        return getContentDiff(origdoc, newdoc, context);
    }

    public List getContentDiff(String rev, XWikiContext context) throws XWikiException, DifferentiationFailedException {
        XWikiDocument revdoc = context.getWiki().getDocument(this, rev, context);
        return getContentDiff(this, revdoc, context);
    }

    public List getLastChanges(XWikiContext context) throws XWikiException, DifferentiationFailedException {
        Version version = getRCSVersion();
        String prev = "1." + (version.last() - 1);
        XWikiDocument prevdoc = context.getWiki().getDocument(this, prev, context);

        return getDeltas(Diff.diff(ToString.stringToArray(getContent()),
                ToString.stringToArray(prevdoc.getContent())));
    }


    public List getRenderedContentDiff(XWikiDocument origdoc, XWikiDocument newdoc, XWikiContext context) throws XWikiException, DifferentiationFailedException {
        String content1, content2;

        content1 = context.getWiki().getRenderingEngine().renderText(origdoc.getContent(), origdoc, context);
        content2 = context.getWiki().getRenderingEngine().renderText(newdoc.getContent(), newdoc, context);

        return getDeltas(Diff.diff(ToString.stringToArray(content1),
                ToString.stringToArray(content2)));
    }

    public List getRenderedContentDiff(String origrev, String newrev, XWikiContext context) throws XWikiException, DifferentiationFailedException {
        XWikiDocument origdoc = context.getWiki().getDocument(this, origrev, context);
        XWikiDocument newdoc = context.getWiki().getDocument(this, newrev, context);
        return getRenderedContentDiff(origdoc, newdoc, context);
    }

    public List getRenderedContentDiff(String rev, XWikiContext context) throws XWikiException, DifferentiationFailedException {
        XWikiDocument revdoc = context.getWiki().getDocument(this, rev, context);
        return getRenderedContentDiff(this, revdoc, context);
    }

    protected List getDeltas(Revision rev) {
        ArrayList list = new ArrayList();
        for (int i = 0; i < rev.size(); i++) {
            list.add(rev.getDelta(i));
        }
        return list;
    }

    public List getMetaDataDiff(String origrev, String newrev, XWikiContext context) throws XWikiException {
        XWikiDocument origdoc = context.getWiki().getDocument(this, origrev, context);
        XWikiDocument newdoc = context.getWiki().getDocument(this, newrev, context);
        return getMetaDataDiff(origdoc, newdoc, context);
    }

    public List getMetaDataDiff(String rev, XWikiContext context) throws XWikiException {
        XWikiDocument revdoc = context.getWiki().getDocument(this, rev, context);
        return getMetaDataDiff(this, revdoc, context);
    }

    public List getMetaDataDiff(XWikiDocument origdoc, XWikiDocument newdoc, XWikiContext context) throws XWikiException {
        List list = new ArrayList();

        if ((origdoc == null) || (newdoc == null))
            return list;

        if (!origdoc.getParent().equals(newdoc.getParent()))
            list.add(new MetaDataDiff("parent", origdoc.getParent(), newdoc.getParent()));
        if (!origdoc.getAuthor().equals(newdoc.getAuthor()))
            list.add(new MetaDataDiff("author", origdoc.getAuthor(), newdoc.getAuthor()));
        if (!origdoc.getWeb().equals(newdoc.getWeb()))
            list.add(new MetaDataDiff("web", origdoc.getWeb(), newdoc.getWeb()));
        if (!origdoc.getName().equals(newdoc.getName()))
            list.add(new MetaDataDiff("name", origdoc.getName(), newdoc.getName()));
        if (!origdoc.getLanguage().equals(newdoc.getLanguage()))
            list.add(new MetaDataDiff("language", origdoc.getLanguage(), newdoc.getLanguage()));
        if (!origdoc.getDefaultLanguage().equals(newdoc.getDefaultLanguage()))
            list.add(new MetaDataDiff("defaultLanguage", origdoc.getDefaultLanguage(), newdoc.getDefaultLanguage()));
        return list;
    }

    public List getObjectDiff(String origrev, String newrev, XWikiContext context) throws XWikiException {
        XWikiDocument origdoc = context.getWiki().getDocument(this, origrev, context);
        XWikiDocument newdoc = context.getWiki().getDocument(this, newrev, context);
        return getObjectDiff(origdoc, newdoc, context);
    }

    public List getObjectDiff(String rev, XWikiContext context) throws XWikiException {
        XWikiDocument revdoc = context.getWiki().getDocument(this, rev, context);
        return getObjectDiff(this, revdoc, context);
    }

    public List getObjectDiff(XWikiDocument origdoc, XWikiDocument newdoc, XWikiContext context) throws XWikiException {
        ArrayList difflist = new ArrayList();
        for (Iterator itobjs = origdoc.getxWikiObjects().values().iterator(); itobjs.hasNext();) {
            Vector objects = (Vector) itobjs.next();
            for (Iterator itobjs2 = objects.iterator(); itobjs2.hasNext();) {
                BaseObject origobj = (BaseObject) itobjs2.next();
                BaseObject newobj = newdoc.getObject(origobj.getClassName(), origobj.getNumber());

                List dlist;
                if (newobj == null)
                    dlist = origobj.getDiff(new BaseObject(), context);
                else
                    dlist = origobj.getDiff(newobj, context);
                if (dlist.size() > 0)
                    difflist.add(dlist);
            }
        }
        for (Iterator itobjs = newdoc.getxWikiObjects().values().iterator(); itobjs.hasNext();) {
            Vector objects = (Vector) itobjs.next();
            for (Iterator itobjs2 = objects.iterator(); itobjs2.hasNext();) {
                BaseObject newobj = (BaseObject) itobjs2.next();
                BaseObject origobj = origdoc.getObject(newobj.getClassName(), newobj.getNumber());

                if (origobj == null) {
                    origobj = new BaseObject();
                    origobj.setClassName(newobj.getClassName());
                    origobj.setNumber(newobj.getNumber());
                    List dlist = origobj.getDiff(newobj, context);
                    if (dlist.size() > 0)
                        difflist.add(dlist);
                }
            }
        }
        return difflist;
    }

    public List getClassDiff(XWikiDocument origdoc, XWikiDocument newdoc, XWikiContext context) throws XWikiException {
        ArrayList difflist = new ArrayList();
        BaseClass origclass = origdoc.getxWikiClass();
        BaseClass newclass = newdoc.getxWikiClass();

        if ((newclass == null) && (origclass == null))
            return difflist;

        List dlist = origclass.getDiff(newclass, context);
        if (dlist.size() > 0)
            difflist.add(dlist);
        return difflist;
    }

    public XWikiDocument renameDocument(String docname, XWikiContext context) throws XWikiException {
        String oldname = getFullName();
        if (oldname.equals(docname))
            return this;

        XWikiDocument newdoc = (XWikiDocument) clone();
        newdoc.setFullName(docname, context);
        newdoc.getxWikiClass().setName(docname);
        Vector objects = newdoc.getObjects(oldname);
        if (objects != null) {
            Iterator it = objects.iterator();
            while (it.hasNext()) {
                BaseObject object = (BaseObject) it.next();
                object.setName(docname);
            }
        }
        return newdoc;
    }

    public XWikiLock getLock(XWikiContext context) throws XWikiException {
        XWikiLock theLock = getStore().loadLock(getId(), context, true);
        if (theLock != null) {
            int timeout = context.getWiki().getXWikiPreferenceAsInt("lock_Timeout", 30 * 60, context);
            if (theLock.getDate().getTime() + timeout * 1000 < new Date().getTime()) {
                getStore().deleteLock(theLock, context, true);
                theLock = null;
            }
        }
        return theLock;
    }

    public void setLock(String userName, XWikiContext context) throws XWikiException {
        XWikiLock lock = new XWikiLock(getId(), userName);
        getStore().saveLock(lock, context, true);
    }

    public void removeLock(XWikiContext context) throws XWikiException {
        XWikiLock lock = getStore().loadLock(getId(), context, true);
        if (lock != null) {
            getStore().deleteLock(lock, context, true);
        }
    }

    // This functions adds an object from an new object creation form
    public BaseObject addObjectFromRequest(XWikiContext context) throws XWikiException {
        // Read info in object
        ObjectAddForm form = new ObjectAddForm();
        form.setRequest((HttpServletRequest) context.getRequest());
        form.readRequest();

        XWikiDocument newdoc = (XWikiDocument) clone();
        String className = form.getClassName();
        int nb = newdoc.createNewObject(className, context);
        BaseObject oldobject = newdoc.getObject(className, nb);
        BaseClass baseclass = oldobject.getxWikiClass(context);
        BaseObject newobject = (BaseObject) baseclass.fromMap(form.getObject(className), oldobject);
        newobject.setNumber(oldobject.getNumber());
        newobject.setName(newdoc.getFullName());
        newdoc.setObject(className, nb, newobject);
        context.getWiki().saveDocument(newdoc, context);
        return newobject;
    }

    public void insertText(String text, String marker, XWikiContext context) throws XWikiException {
        setContent(StringUtils.replaceOnce(getContent(), marker, text + marker));
        context.getWiki().saveDocument(this, context);
    }

    public Object getWikiNode() {
        return wikiNode;
    }

    public void setWikiNode(Object wikiNode) {
        this.wikiNode = wikiNode;
    }

    public String getxWikiClassXML() {
        return xWikiClassXML;
    }

    public void setxWikiClassXML(String xWikiClassXML) {
        this.xWikiClassXML = xWikiClassXML;
    }

    public int getElements() {
        return elements;
    }

    public void setElements(int elements) {
        this.elements = elements;
    }

    public void setElement(int element, boolean toggle) {
        if (toggle)
            elements = elements | element;
        else
            elements = elements & (~element);
    }

    public boolean hasElement(int element) {
        return ((elements & element) == element);
    }

    public String getDefaultEditURL(XWikiContext context) throws XWikiException {
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        if (getContent().indexOf("includeForm(") != -1) {
            return getEditURL("inline", "", context);
        } else {
            String editor = xwiki.getEditorPreference(context);
            return getEditURL("edit", editor, context);
        }
    }

    public String getEditURL(String action, String mode, XWikiContext context) throws XWikiException {
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        String language = "";
        XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
        String realLang = tdoc.getRealLanguage(context);
        if ((xwiki.isMultiLingual(context) == true) && (!realLang.equals(""))) {
            language = realLang;
        }
        return getEditURL(action, mode, language, context);
    }

    public String getEditURL(String action, String mode, String language, XWikiContext context) {
        StringBuffer editparams = new StringBuffer();
        if (!mode.equals("")) {
            editparams.append("xpage=");
            editparams.append(mode);
        }

        if (!language.equals("")) {
            if (!mode.equals(""))
                editparams.append("&");
            editparams.append("language=");
            editparams.append(language);
        }
        return getURL(action, editparams.toString(), context);
    }

    public String getDefaultTemplate() {
        return defaultTemplate;
    }

    public void setDefaultTemplate(String defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public Vector getComments() {
        return getComments(true);
    }

    public Vector getComments(boolean asc) {
        if (asc)
            return getObjects("XWiki.XWikiComments");
        else {
            Vector list = getObjects("XWiki.XWikiComments");
            if (list == null)
                return list;
            Vector newlist = new Vector();
            for (int i = list.size() - 1; i >= 0; i--) {
                newlist.add(list.get(i));
            }
            return newlist;
        }
    }

}
