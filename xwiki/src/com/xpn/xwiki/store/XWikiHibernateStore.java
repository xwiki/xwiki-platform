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

import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import org.apache.commons.jrcs.rcs.*;
import org.apache.ecs.xhtml.object;

import java.io.*;
import java.util.*;

import net.sf.hibernate.*;
import net.sf.hibernate.tool.hbm2ddl.SchemaUpdate;
import net.sf.hibernate.cfg.*;


public class XWikiHibernateStore extends XWikiRCSFileStore {
    protected SessionFactory sessionFactory;
    private Session session;
    protected Transaction transaction;
    protected Configuration configuration;

    private String hibpath;

    public XWikiHibernateStore(XWiki xwiki) {
        setPath(xwiki.Param("xwiki.store.hibernate.path"));
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
        configuration =  new Configuration().configure(getPath());
        sessionFactory = configuration.buildSessionFactory();
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
            // Handle the latest text file
            if (doc.isContentDirty()||doc.isMetaDataDirty()) {
                doc.setDate(new Date());
                doc.incrementVersion();
                doc.updateArchive(getFullContent(doc));
            }

            checkHibernate();
            beginTransaction();

            // Verify if the document already exists
            Query query = getSession().createQuery("select xwikidoc.id from XWikiSimpleDoc as xwikidoc where xwikidoc.id = :id");
            query.setLong("id", doc.getId());
            if (query.uniqueResult()==null)
                getSession().save(doc);
            else
                getSession().update(doc);

            // TODO: handle the case when we delete a class or an object from a document
            BaseClass bclass = doc.getxWikiClass();
            BaseObject bobject = doc.getxWikiObject();
            if (bclass!=null) {
                bclass.setName(doc.getFullName());
                saveXWikiClass(bclass, false);
            }
            if (bobject!=null) {
                bobject.setName(doc.getFullName());
                saveXWikiObject(bobject, false);
            }
            Iterator it = doc.getxWikiObjects().values().iterator();
            while (it.hasNext()) {
                BaseObject obj = (BaseObject)it.next();
                saveXWikiObject(obj, false);
            }

            endTransaction(true);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_FILE,
                    "Exception while saving document {0}", e, args);
        }

    }

    public void loadXWikiDoc(XWikiDocInterface doc) throws XWikiException {
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
                return;
            }

            // TODO: handle the case where there are no xWikiClass and xWikiObject in the Database
            BaseClass bclass = new BaseClass();
            bclass.setName(doc.getFullName());
            loadXWikiClass(bclass, false);
            doc.setxWikiClass(bclass);
            BaseObject bobject = new BaseObject();
            bobject.setName(doc.getFullName());
            bobject.setxWikiClass(bclass);
            loadXWikiObject(bobject, false);
            doc.setxWikiObject(bobject);

            // Find the list of classes for which we have an object
            Query query = getSession().createQuery("select bobject.name, bobject.className from BaseObject as bobject where bobject.name = :name");
            query.setText("name", doc.getFullName());
            Iterator it = query.list().iterator();
            List list = new ArrayList();
            while (it.hasNext()) {
                Object[] result = (Object[]) it.next();
                String name = (String)result[0];
                String classname = (String)result[1];
                if ((!classname.equals(""))&&(!name.equals(classname))) {
                    BaseClass objclass = new BaseClass();
                    objclass.setName(classname);
                    loadXWikiClass(objclass, false);
                    BaseObject object = new BaseObject();
                    object.setName(doc.getFullName());
                    object.setxWikiClass(objclass);
                    loadXWikiObject(object, false);
                    doc.getxWikiObjects().put(objclass.getName(), object);
                }
            }

            endTransaction(true);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_FILE,
                    "Exception while reading document {0}", e, args);
        }
    }

    public void loadXWikiDoc(XWikiDocInterface doc,String version) throws XWikiException {
        //To change body of implemented methods use Options | File Templates.
        try {
            doc.setStore(this);
            checkHibernate();
            beginTransaction();
            getSession().load(doc, new Long(doc.getId()));
            endTransaction(true);
            Object[] text = (Object[]) doc.getRCSArchive().getRevision(version);
            StringBuffer content = new StringBuffer();
            boolean bMetaDataDone = false;
            for (int i=0;i<text.length;i++) {
                String line = text[i].toString();
                if (bMetaDataDone||(parseMetaData(doc,line)==false)) {
                    content.append(line);
                    content.append("\n");
                }
            }
            doc.setContent(content.toString());
            doc.setVersion(version);
        } catch (Exception e) {
            Object[] args = { doc.getFullName(), version.toString() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_VERSION,
                    "Exception while reading document {0} version {1}", e, args);
        }
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

    public XWikiDocCacheInterface newDocCache() {
        return null;  //To change body of implemented methods use Options | File Templates.
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
            Object[] args = { property.getObject().getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                    "Exception while saving object {0}", e, args);

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
