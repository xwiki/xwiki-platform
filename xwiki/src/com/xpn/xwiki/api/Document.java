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
 * User: ludovic
 * Date: 26 févr. 2004
 * Time: 16:59:02
 */

package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.Lines;
import org.apache.commons.jrcs.rcs.Version;

import java.util.*;


public class Document extends Api {
    private XWikiDocInterface doc;

    public Document(XWikiDocInterface doc, XWikiContext context) {
       super(context);
       this.doc = doc;
    }

    public XWikiDocInterface getDocument() {
        if (checkProgrammingRights())
            return doc;
        else
            return null;
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

    public String getFormat() {
        return doc.getFormat();
    }

    public String getAuthor() {
        return doc.getAuthor();
    }

    public Date getDate() {
        return doc.getDate();
    }

    public String getParent() {
        return doc.getParent();
    }

    public String getContent() {
        return doc.getContent();
    }

    public String getRenderedContent() {
        return doc.getRenderedContent(context);
    }

    public String getRenderedContent(String text) {
        return doc.getRenderedContent(text, context);
    }

    public String getEscapedContent() {
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

    public String getActionUrl(String action) {
        return doc.getActionUrl(action, context);
    }

    public String getParentUrl() {
        return doc.getParentUrl(context);
    }

    public Class getxWikiClass() {
        BaseClass bclass = doc.getxWikiClass();
        if (bclass==null)
            return null;
        else
            return new Class(bclass, context);
    }


    public Class[] getxWikiClasses() {
        List list = doc.getxWikiClasses();
        if (list==null)
            return null;
        Class[] result = new Class[list.size()];
        for (int i=0;i<list.size();i++)
            result[i] = new Class((BaseClass) list.get(i), context);
        return result;
    }

    public void createNewObject(String classname) throws XWikiException {
        if (checkProgrammingRights())
         doc.createNewObject(classname, context);
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
            resultmap.put(name, getObjects(objects));
        }
        return resultmap;
    }

    protected Vector getObjects(Vector objects) {
        Vector result = new Vector();

        for (int i=0;i<objects.size();i++) {
            BaseObject bobj = (BaseObject) objects.get(i);
            if (bobj!=null) {
              result.add(new Object(bobj, context));
            }
        }
        return result;
    }

    public Vector getObjects(String classname) {
        Vector objects = doc.getObjects(classname);
        return getObjects(objects);
    }

    public Object getObject(String classname, int nb) {
         return new Object(doc.getObject(classname, nb), context);
    }

    public String toXML() {
         return doc.toXML();
    }

    public org.dom4j.Document toXMLDocument() {
        return doc.toXMLDocument();
    }

    public Version[] getRevisions() throws XWikiException {
        return doc.getRevisions(context);
    }

    public String[] getRecentRevisions() throws XWikiException {
        return doc.getRecentRevisions(context);
    }

    public List getAttachmentList() {
        List list = doc.getAttachmentList();
        List list2 = new ArrayList();
        for (int i=0;i<list.size();i++) {
            list2.add(new Attachment(this, (XWikiAttachment)list.get(i), context));
        }
        return list2;
    }

    public String display(String fieldname, Object obj) {
        return doc.display(fieldname, obj.getBaseObject(), context);
    }

    public String display(String fieldname, String mode, Object obj) {
        return doc.display(fieldname, mode, obj.getBaseObject(), context);
    }

    public String display(String fieldname) {
        return doc.display(fieldname, context);
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
         return doc.displayRendered(pclass.getBasePropertyClass(), prefix, object.getCollection(), context);
    }

    public String displayView(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object) {
         return doc.displayView(pclass.getBasePropertyClass(), prefix, object.getCollection(), context);
    }

    public String displayEdit(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object) {
         return doc.displayEdit(pclass.getBasePropertyClass(), prefix, object.getCollection(), context);
    }

    public String displayHidden(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object) {
         return doc.displayHidden(pclass.getBasePropertyClass(), prefix, object.getCollection(), context);
    }

    public String displaySearch(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object) {
         return doc.displaySearch(pclass.getBasePropertyClass(), prefix, object.getCollection(), context);
    }

    public List getIncludedPages() {
        return doc.getIncludedPages(context);
    }

    public Attachment getAttachment(String filename) {
        return new Attachment(this, doc.getAttachment(filename), context);
    }
}
