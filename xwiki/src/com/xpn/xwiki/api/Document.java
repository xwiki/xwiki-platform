package com.xpn.xwiki.api;

import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.BaseObject;
import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.Lines;
import org.apache.commons.jrcs.rcs.Version;


import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 26 févr. 2004
 * Time: 16:59:02
 * To change this template use File | Settings | File Templates.
 */
public class Document {
    private XWikiDocInterface doc;
    private XWikiContext context;

    public Document(XWikiDocInterface doc, XWikiContext context) {
       this.doc = doc;
       this.context = context;
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
}
