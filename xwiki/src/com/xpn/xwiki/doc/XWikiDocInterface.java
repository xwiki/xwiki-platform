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
 * Date: 23 nov. 2003
 * Time: 23:58:27
 */
package com.xpn.xwiki.doc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.notify.XWikiNotificationInterface;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.EditForm;
import com.xpn.xwiki.web.PrepareEditForm;
import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.ParseException;
import org.apache.commons.jrcs.rcs.Version;
import org.dom4j.Document;
import org.dom4j.DocumentException;

import java.io.FileNotFoundException;
import java.util.*;

public interface XWikiDocInterface extends XWikiNotificationInterface {
    public long getId();
    public void setId(long id);
    public String getName();
    public void setName(String name);
    public String getWeb();
    public void setWeb(String web);
    public String getFullName();
    public void setFullName(String name);
    public void setFullName(String name, XWikiContext context);
    public Version getRCSVersion();
    public void setRCSVersion(Version version);
    public String getVersion();
    public void setVersion(String version);
    public String getFormat();
    public void setFormat(String format);
    public String getAuthor();
    public void setAuthor(String author);
    public Date getDate();
    public void setDate(Date date);
    public String getMeta();
    public void setMeta(String meta);
    public void appendMeta(String meta);

    public String getParent();
    public void setParent(String parent);
    public XWikiDocInterface getParentDoc();

    public String getContent();
    public void setContent(String content);
    public String getRenderedContent(XWikiContext context) throws XWikiException;
    public String getEscapedContent(XWikiContext context) throws XWikiException;

    public boolean isMetaDataDirty();
    public boolean isContentDirty();

    public void incrementVersion();
    public Archive getRCSArchive();
    public void setRCSArchive(Archive archive);
    public String getArchive() throws XWikiException;
    public String getArchive(XWikiContext context) throws XWikiException;
    public void setArchive(String text) throws FileNotFoundException, ParseException, XWikiException;
    public void updateArchive(String text) throws XWikiException;
    public boolean isMostRecent();
    public void setMostRecent(boolean mostRecent);

    public boolean isNew();
    public void setNew(boolean aNew);

    public String getTemplate();
    public void setTemplate(String template);

    public XWikiStoreInterface getStore();
    public void setStore(XWikiStoreInterface store);

    public String getActionUrl(String action, XWikiContext context);

    public BaseClass getxWikiClass();
    public void setxWikiClass(BaseClass xWikiClass);
    public Map getxWikiObjects();
    public void setxWikiObjects(Map xWikiObject);
    public BaseObject getxWikiObject();
    public void createNewObject(String classname, XWikiContext context) throws XWikiException;
    public void mergexWikiClass(XWikiDocInterface templatedoc);
    public void mergexWikiObjects(XWikiDocInterface templatedoc);

    public boolean isFromCache() ;
    public void setFromCache(boolean fromCache);

    public int getObjectNumbers(String classname);
    public Vector getObjects(String classname);
    public void setObjects(String classname, Vector objects);
    public BaseObject getObject(String classname, int nb);
    public void setObject(String classname, int nb, BaseObject object);

    public void readFromForm(EditForm eform, XWikiContext context) throws XWikiException;
    public void readFromTemplate(EditForm eform, XWikiContext context) throws XWikiException;
    public void readFromTemplateForEdit(PrepareEditForm eform, XWikiContext context) throws XWikiException;

    public Object clone();
    public void addObject(String classname, BaseObject object);

    public String toXML(XWikiContext context);
    public Document toXMLDocument(XWikiContext context);
    public void fromXML(String xml) throws DocumentException, java.text.ParseException, IllegalAccessException, InstantiationException, ClassNotFoundException;

    public Version[] getRevisions(XWikiContext context) throws XWikiException;
    public String[] getRecentRevisions(XWikiContext context) throws XWikiException;

    public void setAttachmentList(List list);
    public List getAttachmentList();
    public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context) throws XWikiException;
    public void loadAttachmentContent(XWikiAttachment xWikiAttachment, XWikiContext context) throws XWikiException;
    void deleteAttachment(XWikiAttachment attachment, XWikiContext context) throws XWikiException;

    String getParentUrl(XWikiContext context);
    String display(String fieldname, BaseObject obj, XWikiContext context);
    String display(String fieldname, XWikiContext context);
    String displayForm(String className,String header, String format, XWikiContext context);
    String displayForm(String className,String header, String format, boolean linebreak, XWikiContext context);
    String displayForm(String className, XWikiContext context);
    String display(String fieldname, String type, BaseObject obj, XWikiContext context);

    void renameProperties(String className, Map fieldsToRename);
    void addObjectsToRemove(BaseObject object);
    ArrayList getObjectsToRemove();
    void setObjectsToRemove(ArrayList objectsToRemove);
    List getxWikiClasses(XWikiContext context);


    String displayView(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context);
    String displayEdit(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context);
    String displayHidden(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context);
    String displaySearch(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context);
    String displayRendered(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context);

    String getRenderedContent(String text, XWikiContext context);
    List getIncludedPages(XWikiContext context);
    XWikiAttachment getAttachment(String filename);
    int getIntValue(String className, String objName);
    String getStringValue(String className, String objName);
    String getDatabase();
    void setDatabase(String database);
    BaseObject getObject(String classname);
    void setStringValue(String className, String fieldName, String value);
    void setIntValue(String className, String fieldName, int value);

    // Internationalization
    String getLanguage();
    void setLanguage(String language);
    String getDefaultLanguage();
    void setDefaultLanguage(String defaultLanguage);
    int getTranslation();
    void setTranslation(int translation);
    String getTranslatedContent(String language, XWikiContext context) throws XWikiException;
    XWikiDocInterface getTranslatedDocument(String language, XWikiContext context) throws XWikiException;
    List getTranslationList(XWikiContext context) throws XWikiException;
    String getTranslatedContent(XWikiContext context) throws XWikiException;

    XWikiDocInterface getTranslatedDocument(XWikiContext context) throws XWikiException;

    Date getCreationDate();

    void setCreationDate(Date date);

    String getRealLanguage(XWikiContext context) throws XWikiException;

    String getXMLContent(XWikiContext context) throws XWikiException;
    String toXML(Document doc, XWikiContext context);
    Document toXMLDocument(boolean bWithObjects, boolean bWithRendering,
                                  boolean bWithAttachmentContent,
                                  boolean bWithTranslations, boolean bWithVersions,
                                  XWikiContext context);
}
