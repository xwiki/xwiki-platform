package com.xpn.xwiki.gwt.api.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.*;
import java.util.Collection;
import java.lang.Object;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 19 nov. 2006
 * Time: 19:42:43
 * To change this template use File | Settings | File Templates.
 */
public class Document implements IsSerializable {
    private long id;
    private String title;
    private String parent;
    private String space;
    private String name;
    private String content;
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
        obj.setNumber(list.size()+1);
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


}
