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
 * Time: 01:00:44
 */

package com.xpn.xwiki.store;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.*;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import net.sf.hibernate.*;
import net.sf.hibernate.cfg.Configuration;
import net.sf.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.Node;
import org.apache.commons.jrcs.rcs.Version;

import java.io.BufferedReader;
import java.io.File;
import java.util.*;


public class XWikiHibernateStore extends XWikiRCSFileStore {
    protected SessionFactory sessionFactory;
    private Session session;
    protected Transaction transaction;
    protected Configuration configuration;

    private String hibpath;

    public XWikiHibernateStore(XWiki xwiki, XWikiContext context) {
        String path = xwiki.ParamAsRealPath("xwiki.store.hibernate.path", context);
        setPath(path);
    }

    public XWikiHibernateStore(String hibpath) {
        setPath(hibpath);
    }


    public String getPath() {
        return hibpath;
    }

    public void setPath(String hibpath) {
        this.hibpath = hibpath;
    }

    // Helper Methods
    private void initHibernate() throws HibernateException {
        // Load Configuration and build SessionFactory
        String path = getPath();
        if (path!=null)
            configuration =  (new Configuration()).configure(new File(path));
        else
            configuration = new Configuration().configure();
        sessionFactory = configuration.buildSessionFactory();
    }

    public void shutdownHibernate() throws HibernateException {
        if (session!=null) {
            session.close();
        }
    }

    public void updateSchema() throws HibernateException {
        SchemaUpdate schemaupdate = new SchemaUpdate(configuration);
        schemaupdate.execute(true);
    }


    public void checkHibernate() throws HibernateException {

        if (sessionFactory==null) {
            initHibernate();

            /* Check Schema */
            if (sessionFactory!=null) {
                updateSchema();
            }
        }
    }

    public void beginTransaction()
            throws HibernateException {

        setSession(sessionFactory.openSession());
        transaction = getSession().beginTransaction();
    }

    public void endTransaction(boolean commit)
            throws HibernateException {

        if (commit) {
            transaction.commit();
        } else {
            // Don't commit the transaction, can be faster for read-only operations
            transaction.rollback();
        }
        getSession().close();
    }


    public void saveXWikiDoc(XWikiDocInterface doc) throws XWikiException {
        //To change body of implemented methods use Options | File Templates.
        try {
            doc.setStore(this);

            checkHibernate();
            beginTransaction();

            saveAttachmentList(doc, false);

            // Handle the latest text file
            if (doc.isContentDirty()||doc.isMetaDataDirty()) {
                doc.setDate(new Date());
                doc.incrementVersion();
                doc.updateArchive(doc.toXML());
            }

            // Verify if the document already exists
            Query query = getSession().createQuery("select xwikidoc.id from XWikiSimpleDoc as xwikidoc where xwikidoc.id = :id");
            query.setLong("id", doc.getId());
            if (query.uniqueResult()==null)
                getSession().save(doc);
            else
                getSession().update(doc);

            BaseClass bclass = doc.getxWikiClass();
            if (bclass!=null) {
                bclass.setName(doc.getFullName());
                if (bclass.getFields().size()>0)
                    saveXWikiClass(bclass, false);
            } else {
                // TODO: Remove existing class
            }

            // TODO: Delete all objects for which we don't have a name in the Map..

            Iterator it = doc.getxWikiObjects().values().iterator();
            while (it.hasNext()) {
                Vector objects = (Vector) it.next();
                for (int i=0;i<objects.size();i++) {
                    BaseObject obj = (BaseObject)objects.get(i);
                    saveXWikiObject(obj, false);
                }
                // Delete all objects of this class that have a bigger ID
                String squery = "from BaseObject as bobject where bobject.name = '" + doc.getFullName()
                        + "' and bobject.className = '" + ((BaseObject)objects.get(0)).getxWikiClass().getName()
                        + "' and bobject.number >= " + objects.size();
                int result = getSession().delete(squery);
                System.err.println("Deleted " + result + " instances");
            }

            endTransaction(true);
            doc.setNew(false);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_FILE,
                    "Exception while saving document {0}", e, args);
        }

    }


    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface doc) throws XWikiException {
        //To change body of implemented methods use Options | File Templates.
        BufferedReader fr = null;
        try {
            doc.setStore(this);
            checkHibernate();
            beginTransaction();

            try {
                getSession().load(doc, new Long(doc.getId()));
                doc.setNew(false);
            } catch (ObjectNotFoundException e)
            { // No document
                doc.setNew(true);
                return doc;
            }
            Map bclasses = new HashMap();

            // Loading the attachment list
            loadAttachmentList(doc, false);

            // TODO: handle the case where there are no xWikiClass and xWikiObject in the Database
            BaseClass bclass = new BaseClass();
            bclass.setName(doc.getFullName());
            loadXWikiClass(bclass, false);
            doc.setxWikiClass(bclass);
            bclasses.put(doc.getFullName(), bclass);

            // Find the list of classes for which we have an object
            Query query = getSession().createQuery("select bobject.name, bobject.className, bobject.number from BaseObject as bobject where bobject.name = :name order by bobject.number");
            query.setText("name", doc.getFullName());
            Iterator it = query.list().iterator();

            while (it.hasNext()) {
                Object[] result = (Object[]) it.next();
                String name = (String)result[0];
                String classname = (String)result[1];
                Integer nb = (Integer)result[2];
                if (!classname.equals("")) {
                    BaseClass objclass;
                    objclass = (BaseClass) bclasses.get(classname);
                    if (objclass==null) {
                        objclass = new BaseClass();
                        objclass.setName(classname);
                        loadXWikiClass(objclass, false);
                        bclasses.put(classname, objclass);
                    }

                    BaseObject object = new BaseObject();
                    object.setNumber(nb.intValue());
                    object.setName(doc.getFullName());
                    object.setxWikiClass(objclass);
                    loadXWikiObject(object, false);
                    doc.setObject(objclass.getName(), nb.intValue(), object);
                }
            }

            endTransaction(true);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_FILE,
                    "Exception while reading document {0}", e, args);
        }
        return doc;
    }

    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface basedoc,String version) throws XWikiException {
        XWikiDocInterface doc = new XWikiSimpleDoc(basedoc.getWeb(), basedoc.getName());
        try {
            doc.setStore(this);
            Archive archive = basedoc.getRCSArchive();

            if (archive == null) {
                doc.updateArchive(doc.toXML());
                archive = basedoc.getRCSArchive();
            }

            Object[] text = (Object[]) archive.getRevision(version);
            if (text[0].toString().startsWith("<")) {
                StringBuffer content = new StringBuffer();
                for (int i=0;i<text.length;i++) {
                    String line = text[i].toString();
                    content.append(line);
                    content.append("\n");
                }
                doc.fromXML(content.toString());
            } else {
                StringBuffer content = new StringBuffer();
                boolean bMetaDataDone = false;
                for (int i=0;i<text.length;i++) {
                    String line = text[i].toString();
                    if (bMetaDataDone||(parseMetaData(doc,line)==false)) {
                        content.append(line);
                        content.append("\n");
                    }
                    doc.setContent(content.toString());
                }
            }
            // Make sure the document has the same name
            // as the new document (in case there was a name change
            doc.setName(basedoc.getName());
            doc.setWeb(basedoc.getWeb());
        } catch (Exception e) {
            Object[] args = { doc.getFullName(), version.toString() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_VERSION,
                    "Exception while reading document {0} version {1}", e, args);
        }
        return doc;
    }

    public Version[] getXWikiDocVersions(XWikiDocInterface doc) throws XWikiException {
        try {
            doc.setStore(this);
            checkHibernate();
            beginTransaction();
            getSession().load(doc, new Long(doc.getId()));
            endTransaction(true);
            Node[] nodes = doc.getRCSArchive().changeLog();
            Version[] versions = new Version[nodes.length];
            for (int i=0;i<nodes.length;i++) {
                versions[i] = nodes[i].getVersion();
            }
            return versions;
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_REVISIONS,
                    "Exception while reading document {0} revisions", e, args);
        }
    }

    public void saveXWikiObject(BaseObject object, boolean bTransaction) throws XWikiException {
        try {

            if (bTransaction) {
                checkHibernate();
                beginTransaction();
            }

            // Verify if the property already exists
            Query query = getSession().createQuery("select obj.id from BaseObject as obj where obj.id = :id");
            query.setInteger("id", object.getId());
            if (query.uniqueResult()==null)
                getSession().save(object);
            else
                getSession().update(object);

            Collection coll = object.getFields().values();
            Iterator it = coll.iterator();
            while (it.hasNext()) {
                BaseProperty prop = (BaseProperty) it.next();
                saveXWikiProperty(prop, false);
            }

            if (bTransaction) {
                endTransaction(true);
            }
        } catch (Exception e) {
            Object[] args = { object.getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT,
                    "Exception while saving object {0}", e, args);

        }

    }


    public void loadXWikiObject(BaseObject object, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate();
                beginTransaction();
            }

            try {
                getSession().load(object, new Integer(object.getId()));
            }
            catch (ObjectNotFoundException e) {
                // There is no object data saved
                object = null;
                return;
            }
            HashMap map = new HashMap();
            Query query = session.createQuery("select prop.name, prop.classType from BaseProperty as prop where prop.id.id = :id");
            query.setInteger("id", object.getId());
            Iterator it = query.list().iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                Object[] result = (Object[]) obj;
                String name = (String)result[0];
                String classType = (String)result[1];
                BaseProperty property = (BaseProperty) Class.forName(classType).newInstance();
                property.setObject(object);
                property.setName(name);
                getSession().load(property, property);
                map.put(name, property);
            }
            object.setFields(map);

            if (bTransaction) {
                endTransaction(true);
            }
        } catch (Exception e) {
            Object[] args = { object.getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                    "Exception while loading object {0}", e, args);

        }

    }

    public void saveXWikiProperty(PropertyInterface property, boolean bTransaction) throws XWikiException
    {
        try {
            if (bTransaction) {
                checkHibernate();
                beginTransaction();
            }

            // I'm using a local transaction
            // There might be implications to this for a wider transaction
            Transaction ltransaction = getSession().beginTransaction();

            // Use to chose what to delete
            boolean isSave = false;
            try
            {
                Query query = getSession().createQuery("select prop.name from BaseProperty as prop where prop.id.id = :id and prop.id.name= :name");
                query.setInteger("id", property.getId());
                query.setString("name", property.getName());
                if (query.uniqueResult()==null) {
                    isSave = true;
                    getSession().save(property);
                }
                else {
                    isSave = false;
                    getSession().update(property);
                }

                getSession().flush();
                ltransaction.commit();
            } catch (Exception e) {
                // This seems to have failed..
                // This is an attempt to cleanup a potential mess
                // This code is only called if the tables are in an incoherent state
                // (Example: data in xwikiproperties and no data in xwikiintegers or vice-versa)
                // TODO: verify of the code works with longer transactions
                BaseProperty prop2;
                // Depending on save/update there is too much data either
                // in the BaseProperty table or in the inheritated property table
                // We need to delete this data
                if (isSave)
                    prop2 = (BaseProperty) property;
                else
                    prop2 = new BaseProperty();

                prop2.setName(property.getName());
                prop2.setObject(property.getObject());
                ltransaction.rollback();

                // We need to run the delete in a separate session
                // This is not a problem since this is cleaning up
                Session session2 = sessionFactory.openSession();
                Transaction transaction2 = session2.beginTransaction();
                session2.delete(prop2);
                session2.flush();

                // I don't understand why I can't run this in the general session
                // This might make transactions fail
                if (!isSave)
                    session2.save(property);
                transaction2.commit();
                session2.close();
            }

            if (bTransaction)
                endTransaction(true);

        }
        catch (Exception e) {
            BaseCollection obj = property.getObject();
            Object[] args = { (obj!=null) ? obj.getName() : "unknown", property.getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                    "Exception while saving property {1} of object {0}", e, args);

        }
    }

    public void saveXWikiClass(BaseClass bclass, boolean bTransaction) throws XWikiException {
        try {

            if (bTransaction) {
                checkHibernate();
                beginTransaction();
            }

            // Verify if the property already exists
            Query query = getSession().createQuery("select obj.id from BaseClass as obj where obj.id = :id");
            query.setInteger("id", bclass.getId());
            if (query.uniqueResult()==null)
                getSession().save(bclass);
            else
                getSession().update(bclass);

            Collection coll = bclass.getFields().values();
            Iterator it = coll.iterator();
            while (it.hasNext()) {
                PropertyClass prop = (PropertyClass) it.next();
                saveXWikiClassProperty(prop, false);
            }

            if (bTransaction) {
                endTransaction(true);
            }
        } catch (Exception e) {
            Object[] args = { bclass.getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_CLASS,
                    "Exception while saving class {0}", e, args);
        }
    }


    public void loadXWikiClass(BaseClass bclass, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate();
                beginTransaction();
            }

            try {
                getSession().load(bclass, new Integer(bclass.getId()));
            }
            catch (ObjectNotFoundException e) {
                // There is no class data saved
                bclass = null;
                return;
            }
            HashMap map = new HashMap();
            Query query = session.createQuery("select prop.name, prop.classType from PropertyClass as prop where prop.id.id = :id");
            query.setInteger("id", bclass.getId());
            Iterator it = query.list().iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                Object[] result = (Object[]) obj;
                String name = (String)result[0];
                String classType = (String)result[1];
                PropertyClass property = (PropertyClass) Class.forName(classType).newInstance();
                property.setName(name);
                property.setObject(bclass);
                getSession().load(property, property);
                map.put(name, property);
            }
            bclass.setFields(map);

            if (bTransaction) {
                endTransaction(true);
            }
        } catch (Exception e) {
            Object[] args = { bclass.getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_CLASS,
                    "Exception while loading class {0}", e, args);
        }
    }

    public void saveXWikiClassProperty(PropertyClass property, boolean bTransaction) throws XWikiException
    {
        try {
            if (bTransaction) {
                checkHibernate();
                beginTransaction();
            }

            // I'm using a local transaction
            // There might be implications to this for a wider transaction
            Transaction ltransaction = getSession().beginTransaction();

            // Use to chose what to delete
            boolean isSave = false;
            try
            {
                Query query = getSession().createQuery("select prop.name from PropertyClass as prop where prop.id.id = :id and prop.id.name= :name");
                query.setInteger("id", property.getId());
                query.setString("name", property.getName());
                if (query.uniqueResult()==null) {
                    isSave = true;
                    getSession().save(property);
                }
                else {
                    isSave = false;
                    getSession().update(property);
                }

                getSession().flush();
                ltransaction.commit();
            } catch (Exception e) {
                // This seems to have failed..
                // This is an attempt to cleanup a potential mess
                // This code is only called if the tables are in an incoherent state
                // (Example: data in xwikiproperties and no data in xwikiintegers or vice-versa)
                // TODO: verify of the code works with longer transactions
                PropertyClass prop2;
                // Depending on save/update there is too much data either
                // in the BaseProperty table or in the inheritated property table
                // We need to delete this data
                if (isSave)
                    prop2 = (PropertyClass) property;
                else
                    prop2 = new PropertyClass();

                prop2.setName(property.getName());
                prop2.setObject(property.getObject());
                ltransaction.rollback();

                // We need to run the delete in a separate session
                // This is not a problem since this is cleaning up
                Session session2 = sessionFactory.openSession();
                Transaction transaction2 = session2.beginTransaction();
                session2.delete(prop2);
                session2.flush();

                // I don't understand why I can't run this in the general session
                // This might make transactions fail
                if (!isSave)
                    session2.save(property);
                transaction2.commit();
                session2.close();
            }

            if (bTransaction)
                endTransaction(true);

        }
        catch (Exception e) {
            Object[] args = { property.getObject().getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_CLASS,
                    "Exception while saving class {0}", e, args);

        }
    }

    public void loadAttachmentList(XWikiDocInterface doc, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate();
                beginTransaction();
            }
            Query query = getSession().createQuery("from XWikiAttachment as attach where attach.docId=:docid");
            query.setLong("docid", doc.getId());
            List list = query.list();
            for (int i=0;i<list.size();i++) {
                ((XWikiAttachment)list.get(i)).setDoc(doc);
            }
            doc.setAttachmentList(list);
            if (bTransaction)
                endTransaction(false);
        }
        catch (Exception e) {
            e.printStackTrace();
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCHING_ATTACHMENT,
                    "Exception while searching attachments for documents {0}", e, args);
        }
    }

    public void saveAttachmentList(XWikiDocInterface doc, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate();
                beginTransaction();
            }

            List list = doc.getAttachmentList();
            for (int i=0;i<list.size();i++) {
                XWikiAttachment attachment = (XWikiAttachment) list.get(i);
                saveAttachment(attachment, false);
            }

            if (bTransaction)
                endTransaction(true);
        }
        catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT_LIST,
                    "Exception while saving attachments attachment list of document {0}", e, args);
        }
    }

    public void saveAttachment(XWikiAttachment attachment, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate();
                beginTransaction();
            }

            Query query = getSession().createQuery("select attach.id from XWikiAttachment as attach where attach.id = :id");
            query.setLong("id", attachment.getId());
            if (query.uniqueResult()==null)
             session.save(attachment);
            else
             session.update(attachment);

            if (bTransaction)
                endTransaction(true);
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
                    "Exception while saving attachments for attachment {0} of document {1}", e, args);
        }
    }

    public void saveAttachmentContent(XWikiAttachment attachment, boolean bTransaction) throws XWikiException {
        try {
            XWikiAttachmentContent content = attachment.getAttachment_content();
            if (content.isContentDirty()) {
                attachment.updateContentArchive();
            }
            XWikiAttachmentArchive archive = attachment.getAttachment_archive();

            if (bTransaction) {
                checkHibernate();
                beginTransaction();
            }

            Query query = getSession().createQuery("select attach.id from XWikiAttachmentContent as attach where attach.id = :id");
            query.setLong("id", content.getId());
            if (query.uniqueResult()==null)
             session.save(content);
            else
             session.update(content);

            query = getSession().createQuery("select attach.id from XWikiAttachmentArchive as attach where attach.id = :id");
            query.setLong("id", archive.getId());
            if (query.uniqueResult()==null)
             session.save(archive);
            else
             session.update(archive);

            if (bTransaction)
                endTransaction(true);
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
                    "Exception while saving attachment {0} of document {1}", e, args);
        }

    }

    public void loadAttachmentContent(XWikiAttachment attachment, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate();
                beginTransaction();
            }

            XWikiAttachmentContent content = new XWikiAttachmentContent(attachment);
            attachment.setAttachment_content(content);

            session.load(content, new Long(content.getId()));

            if (bTransaction)
                endTransaction(false);
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_ATTACHMENT,
                    "Exception while loading attachment {0} of document {1}", e, args);
        }
    }

    public void loadAttachmentArchive(XWikiAttachment attachment, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate();
                beginTransaction();
            }

            XWikiAttachmentArchive archive = new XWikiAttachmentArchive();
            archive.setAttachment(attachment);
            attachment.setAttachment_archive(archive);

            session.load(archive, new Long(archive.getId()));

            if (bTransaction)
                endTransaction(false);
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_ATTACHMENT,
                    "Exception while loading attachment {0} of document {1}", e, args);
        }
    }

    public void getContent(XWikiDocInterface doc, StringBuffer buf) {
        buf.append(doc.getContent());
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public List getClassList() throws XWikiException {
        try {
            checkHibernate();
            beginTransaction();
            Query query = getSession().createQuery("select bclass.name from BaseClass as bclass");
            Iterator it = query.list().iterator();
            List list = new ArrayList();
            while (it.hasNext()) {
                String name = (String)it.next();
                list.add(name);
            }
            endTransaction(false);
            return list;
        }
        catch (Exception e) {
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                    "Exception while searching class list", e);
        }
    }

    public List searchDocuments(String wheresql) throws XWikiException {
        return searchDocuments(wheresql,0,0);
    }

    public List searchDocuments(String wheresql, int nb, int start) throws XWikiException {
        try {
            checkHibernate();
            beginTransaction();
            StringBuffer sql = new StringBuffer("select doc.web, doc.name from XWikiSimpleDoc as doc");
            wheresql.trim();
            if (!wheresql.equals("")) {
                if ((!wheresql.startsWith("where"))&&(!wheresql.startsWith(",")))
                    sql.append(" where ");
                else
                    sql.append(" ");

                sql.append(wheresql);
            }
            Query query = getSession().createQuery(sql.toString());
            if (start!=0)
                query.setFirstResult(start);
            if (nb!=0)
                query.setMaxResults(nb);
            Iterator it = query.list().iterator();
            List list = new ArrayList();
            while (it.hasNext()) {
                Object[] result = (Object[]) it.next();
                String name = (String) result[0] + "." + (String)result[1];
                list.add(name);
            }
            endTransaction(false);
            return list;
        }
        catch (Exception e) {
            Object[] args = { wheresql };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                    "Exception while searching documents with sql {0}", e, args);
        }
    }


}
