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
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import net.sf.hibernate.*;
import net.sf.hibernate.cfg.Configuration;
import net.sf.hibernate.dialect.Dialect;
import net.sf.hibernate.impl.SessionFactoryImpl;
import net.sf.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.Node;
import org.apache.commons.jrcs.rcs.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;


public class XWikiHibernateStore implements XWikiStoreInterface {

    private SessionFactory sessionFactory;
    private Configuration configuration;

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
            setConfiguration((new Configuration()).configure(new File(path)));
        else
            setConfiguration(new Configuration().configure());
        setSessionFactory(getConfiguration().buildSessionFactory());
    }

    public Session getSession(XWikiContext context) {
        Session session = (Session) context.get("hibsession");
        return session;
    }

    public void setSession(Session session, XWikiContext context) {
        if (session==null)
            context.remove("hibsession");
        else
            context.put("hibsession", session);
    }


    public Transaction getTransaction(XWikiContext context) {
        Transaction transaction = (Transaction) context.get("hibtransaction");
        return transaction;
    }

    public void setTransaction(Transaction transaction, XWikiContext context) {
        if (transaction==null)
            context.remove("hibtransaction");
        else
            context.put("hibtransaction", transaction);
    }


    public void shutdownHibernate(XWikiContext context) throws HibernateException {
        Session session = getSession(context);

        if (session!=null) {
            session.close();
        }
        if (getSessionFactory()!=null) {
            ((SessionFactoryImpl)getSessionFactory()).getConnectionProvider().close();
        }
    }

    public void updateSchema(XWikiContext context) throws HibernateException {
        Session session;
        Transaction transaction;
        Connection connection;
        DatabaseMetadata meta;
        Statement stmt=null;
        Dialect dialect = Dialect.getDialect(getConfiguration().getProperties());

		try {
			try {
                transaction = beginTransaction(context);
                session = getSession(context);
                connection = session.connection();
                setDatabase(session, context);
                // we need to get the dialect
				meta = new DatabaseMetadata(connection, dialect);
				stmt = connection.createStatement();
			}
			catch (SQLException sqle) {
                System.err.println("Failed updating schema: " + sqle.getMessage());
				throw sqle;
			}

			String[] createSQL = configuration.generateSchemaUpdateScript(dialect, meta);
			for (int j = 0; j < createSQL.length; j++) {

				final String sql = createSQL[j];
				try {
					System.out.println(sql);
					stmt.executeUpdate(sql);
                    connection.commit();
				}
				catch (SQLException e) {
                    connection.rollback();
                    System.err.println("Failed updating schema: " + e.getMessage());
					// log.error( "Unsuccessful: " + sql );
					//log.error( e.getMessage() );
				}
			}
		}
		catch (Exception e) {
			  System.err.println("Failed updating schema: " + e.getMessage());
		}
		finally {

			try {
				if (stmt!=null) stmt.close();
                endTransaction(context, true);
			}
			catch (Exception e) {
			}
		}
    }


    public void checkHibernate(XWikiContext context) throws HibernateException {

        if (getSessionFactory()==null) {
            initHibernate();

            /* Check Schema */
            if (getSessionFactory()!=null) {
                updateSchema(context);
            }
        }
    }

    public void setDatabase(Session session, XWikiContext context) throws XWikiException {
        String database = context.getDatabase();
        try {
            System.out.println("Switch database to: " + database);
             if (database!=null)
              session.connection().setCatalog(database);
        } catch (Exception e) {
            Object[] args = { database };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SWITCH_DATABASE,
                    "Exception while switching to database {0}", e, args);
        }
    }

    public Transaction beginTransaction(XWikiContext context)
            throws HibernateException, XWikiException {

        Transaction transaction;
        Session session = getSession(context);
        if (session==null) {
         session = getSessionFactory().openSession();
         setSession(session, context);
         setDatabase(session, context);
         try {
             String database = context.getDatabase();
             if ((database!=null)&&(!database.equals("")))
              session.connection().setCatalog(database);
         } catch (Exception e) {
             System.err.println("Failed to setup catalog " + context.getDatabase());
         }

         transaction = session.beginTransaction();
         setTransaction(transaction, context);
        } else {
           transaction = getTransaction(context);
           // transaction = session.beginTransaction();
           // setTransaction(transaction, context);
        }
        return transaction;
    }


    public void endTransaction(XWikiContext context, boolean commit)
            throws HibernateException {

        Session session = getSession(context);
        Transaction transaction = getTransaction(context);
        setSession(null, context);
        setTransaction(null, context);

        if (commit) {
            transaction.commit();
        } else {
            // Don't commit the transaction, can be faster for read-only operations
            transaction.rollback();
        }
        session.close();
    }


    public void saveXWikiDoc(XWikiDocInterface doc, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            doc.setStore(this);

            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);

            saveAttachmentList(doc, context, false);

            // Handle the latest text file
            if (doc.isContentDirty()||doc.isMetaDataDirty()) {
                doc.setDate(new Date());
                doc.incrementVersion();
                doc.updateArchive(doc.toXML());
            }

            // Verify if the document already exists
            Query query = session.createQuery("select xwikidoc.id from XWikiSimpleDoc as xwikidoc where xwikidoc.id = :id");
            query.setLong("id", doc.getId());
            if (query.uniqueResult()==null)
                session.save(doc);
            else
                session.update(doc);

            // Remove properties planned for removal
            if (doc.getObjectsToRemove().size()>0) {
               for (int i=0;i<doc.getObjectsToRemove().size();i++) {
                   deleteXWikiObject((BaseObject)doc.getObjectsToRemove().get(i), context, false);
               }
               doc.setObjectsToRemove(new ArrayList());
            }

            BaseClass bclass = doc.getxWikiClass();
            if (bclass!=null) {
                bclass.setName(doc.getFullName());
                if (bclass.getFields().size()>0)
                    saveXWikiClass(bclass, context, false);
            } else {
                // TODO: Remove existing class
            }

            // TODO: Delete all objects for which we don't have a name in the Map..

            Iterator it = doc.getxWikiObjects().values().iterator();
            while (it.hasNext()) {
                Vector objects = (Vector) it.next();
                for (int i=0;i<objects.size();i++) {
                    BaseObject obj = (BaseObject)objects.get(i);
                    saveXWikiObject(obj, context, false);
                }
            }

            if (bTransaction) {
               endTransaction(context, true);
            }
            doc.setNew(false);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC,
                    "Exception while saving document {0}", e, args);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
          } catch (Exception e) {}
        }

    }

    public void saveXWikiDoc(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        saveXWikiDoc(doc, context, true);
    }


    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        //To change body of implemented methods use Options | File Templates.
        BufferedReader fr = null;
        try {
            doc.setStore(this);
            checkHibernate(context);
            beginTransaction(context);
            Session session = getSession(context);

            try {
                session.load(doc, new Long(doc.getId()));
                doc.setNew(false);
            } catch (ObjectNotFoundException e)
            { // No document
                doc.setNew(true);
                return doc;
            }
            Map bclasses = new HashMap();

            // Loading the attachment list
            loadAttachmentList(doc, context, false);

            // TODO: handle the case where there are no xWikiClass and xWikiObject in the Database
            BaseClass bclass = new BaseClass();
            bclass.setName(doc.getFullName());
            loadXWikiClass(bclass, context, false);
            doc.setxWikiClass(bclass);
            bclasses.put(doc.getFullName(), bclass);

            // Find the list of classes for which we have an object
            Query query = session.createQuery("select bobject.name, bobject.className, bobject.number from BaseObject as bobject where bobject.name = :name order by bobject.number");
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
                        loadXWikiClass(objclass, context, false);
                        bclasses.put(classname, objclass);
                    }

                    BaseObject object = new BaseObject();
                    object.setNumber(nb.intValue());
                    object.setName(doc.getFullName());
                    object.setxWikiClass(objclass);
                    loadXWikiObject(object, context, false);
                    doc.setObject(objclass.getName(), nb.intValue(), object);
                }
            }

            endTransaction(context, false);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC,
                    "Exception while reading document {0}", e, args);
        } finally {
            try {
                  endTransaction(context, false);
            } catch (Exception e) {}
        }
        return doc;
    }

    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface basedoc,String version, XWikiContext context) throws XWikiException {
        XWikiDocInterface doc = new XWikiSimpleDoc(basedoc.getWeb(), basedoc.getName());
        try {
            doc.setStore(this);
            Archive archive = basedoc.getRCSArchive();
            doc.setRCSArchive(archive);

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
                    if (bMetaDataDone||(XWikiRCSFileStore.parseMetaData(doc,line)==false)) {
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

    public void deleteXWikiDoc(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        try {
            checkHibernate(context);
            beginTransaction(context);
            Session session = getSession(context);

            if (doc.getStore()==null) {
                Object[] args = { doc.getFullName() };
                throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CANNOT_DELETE_UNLOADED_DOC,
                        "Impossible to delete document {0} if it is not loaded", null, args);
            }

            // Let's delete any attachment this document might have
            List attachlist = doc.getAttachmentList();
            for (int i=0;i<attachlist.size();i++) {
                XWikiAttachment attachment = (XWikiAttachment) attachlist.get(i);
                deleteXWikiAttachment(attachment, context, false);
            }

            BaseClass bclass = doc.getxWikiClass();
            if ((bclass==null)&&(bclass.getName()!=null)) {
                deleteXWikiClass(bclass, context, false);
            }

            // Find the list of classes for which we have an object
            // Remove properties planned for removal
            if (doc.getObjectsToRemove().size()>0) {
                for (int i=0;i<doc.getObjectsToRemove().size();i++) {
                    deleteXWikiObject((BaseObject)doc.getObjectsToRemove().get(i), context, false);
                }
                doc.setObjectsToRemove(new ArrayList());
            }
            Iterator it = doc.getxWikiObjects().values().iterator();
            while (it.hasNext()) {
                Vector objects = (Vector) it.next();
                for (int i=0;i<objects.size();i++) {
                    BaseObject obj = (BaseObject)objects.get(i);
                    deleteXWikiObject(obj, context, false);
                }
            }

            session.delete(doc);
            endTransaction(context, true);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_DOC,
                    "Exception while deleting document {0}", e, args);
        }finally {
            try {
                endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    public Version[] getXWikiDocVersions(XWikiDocInterface doc) throws XWikiException {
        return getXWikiDocVersions(doc, null);
    }

    public Version[] getXWikiDocVersions(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        try {
            if (doc.getStore()==null) {
                doc = loadXWikiDoc(doc, context);
            }
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

    public void saveXWikiObject(BaseObject object, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            // Nothing to save
            if (object==null)
                return;

            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);

            // Verify if the property already exists
            Query query = session.createQuery("select obj.id from BaseObject as obj where obj.id = :id");
            query.setInteger("id", object.getId());
            if (query.uniqueResult()==null)
                session.save(object);
            else
                session.update(object);

            // Remove all existing properties
            if (object.getFieldsToRemove().size()>0) {
               for (int i=0;i<object.getFieldsToRemove().size();i++) {
                   session.delete(object.getFieldsToRemove().get(i));
               }
               object.setFieldsToRemove(new ArrayList());
            }

            Iterator it = object.getFields().keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                BaseProperty prop = (BaseProperty) object.getFields().get(key);
                if (!prop.getName().equals(key)) {
                    Object[] args = { key, object.getName() };
                    throw new XWikiException(XWikiException.MODULE_XWIKI_CLASSES, XWikiException.ERROR_XWIKI_CLASSES_FIELD_INVALID,
                            "Field {0} in object {1} has an invalid name", null, args);
                }
                saveXWikiProperty(prop, context, false);
            }

            if (bTransaction) {
                endTransaction(context, true);
            }
        } catch (XWikiException xe) {
            throw xe;
        } catch (Exception e) {
            Object[] args = { object.getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT,
                    "Exception while saving object {0}", e, args);

        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    public void loadXWikiObject(BaseObject object, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);


            try {
                session.load(object, new Integer(object.getId()));
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
                loadXWikiProperty(property, context, false);
                map.put(name, property);
            }
            object.setFields(map);

            if (bTransaction) {
                    endTransaction(context, false);
            }
        } catch (Exception e) {
            Object[] args = { object.getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                    "Exception while loading object {0}", e, args);

        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }

    }

    public void deleteXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);
            for (Iterator it = object.getFields().values().iterator(); it.hasNext();) {
                    BaseProperty property = (BaseProperty)it.next();
                    session.delete(property);
            }
            session.delete(object);
            if (bTransaction) {
                    endTransaction(context, false);
            }
        } catch (Exception e) {
            Object[] args = { object.getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_OBJECT,
                    "Exception while deleting object {0}", e, args);
        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    public void deleteXWikiObject(BaseObject baseObject, XWikiContext context, boolean b) throws XWikiException {
        deleteXWikiCollection(baseObject, context, b);
    }

    public void deleteXWikiClass(BaseClass baseClass, XWikiContext context, boolean b) throws XWikiException {
        deleteXWikiCollection(baseClass, context, b);
    }


    public void loadXWikiProperty(PropertyInterface property, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        try {
            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);


            session.load(property, (Serializable) property);

            if (bTransaction) {
                endTransaction(context, false);
            }
        }
        catch (Exception e) {
            BaseCollection obj = property.getObject();
            Object[] args = { (obj!=null) ? obj.getName() : "unknown", property.getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                    "Exception while saving property {1} of object {0}", e, args);

        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }


    public void saveXWikiProperty(PropertyInterface property, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        try {
            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);

// I'm using a local transaction
// There might be implications to this for a wider transaction
            Transaction ltransaction = session.beginTransaction();

// Use to chose what to delete
            boolean isSave = false;
            try
            {
                Query query = session.createQuery("select prop.name from BaseProperty as prop where prop.id.id = :id and prop.id.name= :name");
                query.setInteger("id", property.getId());
                query.setString("name", property.getName());
                if (query.uniqueResult()==null) {
                    isSave = true;
                session.save(property);
                }
                else {
                    isSave = false;
                    session.update(property);
                }
                session.flush();
                ltransaction.commit();
            } catch (Exception e) {
// We can't clean-up ListProperties
                if (property instanceof ListProperty)
                    throw e;

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
                Session session2 = getSessionFactory().openSession();
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
                endTransaction(context, true);

        }
        catch (Exception e) {
            BaseCollection obj = property.getObject();
            Object[] args = { (obj!=null) ? obj.getName() : "unknown", property.getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                    "Exception while saving property {1} of object {0}", e, args);

        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    public void saveXWikiClass(BaseClass bclass, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {

            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);


// Verify if the property already exists
            Query query = session.createQuery("select obj.id from BaseClass as obj where obj.id = :id");
            query.setInteger("id", bclass.getId());
            if (query.uniqueResult()==null)
                session.save(bclass);
            else
                session.update(bclass);

            // Remove all existing properties
            if (bclass.getFieldsToRemove().size()>0) {
               for (int i=0;i<bclass.getFieldsToRemove().size();i++) {
                   session.delete(bclass.getFieldsToRemove().get(i));
               }
               bclass.setFieldsToRemove(new ArrayList());
            }


            Collection coll = bclass.getFields().values();
            Iterator it = coll.iterator();
            while (it.hasNext()) {
                PropertyClass prop = (PropertyClass) it.next();
                saveXWikiClassProperty(prop, context, false);
            }

            if (bTransaction) {
                endTransaction(context, true);
            }
        } catch (Exception e) {
            Object[] args = { bclass.getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_CLASS,
                    "Exception while saving class {0}", e, args);
        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }


    public void loadXWikiClass(BaseClass bclass, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);


            try {
                session.load(bclass, new Integer(bclass.getId()));
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
                session.load(property, property);
                map.put(name, property);
            }
            bclass.setFields(map);

            if (bTransaction) {
                endTransaction(context, true);
            }
        } catch (Exception e) {
            Object[] args = { bclass.getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_CLASS,
                    "Exception while loading class {0}", e, args);
        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    public void saveXWikiClassProperty(PropertyClass property, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        try {
            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);


// I'm using a local transaction
// There might be implications to this for a wider transaction
            Transaction ltransaction = session.beginTransaction();

// Use to chose what to delete
            boolean isSave = false;
            try
            {
                Query query = session.createQuery("select prop.name from PropertyClass as prop where prop.id.id = :id and prop.id.name= :name");
                query.setInteger("id", property.getId());
                query.setString("name", property.getName());
                if (query.uniqueResult()==null) {
                    isSave = true;
                    session.save(property);
                }
                else {
                    isSave = false;
                    session.update(property);
                }

                session.flush();
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
                Session session2 = getSessionFactory().openSession();
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
                endTransaction(context, true);

        }
        catch (Exception e) {
            Object[] args = { property.getObject().getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_CLASS,
                    "Exception while saving class {0}", e, args);

        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    public void loadAttachmentList(XWikiDocInterface doc, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);

            Query query = session.createQuery("from XWikiAttachment as attach where attach.docId=:docid");
            query.setLong("docid", doc.getId());
            List list = query.list();
            for (int i=0;i<list.size();i++) {
                ((XWikiAttachment)list.get(i)).setDoc(doc);
            }
            doc.setAttachmentList(list);
            if (bTransaction)
                endTransaction(context, false);
        }
        catch (Exception e) {
            e.printStackTrace();
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCHING_ATTACHMENT,
                    "Exception while searching attachments for documents {0}", e, args);
        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    public void saveAttachmentList(XWikiDocInterface doc, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);


            List list = doc.getAttachmentList();
            for (int i=0;i<list.size();i++) {
                XWikiAttachment attachment = (XWikiAttachment) list.get(i);
                saveAttachment(attachment, context, false);
            }

            if (bTransaction)
                endTransaction(context, true);
        }
        catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT_LIST,
                    "Exception while saving attachments attachment list of document {0}", e, args);
        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    public void saveAttachment(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);


            Query query = session.createQuery("select attach.id from XWikiAttachment as attach where attach.id = :id");
            query.setLong("id", attachment.getId());
            if (query.uniqueResult()==null)
                session.save(attachment);
            else
                session.update(attachment);

            if (bTransaction) {
                saveXWikiDoc(attachment.getDoc(), context, false);
                endTransaction(context, true);
            }
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
                    "Exception while saving attachments for attachment {0} of document {1}", e, args);
        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            XWikiAttachmentContent content = attachment.getAttachment_content();
            if (content.isContentDirty()) {
                attachment.updateContentArchive(context);
            }
            XWikiAttachmentArchive archive = attachment.getAttachment_archive();

            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);


            Query query = session.createQuery("select attach.id from XWikiAttachmentContent as attach where attach.id = :id");
            query.setLong("id", content.getId());
            if (query.uniqueResult()==null)
                session.save(content);
            else
                session.update(content);

            query = session.createQuery("select attach.id from XWikiAttachmentArchive as attach where attach.id = :id");
            query.setLong("id", archive.getId());
            if (query.uniqueResult()==null)
                session.save(archive);
            else
                session.update(archive);

            if (bTransaction) {
                saveXWikiDoc(attachment.getDoc(), context, false);
                endTransaction(context, true);
            }
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
                    "Exception while saving attachment {0} of document {1}", e, args);
        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }

    }

    public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);


            XWikiAttachmentContent content = new XWikiAttachmentContent(attachment);
            attachment.setAttachment_content(content);

            session.load(content, new Long(content.getId()));

            if (bTransaction)
                endTransaction(context, false);
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_ATTACHMENT,
                    "Exception while loading attachment {0} of document {1}", e, args);
        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    public void loadAttachmentArchive(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
              checkHibernate(context);
              beginTransaction(context);
            }
            Session session = getSession(context);


            XWikiAttachmentArchive archive = new XWikiAttachmentArchive();
            archive.setAttachment(attachment);
            attachment.setAttachment_archive(archive);

            session.load(archive, new Long(archive.getId()));

            if (bTransaction)
                endTransaction(context, false);
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_ATTACHMENT,
                    "Exception while loading attachment {0} of document {1}", e, args);
        } finally {
            try {
                  if (bTransaction)
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }


    public void deleteXWikiAttachment(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
       try {
            if (bTransaction) {
               checkHibernate(context);
               beginTransaction(context);
            }

            Session session = getSession(context);

            // Delete the three attachement entries
           loadAttachmentContent(attachment, context, false);
           session.delete(attachment.getAttachment_content());
           loadAttachmentArchive(attachment, context, false);
           session.delete(attachment.getAttachment_archive());
           session.delete(attachment);

           if (bTransaction) {
                List list = attachment.getDoc().getAttachmentList();
                for (int i=0;i<list.size();i++) {
                    XWikiAttachment attach = (XWikiAttachment) list.get(i);
                    if (attachment.getFilename().equals(attach.getFilename())) {
                        list.remove(i);
                        break;
                    }
                }
                saveXWikiDoc(attachment.getDoc(), context, false);
                endTransaction(context, true);
              }
            }
            catch (Exception e) {
                Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
                throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_ATTACHMENT,
                        "Exception while deleting attachment {0} of document {1}", e, args);
            } finally {
                try {
                      if (bTransaction)
                       endTransaction(context, false);
                } catch (Exception e) {}
            }
        }


    public void getContent(XWikiDocInterface doc, StringBuffer buf) {
        buf.append(doc.getContent());
    }

    public List getClassList(XWikiContext context) throws XWikiException {
        try {
             checkHibernate(context);
             beginTransaction(context);
             Session session = getSession(context);

            Query query = session.createQuery("select bclass.name from BaseClass as bclass");
            Iterator it = query.list().iterator();
            List list = new ArrayList();
            while (it.hasNext()) {
                String name = (String)it.next();
                list.add(name);
            }
            endTransaction(context, false);
            return list;
        }
        catch (Exception e) {
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                    "Exception while searching class list", e);
        } finally {
            try {
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    public List searchDocuments(String wheresql, XWikiContext context) throws XWikiException {
        return searchDocuments(wheresql,0,0, context);
    }

    public List search(String sql, int nb, int start, XWikiContext context) throws XWikiException {
        try {
            checkHibernate(context);
            beginTransaction(context);
            Session session = getSession(context);
            Query query = session.createQuery(sql.toString());
            if (start!=0)
                query.setFirstResult(start);
            if (nb!=0)
                query.setMaxResults(nb);
            Iterator it = query.list().iterator();
            List list = new ArrayList();
            while (it.hasNext()) {
                list.add(it.next());
            }
            endTransaction(context, false);
            return list;
        }
        catch (Exception e) {
            Object[] args = { sql };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                    "Exception while searching documents with sql {0}", e, args);
        } finally {
            try {
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }


    public List searchDocuments(String wheresql, int nb, int start, XWikiContext context) throws XWikiException {
        try {
            checkHibernate(context);
            beginTransaction(context);
            Session session = getSession(context);
            StringBuffer sql = new StringBuffer("select distinct doc.web, doc.name from XWikiSimpleDoc as doc");
            wheresql.trim();
            if (!wheresql.equals("")) {
                if ((!wheresql.startsWith("where"))&&(!wheresql.startsWith(",")))
                    sql.append(" where ");
                else
                    sql.append(" ");

                sql.append(wheresql);
            }
            Query query = session.createQuery(sql.toString());
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
            endTransaction(context, false);
            return list;
        }
        catch (Exception e) {
            Object[] args = { wheresql };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                    "Exception while searching documents with sql {0}", e, args);
        } finally {
            try {
                   endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }


}
