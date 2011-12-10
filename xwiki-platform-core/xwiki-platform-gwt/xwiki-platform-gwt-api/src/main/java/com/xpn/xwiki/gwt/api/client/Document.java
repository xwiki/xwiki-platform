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
 */
package com.xpn.xwiki.gwt.api.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.*;
import java.util.Collection;
import java.lang.Object;

public class Document implements IsSerializable {

    public static final int HAS_ATTACHMENTS = 1;
    public static final int HAS_OBJECTS = 2;
    public static final int HAS_CLASS = 4;


    private long id;
    private String title;
    private String parent;
    private String space;
    private String name;
    private String content;
    private String renderedContent;
    private String meta;
    private String format;
    private String creator;
    private String author;
    private String contentAuthor;
    private String customClass;
    private String version;
    private long contentUpdateDate;
    private long updateDate;
    private long creationDate;
    private boolean mostRecent = false;
    private boolean isNew = true;
    private String template;
    private String language;
    private String defaultLanguage;
    private int translation;
    private Map objects = new HashMap();
    private XObject currentObj;
    private String fullName;
    private boolean editRight;
    private boolean viewRight = true;
    private boolean commentRight = true;
    private List attachments = new ArrayList();
    private String uploadURL;
    private String saveURL;
    private String viewURL;
    private int hasElement;
    private String comment;
    private int commentsnb;

    public Document() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getWeb() {
        return getSpace();
    }

    public void setWeb(String web) {
        setSpace(web);
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContentAuthor() {
        return contentAuthor;
    }

    public void setContentAuthor(String contentAuthor) {
        this.contentAuthor = contentAuthor;
    }

    public String getCustomClass() {
        return customClass;
    }

    public void setCustomClass(String customClass) {
        this.customClass = customClass;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getContentUpdateDate() {
        return contentUpdateDate;
    }

    public void setContentUpdateDate(long contentUpdateDate) {
        this.contentUpdateDate = contentUpdateDate;
    }

    public long getDate() {
        return updateDate;
    }

    public void setDate(long updateDate) {
        this.updateDate = updateDate;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isMostRecent() {
        return mostRecent;
    }

    public void setMostRecent(boolean mostRecent) {
        this.mostRecent = mostRecent;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
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

    public Map getObjects() {
        return objects;
    }

    public List getObjects(String className) {
        return (List) objects.get(className);
    }

    public int addObject(String className, XObject obj) {
        List list = getObjects(className);
        if (list==null) {
            list = new ArrayList();
            objects.put(className, list);
        }
//        obj.setNumber(list.size()+1);
        list.add(obj);
        return obj.getNumber();
    }

    public XObject getObject(String className, int nb) {
        List list = getObjects(className);
        return (XObject) list.get(nb);
    }

    public XObject getObject(String className) {
        List list = getObjects(className);
        if ((list==null)||(list.size()==0))
         return null;
        for (int i=0;i<list.size();i++) {
            XObject obj = (XObject) list.get(i);
            if (obj!=null)
             return obj;
        }
        return null;
    }

    public XObject getFirstObject(String fieldname) {
        java.util.Collection objectscoll = objects.values();
        if (objectscoll == null)
            return null;

        for (Iterator itobjs = objectscoll.iterator(); itobjs.hasNext();) {
            Vector objects = (Vector) itobjs.next();
            for (Iterator itobjs2 = objects.iterator(); itobjs2.hasNext();) {
                XObject obj = (XObject) itobjs2.next();
                if (obj != null) {
                        Collection set = obj.getPropertyNames();
                        if ((set != null) && set.contains(fieldname))
                            return obj;
                    }
                    Collection set = obj.getPropertyNames();
                    if ((set != null) && set.contains(fieldname))
                        return obj;
                }
            }
        return null;
    }

    public void use(String className) {
        currentObj = getObject(className);
    }

    public void use(String className, int nb) {
        currentObj = getObject(className, nb);
    }

    public void use(XObject xobject) {
        currentObj =  xobject;
    }

    public String get(String name) {
        XObject obj = currentObj;
        if (obj==null)
         obj = getFirstObject(name);
        return (String) obj.getViewProperty(name);
    }

    public Object getValue(String name) {
        XObject obj = currentObj;
        if (obj==null)
            obj = getFirstObject(name);
        return obj.get(name);

    }

    public Object display(String name) {
        return display(name, "view");
    }

    public String display(String name, String type) {
        XObject obj = currentObj;
        if (obj==null)
         obj = getFirstObject(name);
        if (type.equals("edit"))
            return (String) obj.getEditProperty(name);
        else
         return (String) obj.getViewProperty(name);
    }


    public String getRenderedContent() {
        return renderedContent;
    }

    public void setRenderedContent(String renderedContent) {
        this.renderedContent = renderedContent;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean hasEditRight() {
        return editRight;
    }

    public void setEditRight(boolean editRight) {
        this.editRight = editRight;
    }

    public boolean hasViewRight() {
        return viewRight;
    }

    public void setViewRight(boolean viewRight) {
        this.viewRight = viewRight;
    }

    public boolean hasCommentRight() {
        return commentRight;
    }

    public void setCommentRight(boolean commentRight) {
        this.commentRight = commentRight;
    }

    public List getAttachments() {
        return attachments;
    }

    public void setAttachments(List attachments) {
        this.attachments = attachments;
    }

    public void addAttachments(Attachment att) {
        if (attachments==null) {
            attachments = new ArrayList();
        }
        attachments.add(att);
    }

    public String getUploadURL() {
        return uploadURL;
    }

    public void setUploadURL(String uploadURL) {
        this.uploadURL = uploadURL;
    }

    public boolean hasElement(int element) {
        return ((hasElement & element) == element);
    }

    public void setHasElement(int hasElement) {
        this.hasElement = hasElement;
    }

    public String getSaveURL() {
        return saveURL;
    }

    public void setSaveURL(String saveURL) {
        this.saveURL = saveURL;
    }

    public String getViewURL() {
        return viewURL;
    }

    public void setViewURL(String viewURL) {
        this.viewURL = viewURL;
    }

    public String getComment() {
       return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getCommentsNumber() {
        return this.commentsnb;
    }

    public void setCommentsNumber(int commentsnb) {
        this.commentsnb = commentsnb;
    }
}
