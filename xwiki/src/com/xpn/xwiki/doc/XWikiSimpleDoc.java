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
 * Date: 24 nov. 2003
 * Time: 00:00:31
 */
package com.xpn.xwiki.doc;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.EditForm;
import com.xpn.xwiki.web.PrepareEditForm;
import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.Lines;
import org.apache.commons.jrcs.rcs.Version;
import org.apache.tools.ant.filters.StringInputStream;
import org.apache.velocity.VelocityContext;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;


public class XWikiSimpleDoc extends XWikiDefaultDoc {
    private String parent;
    private String web;
    private String name;
    private String content;
    private String meta;
    private String format;
    private String author;
    private Archive archive;
    private Date date;
    private Version version;
    private long id = 0;
    private boolean mostRecent = false;
    private boolean isNew = true;
    private String template;

    // Used to make sure the MetaData String is regenerated
    private boolean isContentDirty = false;
    // Used to make sure the MetaData String is regenerated
    private boolean isMetaDataDirty = false;

    // Meta Data
    private BaseClass xWikiClass;
    private Map xWikiObjects = new HashMap();

    private List attachmentList;

    // Caching
    private boolean fromCache = false;

    public long getId() {
        if (id==0) {
            id = getFullName().hashCode();
        }
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

    public XWikiSimpleDoc() {
        this("Main", "WebHome");
    }

    public XWikiSimpleDoc(String web, String name) {
        this.web = web;
        this.name = name;
        this.date = new Date();
        this.parent = "";
        this.content = "\n";
        this.format = "";
        this.author = "";
        this.archive = null;
        this.attachmentList = new ArrayList();
    }

    public XWikiDocInterface getParentDoc() {
        return new XWikiSimpleDoc(web, parent);
    }

    public String getParent() {
        return parent;
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
        }
        this.content = content;
    }

    public String getRenderedContent(XWikiContext context) {
        return context.getWiki().getRenderingEngine().renderDocument(this, context);
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
        // It is not allowed to use this..
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
        if (!format.equals(this.format)) {
            setMetaDataDirty(true);
        }
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        if (!author.equals(this.author)) {
            setMetaDataDirty(true);
        }
        this.author = author;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        if (!date.equals(this.date)) {
            setMetaDataDirty(true);
        }
        this.date = date;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        if (meta==null) {
            if (this.meta!=null)
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
        if (version==null)
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

    public String getActionUrl(String action, XWikiContext context) {
        StringBuffer url = new StringBuffer();
        url.append(context.getWiki().getBase());
        url.append(action);
        url.append("/");
        url.append(getWeb());
        url.append("/");
        url.append(getName());
        return url.toString();
    }

    public String getViewUrl(XWikiContext context) {
        return getActionUrl("view", context);
    }

    public String getEditUrl(XWikiContext context) {
        return getActionUrl("edit", context);
    }

    public String getPreviewUrl(XWikiContext context) {
        return getActionUrl("preview", context);
    }

    public String getSaveUrl(XWikiContext context) {
        return getActionUrl("save", context);
    }

    public String getAttachUrl(XWikiContext context) {
        return getActionUrl("attach", context);
    }

    public String getParentUrl(XWikiContext context) {
        StringBuffer url = new StringBuffer();
        url.append(context.getWiki().getBase());
        url.append("view");
        url.append("/");
        String parent = getParent();
        parent = parent.replace('.', '/');
        url.append(parent);
        return url.toString();
    }


    public Version[] getRevisions() throws XWikiException {
        return getStore().getXWikiDocVersions(this);
    }

    public String[] getRecentRevisions() throws XWikiException {
        Version[] revisions = getStore().getXWikiDocVersions(this);
        int length = 5;
        if (revisions.length<5)
            length = revisions.length;

        String[] recentrevs = new String[length];
        for (int i = 1; i <= length; i++)
            recentrevs[i-1
                    ] = revisions[revisions.length-i].toString();
        return recentrevs;
    }

    public Archive getRCSArchive() {
        return archive;
    }

    public void setRCSArchive(Archive archive) {
        this.archive = archive;
    }

    public String getArchive() throws XWikiException {
        if ((content==null)||(content.equals("")))
            setContent("\n");
        if (archive==null)
            updateArchive(toXML());
        if (archive==null)
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
            Object[] args = { getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ARCHIVEFORMAT,
                    "Exception while manipulating the archive for doc {0}", e, args);
        }
    }

    public void updateArchive(String text) throws XWikiException {
        try {
            Lines lines = new Lines(text);
            if (archive!=null)
                archive.addRevision(lines.toArray(),"");
            else
                archive = new Archive(lines.toArray(),getFullName(),getVersion());
        }
        catch (Exception e) {
            Object[] args = { getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ARCHIVEFORMAT,
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
        if (xWikiClass==null) {
            xWikiClass = new BaseClass();
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
        return getObject(getFullName(),0);
    }

    public void createNewObject(String classname, XWikiContext context) throws XWikiException {
        XWiki xwiki = (XWiki) context.getWiki();
        BaseClass objclass = new BaseClass();
        objclass.setName(classname);
        ((XWikiStoreInterface)xwiki.getStore()).loadXWikiClass(objclass, true);
        BaseObject object = new BaseObject();
        object.setName(getFullName());
        object.setxWikiClass(objclass);
        Vector objects = getObjects(objclass.getName());
        if (objects==null) {
           objects = new Vector();
           setObjects(objclass.getName(), objects);
        }
        objects.add(object);
        object.setNumber(objects.size()-1);
    }

    public int getObjectNumbers(String classname) {
        try {
            return ((Vector)getxWikiObjects().get(classname)).size();
        } catch (Exception e) {
            return 0;
        }
    }

    public Vector getObjects(String classname) {
        try {
            return (Vector)getxWikiObjects().get(classname);
        } catch (Exception e) {
            return null;
        }
    }

    public void setObjects(String classname, Vector objects) {
        getxWikiObjects().put(classname, objects);
    }

    public BaseObject getObject(String classname, int nb) {
        try {
            return (BaseObject) ((Vector)getxWikiObjects().get(classname)).get(nb);
        } catch (Exception e) {
            return null;
        }
    }

    public void addObject(String classname, BaseObject object) {
        Vector vobj = getObjects(classname);
        if (vobj==null)
         setObject(classname, 0, object);
        else
         setObject(classname, vobj.size(), object);
    }

    public void setObject(String classname, int nb, BaseObject object) {
        Vector objects = null;
        objects = getObjects(classname);
        if (objects==null) {
            objects = new Vector();
            setObjects(classname, objects);
        }
        if (nb >= objects.size()) {
         objects.add(object);
         object.setNumber(objects.size()-1);
        }
        else {
         objects.set(nb, object);
         object.setNumber(nb);
        }
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public void mergexWikiClass(XWikiDocInterface templatedoc) {
        BaseClass bclass = getxWikiClass();
        BaseClass tbclass = templatedoc.getxWikiClass();
        if (tbclass!=null) {
            if (bclass==null) {
                setxWikiClass((BaseClass)tbclass.clone());
            } else {
                getxWikiClass().merge((BaseClass)tbclass.clone());
            }
        }
    }

    public void mergexWikiObjects(XWikiDocInterface templatedoc) {
        // TODO: look for each object if it already exist and add it if it doesn't
        Iterator itobjects = templatedoc.getxWikiObjects().keySet().iterator();
        while (itobjects.hasNext()) {
            String name = (String) itobjects.next();
            Vector objects = (Vector) getxWikiObjects().get(name);

            if (objects!=null) {
                Vector tobjects = (Vector) templatedoc.getxWikiObjects().get(name);
                for (int i=0;i<tobjects.size();i++) {
                    {
                     BaseObject bobj = (BaseObject) ((BaseObject) tobjects.get(i)).clone();
                     objects.add(bobj);
                     bobj.setNumber(objects.size()-1);
                    }
                }
            } else {
                Vector tobjects = (Vector) templatedoc.getxWikiObjects().get(name);
                objects = new Vector();
                for (int i=0;i<tobjects.size();i++)
                {
                    BaseObject bobj = (BaseObject) ((BaseObject) tobjects.get(i)).clone();
                    objects.add(bobj);
                    bobj.setNumber(objects.size()-1);
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
            PropertyClass pclass = (PropertyClass) getxWikiClass().get(fieldname);
            String prefix = obj.getxWikiClass().getName() + "_" + obj.getNumber() + "_";

            if (type.equals("view")) {
                pclass.displayView(result, fieldname, prefix, obj, context);
            }
            else if (type.equals("edit")) {
                pclass.displayEdit(result, fieldname, prefix, obj, context);
            }
            else if (type.equals("hidden")) {
                pclass.displayHidden(result, fieldname, prefix, obj, context);
            }
            else if (type.equals("search")) {
                pclass.displaySearch(result, fieldname, prefix, obj, context);
            }
            else {
                pclass.displayView(result, fieldname, prefix, obj, context);
            }
            return result.toString();
        }
        catch (Exception e) {
            return "||Exception showing field " + fieldname + ": " + e.getMessage() + "||";
        }
    }

    public String display(String fieldname, BaseObject obj, XWikiContext context) {
        String type = null;
        try { type = (String) context.get("display"); }
        catch (Exception e) {
        };
        if (type==null)
            type = "view";
        return display(fieldname, type, obj, context);
    }

    public String display(String fieldname, XWikiContext context) {
        return display(fieldname, getxWikiObject(), context);
    }

    public String displayForm(String className,String header, String format, XWikiContext context) {
        return displayForm(className, header, format, true, context);
    }

    public String displayForm(String className,String header, String format, boolean linebreak, XWikiContext context) {
      Vector objects = getObjects(className);
      if (format.endsWith("\\n"))
       linebreak = true;

      if (objects.size()==0)
       return "";
        BaseClass bclass = ((BaseObject)objects.get(0)).getxWikiClass();
        Map fields = bclass.getFields();
        if (fields.size()==0)
         return "";

      StringBuffer result = new StringBuffer();
      XWikiVelocityRenderer renderer = new XWikiVelocityRenderer();
      VelocityContext vcontext = new VelocityContext();
      for (Iterator it = fields.values().iterator();it.hasNext();) {
          PropertyClass pclass = (PropertyClass) it.next();
          vcontext.put(pclass.getName(), pclass.getPrettyName());
      }
      result.append(renderer.evaluate(header, "Form displayer for class " + className, vcontext));
      if (linebreak)
         result.append("\n");

      // display each line
      for (int i=0;i<objects.size();i++) {
          vcontext.put("id", new Integer(i+1));
          BaseObject object = (BaseObject) objects.get(i);
          for (Iterator it = fields.keySet().iterator();it.hasNext();) {
              String name = (String) it.next();
              vcontext.put(name, display(name, object, context));
          }
          result.append(renderer.evaluate(format, "Form displayer for class " + className, vcontext));
          if (linebreak)
             result.append("\n");
      }
      return result.toString();
    }

    public String displayForm(String className, XWikiContext context) {
      Vector objects = getObjects(className);
      if (objects.size()==0)
       return "";
      BaseClass bclass = ((BaseObject)objects.get(0)).getxWikiClass();
      Map fields = bclass.getFields();
      if (fields.size()==0)
       return "";

      StringBuffer result = new StringBuffer();
      result.append("|");
      for (Iterator it = fields.values().iterator();it.hasNext();) {
          PropertyClass pclass = (PropertyClass) it.next();
          result.append(" *");
          result.append(pclass.getPrettyName());
          result.append("* |");
      }
      result.append("\n");
      for (int i=0;i<objects.size();i++) {
          BaseObject object = (BaseObject) objects.get(i);
          result.append("|");
          for (Iterator it = fields.keySet().iterator();it.hasNext();) {
              result.append("<nop>");
              result.append(display((String)it.next(), object, context));
              result.append("|");
          }
          result.append("\n");
      }
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
        if ((content!=null)&&(!content.equals("")))
            setContent(content);
        String parent = eform.getParent();
        if (parent!=null)
            setParent(parent);
        readFromTemplate(eform, context);

        Iterator itobj = getxWikiObjects().keySet().iterator();
        while (itobj.hasNext()) {
            String name = (String) itobj.next();
            BaseObject baseobject = (BaseObject)getObject(name, 0);
            BaseClass baseclass = baseobject.getxWikiClass();
            int nb = eform.getObjectNumbers(baseclass.getName());
            if (nb>0) {
                Vector newobjects = new Vector();
                for (int i=0;i<nb;i++) {
                    BaseObject oldobject = (BaseObject) getObject(name, i);
                    BaseObject newobject = (BaseObject) baseclass.fromMap(eform.getObject(baseclass.getName() + "_" + i), oldobject);
                    newobject.setNumber(i);
                    newobject.setName(getFullName());
                    newobjects.add(newobject);
                }
                getxWikiObjects().put(name, newobjects);
            }
        }

    }

    public void readFromTemplate(EditForm eform, XWikiContext context) throws XWikiException {
        // Get the class from the template
        String template = eform.getTemplate();
        if ((template!=null)&&(!template.equals(""))) {
            if (template.indexOf('.')==-1) {
                template = getWeb() + "." + template;
            }
            XWiki xwiki = context.getWiki();
            XWikiDocInterface templatedoc = xwiki.getDocument(template);
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

    public void readFromTemplateForEdit(PrepareEditForm eform, XWikiContext context) throws XWikiException {

        String template = eform.getTemplate();
        if ((template!=null)&&(!template.equals(""))) {
            String content = getContent();
            if ((content==null)||(!content.equals(""))) {
                Object[] args = { getFullName() };
                throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY,
                        "Cannot add a template to document {0} because it already has content", null, args);
            } else {

                if (template.indexOf('.')==-1) {
                    template = getWeb() + "." + template;
                }
                XWiki xwiki = context.getWiki();
                XWikiDocInterface templatedoc = xwiki.getDocument(template);
                if (templatedoc.isNew()) {
                    Object[] args = { template, getFullName() };
                    throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_APP_TEMPLATE_DOES_NOT_EXIST,
                            "Template document {0} does not exist when adding to document {1}", null, args);
                } else {
                    setTemplate(template);
                    setContent(templatedoc.getContent());
                    if ((getParent()==null)||(getParent().equals(""))) {
                        String tparent = templatedoc.getParent();
                        if (tparent!=null)
                            setParent(tparent);
                    }

                    // Merge the external objects
                    // Currently the choice is not to merge the base class and object because it is not
                    // the prefered way of using external classes and objects.
                    mergexWikiObjects(templatedoc);
                }
            }
        }
    }

    public void notify(XWikiNotificationRule rule, XWikiDocInterface newdoc, XWikiDocInterface olddoc, int event, XWikiContext context) {
        // Do nothing for the moment..
        // A usefull thing here would be to look at any instances of a Notification Object
        // with email addresses and send an email to warn that the document has been modified..

    }

    public Object clone() {
        XWikiSimpleDoc doc = (XWikiSimpleDoc) super.clone();
        doc.setRCSArchive(getRCSArchive());
        doc.setRCSVersion(getRCSVersion());
        doc.setAuthor(getAuthor());
        doc.setContent(getContent());
        doc.setContentDirty(isContentDirty());
        doc.setDate(getDate());
        doc.setFormat(getFormat());
        doc.setFromCache(isFromCache());
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
        doc.setxWikiClass((BaseClass)getxWikiClass().clone());
        doc.mergexWikiObjects(this);
        return doc;
    }

    public boolean equals(Object object) {
        XWikiSimpleDoc doc = (XWikiSimpleDoc) object;
        if (!getName().equals(doc.getName()))
         return false;

        if (!getWeb().equals(doc.getWeb()))
                 return false;

        if (!getAuthor().equals(doc.getAuthor()))
                 return false;

        if (getDate().getTime() != doc.getDate().getTime())
                 return false;

        if (!getFormat().equals(doc.getFormat()))
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

        for (Iterator it = list1.iterator();it.hasNext();) {
            String name = (String) it.next();
            Vector v1 = getObjects(name);
            Vector v2 = doc.getObjects(name);
            if (v1.size()!=v2.size())
                return false;
            for (int i=0;i<v1.size();i++) {
                if (!v1.get(i).equals(v2.get(i)))
                    return false;
            }
        }


        return true;
    }

    public String toXML() {
        Document doc = toXMLDocument();
        OutputFormat outputFormat = new OutputFormat("", true);
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter( out, outputFormat );
        try {
            writer.write(doc);
            return out.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public Document toXMLDocument() {
        Document doc = new DOMDocument();
        Element docel = new DOMElement("xwikidoc");
        doc.setRootElement(docel);

        Element el = new DOMElement("web");
        el.addText(getWeb());
        docel.add(el);

        el = new DOMElement("name");
        el.addText(getName());
        docel.add(el);

        el = new DOMElement("parent");
        el.addText(getParent());
        docel.add(el);

        el = new DOMElement("author");
        el.addText(getAuthor());
        docel.add(el);


        long d = getDate().getTime();
        el = new DOMElement("date");
        el.addText("" + d);
        docel.add(el);

        el = new DOMElement("version");
        el.addText(getVersion());
        docel.add(el);

        List alist = getAttachmentList();
        for (int ai=0;ai<alist.size();ai++) {
            XWikiAttachment attach = (XWikiAttachment) alist.get(ai);
            docel.add(attach.toXML());

        }

        // Add Class
        BaseClass bclass = getxWikiClass();
        if (bclass.getFields().size()>0) {
          docel.add(bclass.toXML());
        }

        // Add Objects
        Iterator it = getxWikiObjects().values().iterator();
        while (it.hasNext()) {
            Vector objects = (Vector) it.next();
            for (int i=0;i<objects.size();i++) {
                BaseObject obj = (BaseObject)objects.get(i);
                docel.add(obj.toXML());
            }
        }

        // Add Content
        el = new DOMElement("content");
        el.addText(getContent());
        docel.add(el);
        return doc;
    }

     public void fromXML(String xml) throws DocumentException, java.text.ParseException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        StringInputStream in = new StringInputStream(xml);
        SAXReader reader = new SAXReader();
        Document domdoc = reader.read(in);
        Element docel = domdoc.getRootElement();

        setName(docel.element("name").getText());
        setWeb(docel.element("web").getText());
        setParent(docel.element("parent").getText());
        setAuthor(docel.element("author").getText());
        setVersion(docel.element("version").getText());
        setContent(docel.element("content").getText());

        String sdate = docel.element("date").getText();
        Date date = new Date(Long.parseLong(sdate));
        setDate(date);

        List atels = docel.elements("attachment");
        for (int i=0;i<atels.size();i++) {
            Element atel = (Element) atels.get(i);
            XWikiAttachment attach = new XWikiAttachment();
            attach.setDoc(this);
            attach.fromXML(atel);
        }

        Element cel = docel.element("class");
        BaseClass bclass = new BaseClass();
        if (cel!=null) {
            bclass.fromXML(cel);
            setxWikiClass(bclass);
        }

        //
        List objels = docel.elements("object");
        for (int i=0;i<objels.size();i++) {
            Element objel = (Element) objels.get(i);
            BaseObject bobject = new BaseObject();
            bobject.fromXML(objel);
            addObject(bobject.getClassName(), bobject);
        }
    }

   public String toString() {
        return toXML();
    }

    public void setAttachmentList(List list) {
       attachmentList = list;
    }

    public List getAttachmentList() {
        return attachmentList;
    }

    public void saveAttachmentContent(XWikiAttachment attachment) throws XWikiException {
        getStore().saveAttachmentContent(attachment, true);
    }

    public void loadAttachmentContent(XWikiAttachment attachment) throws XWikiException {
        getStore().loadAttachmentContent(attachment,  true);
    }

}
