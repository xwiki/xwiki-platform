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

    public BaseClass getxWikiClass() {
        return doc.getxWikiClass();
    }

    public Map getxWikiObjects() {
        return doc.getxWikiObjects();
    }

    public BaseObject getxWikiObject() {
        return doc.getxWikiObject();
    }

    public List getxWikiClasses() {
        return doc.getxWikiClasses();
    }

    public void createNewObject(String classname) throws XWikiException {
        doc.createNewObject(classname, context);
    }

    public boolean isFromCache() {
        return doc.isFromCache();
    }

    public int getObjectNumbers(String classname) {
        return doc.getObjectNumbers(classname);
    }

    public Vector getObjects(String classname) {
        return doc.getObjects(classname);
    }

    public BaseObject getObject(String classname, int nb) {
         return doc.getObject(classname, nb);
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

    public String display(String fieldname, BaseObject obj) {
        return doc.display(fieldname, obj, context);
    }

    public String display(String fieldname, String mode, BaseObject obj) {
        return doc.display(fieldname, mode, obj, context);
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

    public String displayRendered(PropertyClass pclass, String prefix, BaseCollection object) {
         return doc.displayRendered(pclass, prefix, object, context);
    }

    public String displayView(PropertyClass pclass, String prefix, BaseCollection object) {
         return doc.displayView(pclass, prefix, object, context);
    }

    public String displayEdit(PropertyClass pclass, String prefix, BaseCollection object) {
         return doc.displayEdit(pclass, prefix, object, context);
    }

    public String displayHidden(PropertyClass pclass, String prefix, BaseCollection object) {
         return doc.displayHidden(pclass, prefix, object, context);
    }

    public String displaySearch(PropertyClass pclass, String prefix, BaseCollection object) {
         return doc.displaySearch(pclass, prefix, object, context);
    }

    public List getIncludedPages() {
        return doc.getIncludedPages(context);
    }
}
