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
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import org.apache.commons.jrcs.rcs.*;
import java.io.*;
import java.util.Date;
import net.sf.hibernate.*;
import net.sf.hibernate.tool.hbm2ddl.SchemaUpdate;
import net.sf.hibernate.impl.SessionFactoryImpl;
import net.sf.hibernate.dialect.Dialect;
import net.sf.hibernate.cfg.*;


public class XWikiHibernateStore extends XWikiRCSFileStore {
    protected SessionFactory sessionFactory;
    protected Session session;
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


    private void updateSchema() throws HibernateException {
        SchemaUpdate schemaupdate = new SchemaUpdate(configuration);
        schemaupdate.execute(true);
    }


    private void checkHibernate() throws HibernateException {

         if (sessionFactory==null) {
          initHibernate();

          /* Check Schema */
          if (sessionFactory!=null) {
            updateSchema();
          }
        }
    }

    private void beginTransaction()
            throws HibernateException {

        session = sessionFactory.openSession();
        transaction = session.beginTransaction();
    }

    private void endTransaction(boolean commit)
            throws HibernateException {

        if (commit) {
            transaction.commit();
        } else {
            // Don't commit the transaction, can be faster for read-only operations
            transaction.rollback();
        }
        session.close();
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
            Query query = session.createQuery("select xwikidoc.id from XWikiSimpleDoc as xwikidoc where xwikidoc.id = :id");
            query.setLong("id", doc.getId());
            if (query.uniqueResult()==null)
                session.save(doc);
            else
                session.update(doc);

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
            session.load(doc, new Long(doc.getId()));
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
            session.load(doc, new Long(doc.getId()));
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
            session.load(doc, new Long(doc.getId()));
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


    public void getContent(XWikiDocInterface doc, StringBuffer buf) {
        buf.append(doc.getContent());
    }

}
