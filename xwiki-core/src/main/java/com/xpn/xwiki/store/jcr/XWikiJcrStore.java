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
 *
 */
package com.xpn.xwiki.store.jcr;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.VersionException;
import javax.transaction.NotSupportedException;

import com.xpn.xwiki.web.Utils;
import org.apache.portals.graffito.jcr.query.Filter;
import org.apache.portals.graffito.jcr.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.stats.impl.XWikiStats;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.util.Util;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

/** XWikiJCRStore - XWiki Store System backend to JCR */
public class XWikiJcrStore extends XWikiJcrBaseStore implements XWikiStoreInterface
{
    /**
     * QueryManager for this store. Injected via component manager.
     */
    org.xwiki.query.QueryManager queryManager;

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead and for the wiki name for which
     * the current wiki is used instead of the current document reference's wiki.
     */
    private DocumentReferenceResolver currentMixedDocumentReferenceResolver =
        Utils.getComponent(DocumentReferenceResolver.class, "currentmixed");

    /**
     * Used to convert a proper Document Reference to a string but without the wiki name.
     */
    private EntityReferenceSerializer<String> localEntityReferenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.class, "local");

    public XWikiJcrStore(XWiki xwiki, XWikiContext context) throws SecurityException, IllegalArgumentException,
        NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException,
        InvocationTargetException, XWikiException
    {
        super(xwiki, context);
        // create default wiki
        createWiki("xwiki", context);
    }

    public XWikiJcrStore(XWikiContext context) throws SecurityException, IllegalArgumentException,
        NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException,
        InvocationTargetException, XWikiException
    {
        this(context.getWiki(), context);
    }

    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        saveXWikiDoc(doc, context, true);
    }

    public void saveXWikiDoc(final XWikiDocument doc, final XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            // From XWikiHibernateStore:
            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer("jcr");
            }
            doc.setStore(this);
            // Make sure the database name is stored
            doc.setDatabase(context.getDatabase());

            // These informations will allow to not look for attachments and objects on loading
            doc.setElement(XWikiDocument.HAS_ATTACHMENTS, (doc.getAttachmentList().size() != 0));
            doc.setElement(XWikiDocument.HAS_OBJECTS, (doc.getxWikiObjects().size() != 0));

            // Let's update the class XML since this is the new way to store it
            final BaseClass bclass = doc.getxWikiClass();
            if ((bclass != null) && (bclass.getFieldList().size() > 0)) {
                doc.setxWikiClassXML(bclass.toXMLString());
            }
            if (bclass != null) {
                bclass.setName(doc.getFullName());
            }

            /*
             * not needed: if (doc.hasElement(XWikiDocument.HAS_ATTACHMENTS)) saveAttachmentList(doc, context, false);
             */
            boolean needUpdateArchive = false;
            boolean needSaveArchive = false;
            boolean needGetArchive = false;
            // Handle the latest text file
            if (doc.isContentDirty() || doc.isMetaDataDirty()) {
                Date ndate = new Date();
                doc.setDate(ndate);
                if (doc.isContentDirty()) {
                    doc.setContentUpdateDate(ndate);
                    doc.setContentAuthor(doc.getAuthor());
                }
                doc.incrementVersion();
                needUpdateArchive = true;
            } else {
                if (doc.getDocumentArchive() != null) {
                    // Let's make sure we save the archive if we have one
                    // This is especially needed if we load a document from XML
                    needSaveArchive = true;
                } else {
                    // Make sure the getArchive call has been made once
                    // with a valid context
                    needGetArchive = true;
                }
            }
            // end
            executeWrite(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession ses) throws RepositoryException, XWikiException
                {
                    Node storeNode = ses.getStoreNode();
                    Node spaceNode = JcrUtil.getOrCreateSubNode(storeNode, doc.getSpace(), ntXWikiSpace);
                    String docNodeName = doc.getName();
                    String docLang = doc.getLanguage();
                    if (docLang != null) {
                        docLang = docLang.trim();
                        if (!"".equals(docLang)) {
                            docNodeName += "." + docLang;
                        }
                    }
                    Node docNode = JcrUtil.getOrCreateSubNode(spaceNode, docNodeName, ntXWikiDocument);
                    Node baseDocNode = JcrUtil.getOrCreateSubNode(spaceNode, doc.getName(), ntXWikiDocument);
                    ses.updateObject(docNode, doc);

                    if (doc.hasElement(XWikiDocument.HAS_ATTACHMENTS)) {
                        Node nattachs = JcrUtil.getOrCreateSubNode(baseDocNode, "attach", ntXWikiAttachments);
                        Iterator it = doc.getAttachmentList().iterator();
                        Set attachset = new HashSet();
                        while (it.hasNext()) {
                            XWikiAttachment att = (XWikiAttachment) it.next();
                            attachset.add(att.getFilename());
                            Node natt = JcrUtil.getOrCreateSubNode(nattachs, att.getFilename(), ntXWikiAttachment);
                            ses.updateObject(natt, att);
                            natt.setProperty("doc", baseDocNode);
                        }
                        NodeIterator ni = nattachs.getNodes();
                        while (ni.hasNext()) {
                            Node natt = ni.nextNode();
                            if (!attachset.contains(natt.getName())) {
                                natt.remove();
                            }
                        }
                    } else {
                        try {
                            baseDocNode.getNode("attach").remove();
                        } catch (PathNotFoundException e) {
                        }
                        ;
                    }

                    doc.setObjectsToRemove(new ArrayList());
                    if (bclass != null) {
                        bclass.setName(doc.getFullName());
                        if (bclass.getFieldList().size() > 0) {
                            Node classNode = JcrUtil.getOrCreateSubNode(baseDocNode, "class", ntXWikiClass);
                            saveXWikiClass(ses, classNode, bclass);
                        }
                    } else {
                        ses.removeObject(baseDocNode.getPath() + "/class");
                    }

                    ses.removeObject(baseDocNode.getPath() + "/obj");
                    if (doc.hasElement(XWikiDocument.HAS_OBJECTS)) {
                        Iterator it = doc.getXObjects().values().iterator();
                        while (it.hasNext()) {
                            List objects = (List) it.next();
                            for (int i = 0; i < objects.size(); i++) {
                                BaseCollection obj = (BaseCollection) objects.get(i);
                                if (obj != null) {
                                    saveXWikiCollection(ses, baseDocNode, obj);
                                }
                            }
                        }
                    }
                    if (context.getWiki().hasBacklinks(context)) {
                        saveLinks(doc, context, false);
                    }
                    doc.setNew(false);
                    ses.save();
                    return null;
                }
            });

            if (needUpdateArchive) {
                context.getWiki().getVersioningStore().updateXWikiDocArchive(doc, false, context);
            }
            if (needSaveArchive) {
                context.getWiki().getVersioningStore().saveXWikiDocArchive(doc.getDocumentArchive(), false, context);
            }
            if (needGetArchive) {
                try {
                    doc.getDocumentArchive(context);
                } catch (XWikiException e) {
                    // this is a non critical error
                }
            }

            // We need to ensure that the saved document becomes the original document
            doc.setOriginalDocument((XWikiDocument) doc.clone());

        } catch (Exception e) {
            Object[] args = {doc.getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_JCR_SAVING_DOC, "Exception while saving document {0}", e, args);
        } finally {
            // End monitoring timer
            if (monitor != null) {
                monitor.endTimer("jcr");
            }
        }
    }

    private void saveXWikiClass(XWikiJcrSession ses, Node classNode, BaseClass bclass) throws RepositoryException
    {
        clearNodeChildrens(classNode);
        ses.updateObject(classNode, bclass, BaseClass.class);
        Iterator it = bclass.getFieldList().iterator();
        while (it.hasNext()) {
            PropertyClass pc = (PropertyClass) it.next();
            ses.insertObject(classNode, pc.getName(), pc);
        }
    }

    private Node saveXWikiCollection(XWikiJcrSession ses, Node docnode, BaseCollection object) throws XWikiException
    {
        try {
            // We need a slightly different behavior here
            boolean stats = (object instanceof XWikiStats);

            String oclassname = object.getClassName();
            int i = oclassname.lastIndexOf(".");
            String ospace = oclassname.substring(0, i);
            String oname = oclassname.substring(i + 1);
            Node wnode = docnode;
            wnode = JcrUtil.getOrCreateSubNode(wnode, "obj", ntXWikiObjects);
            if (!"".equals(ospace)) {
                wnode = JcrUtil.getOrCreateSubNode(wnode, ospace, ntXWikiSpaceObject);
            }
            Node onode;
            if (stats) {
                onode = ses.insertObject(wnode, oname, object);
            } else {
                onode = ses.insertObject(wnode, oname, object, BaseObject.class);
            }

            onode.setProperty("doc", docnode);

            if (object.getClassName().equals("internal")) {
                return onode;
            }

            Iterator it = object.getPropertyList().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                BaseProperty prop = (BaseProperty) object.getField(key);
                if (!prop.getName().equals(key)) {
                    Object[] args = {key, object.getName()};
                    throw new XWikiException(XWikiException.MODULE_XWIKI_CLASSES,
                        XWikiException.ERROR_XWIKI_CLASSES_FIELD_INVALID,
                        "Field {0} in object {1} has an invalid name", null, args);
                }
                String pname = prop.getName();
                if (pname != null && !pname.trim().equals("")) {
                    saveXWikiProperty(ses, onode, prop);
                }
            }
            return onode;
        } catch (RepositoryException e) {
            Object[] args = {object.getName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
                "Exception while saving object {0}", e, args);
        }
    }

    private BaseObject loadXWikiCollection(XWikiJcrSession ses, Node nobj, BaseClass bclass, XWikiContext context)
        throws RepositoryException, XWikiException
    {
        BaseObject object = (BaseObject) ses.loadObject(nobj.getPath());
        BaseObject newobject = (BaseObject) bclass.newObject(context);
        newobject.setId(object.getId());
        newobject.setClassName(bclass.getName());
        newobject.setNumber(object.getNumber());
        object = newobject;
        PropertyIterator pi = nobj.getProperties();
        while (pi.hasNext()) {
            Property iprop = pi.nextProperty();
            String sprop = iprop.getName();
            if (!sprop.startsWith("xp:")) {
                continue;
            }
            int ind = sprop.indexOf(':');
            sprop = sprop.substring(ind + 1);
            PropertyClass pc = (PropertyClass) bclass.get(sprop);
            BaseProperty prop = pc.newProperty();
            prop.setName(sprop);
            loadXWikiProperty(ses, iprop, prop);
            prop.setObject(object);
            object.addField(prop.getName(), prop);
        }
        return object;
    }

    private void saveXWikiProperty(XWikiJcrSession ses, Node objnode, BaseProperty prop) throws ItemExistsException,
        PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException
    {
        if (prop.getValue() == null) {
            return;
        }
        final String propname = "xp:" + prop.getName();
        final ValueFactory vf = ses.getValueFactory();
        if (prop instanceof BaseStringProperty) {
            objnode.setProperty(propname, (String) prop.getValue());
        } else if (DateProperty.class.equals(prop)) {
            Date date = (Date) prop.getValue();
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            objnode.setProperty(propname, vf.createValue(cal));
        } else if (IntegerProperty.class.equals(prop.getClass()) || LongProperty.class.equals(prop.getClass())) {
            objnode.setProperty(propname, vf.createValue(((Number) prop.getValue()).longValue()));
        } else if (FloatProperty.class.equals(prop.getClass()) || DoubleProperty.class.equals(prop.getClass())) {
            objnode.setProperty(propname, vf.createValue(((Number) prop.getValue()).doubleValue()));
        } else if (StringListProperty.class.equals(prop.getClass())) {
            objnode.setProperty(propname, vf.createValue(((StringListProperty) prop).getTextValue()));
        } else if (DBStringListProperty.class.equals(prop.getClass())) {
            List list = ((DBStringListProperty) prop).getList();
            Value[] v = new Value[list.size()];
            for (int i = 0; i < list.size(); i++) {
                v[i] = vf.createValue((String) list.get(i));
            }
            objnode.setProperty(propname, v);
        }
        /*
         * was: Node propNode = ses.insertObject(objnode, "xp:"+prop.getName(), prop); propNode.setProperty("obj",
         * objnode);
         */
    }

    private void loadXWikiProperty(XWikiJcrSession ses, Property iprop, BaseProperty prop) throws RepositoryException
    {
        if (prop instanceof BaseStringProperty) {
            prop.setValue(iprop.getString());
        } else if (DateProperty.class.equals(prop)) {
            prop.setValue(iprop.getDate().getTime());
        } else if (IntegerProperty.class.equals(prop.getClass())) {
            prop.setValue(new Integer((int) iprop.getLong()));
        } else if (LongProperty.class.equals(prop.getClass())) {
            prop.setValue(new Long(iprop.getLong()));
        } else if (FloatProperty.class.equals(prop.getClass())) {
            prop.setValue(new Float(iprop.getDouble()));
        } else if (DoubleProperty.class.equals(prop.getClass())) {
            prop.setValue(new Double(iprop.getDouble()));
        } else if (StringListProperty.class.equals(prop.getClass())) {
            ((StringListProperty) prop).setTextValue(iprop.getString());
        } else if (DBStringListProperty.class.equals(prop.getClass())) {
            Value[] v = iprop.getValues();
            List list = new ArrayList(v.length);
            for (int i = 0; i < v.length; i++) {
                list.add(v[i].getString());
            }
            ((ListProperty) prop).setList(list);
        }
        /*
         * was: BaseProperty prop = (BaseProperty) ses.loadObject(nprop.getPath()); final String sname =
         * nprop.getName(); int ip = sname.indexOf(":"); if (ip<0) return null; prop.setName( decode(
         * sname.substring(ip+1) ) ); return prop;
         */
    }

    public XWikiDocument loadXWikiDoc(final XWikiDocument doc, final XWikiContext context) throws XWikiException
    {
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer("jcr");
            }
            XWikiDocument retdoc = (XWikiDocument) executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession ses) throws RepositoryException, XWikiException
                {
                    XWikiDocument retdoc = (XWikiDocument) ses.loadObject(getDocPath(doc));
                    if (retdoc != null) {
                        retdoc.setStore(context.getWiki().getStore());
                        retdoc.setDatabase(context.getDatabase());
                        retdoc.setNew(false);
                    } else {
                        doc.setNew(true);
                        return doc;
                    }
                    Node baseDocNode = ses.getNode(getBaseDocPath(retdoc));

                    if (retdoc.hasElement(XWikiDocument.HAS_ATTACHMENTS)) {
                        List attlist = new ArrayList();
                        NodeIterator ni = baseDocNode.getNode("attach").getNodes();
                        while (ni.hasNext()) {
                            Node natt = ni.nextNode();
                            XWikiAttachment att = (XWikiAttachment) ses.loadObject(natt.getPath());
                            att.setFilename(natt.getName());
                            att.setDoc(retdoc);
                            attlist.add(att);
                        }
                        retdoc.setAttachmentList(attlist);
                    } else {
                        retdoc.setAttachmentList(new ArrayList(0));
                    }

                    BaseClass bclass = new BaseClass();
                    String cxml = retdoc.getxWikiClassXML();
                    if (cxml != null) {
                        bclass.fromXML(cxml);
                        bclass.setName(retdoc.getFullName());
                        retdoc.setxWikiClass(bclass);
                    } else {
                        try {
                            Node nclass = baseDocNode.getNode("class");
                            bclass = loadXWikiClass(ses, nclass);
                        } catch (PathNotFoundException e) {
                            bclass = new BaseClass();
                        }
                        bclass.setName(retdoc.getFullName());
                        retdoc.setxWikiClass(bclass);
                    }
                    // Store this XWikiClass in the context so that we can use it in case of recursive usage of classes
                    context.addBaseClass(bclass);

                    if (retdoc.hasElement(XWikiDocument.HAS_OBJECTS)) {
                        Node nobjs = baseDocNode.getNode("obj");
                        NodeIterator nsi = nobjs.getNodes(); // spaces
                        while (nsi.hasNext()) {
                            Node nspace = nsi.nextNode();
                            // We use the internal class to store the statistics
                            if (ntXWikiObject.equals(nspace.getPrimaryNodeType().getName())) {
                                continue;
                            }
                            NodeIterator noi = nspace.getNodes(); // objects
                            while (noi.hasNext()) {
                                Node nobj = noi.nextNode();
                                BaseClass objclass =
                                    context.getWiki().getClass(nspace.getName() + "." + nobj.getName(), context);
                                BaseObject object = loadXWikiCollection(ses, nobj, objclass, context);
                                object.setName(retdoc.getFullName());
                                retdoc.setObject(object.getClassName(), object.getNumber(), object);
                            }
                        }
                    }
                    return retdoc;
                }
            });

            // We need to ensure that the loaded document becomes the original document
            retdoc.setOriginalDocument((XWikiDocument) retdoc.clone());

            return retdoc;
        } catch (Exception e) {
            Object[] args = {doc.getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
                "Exception while loading document {0}", e, args);
        } finally {
            if (monitor != null) {
                monitor.endTimer("jcr");
            }
        }
    }

    private BaseClass loadXWikiClass(XWikiJcrSession ses, Node nclass) throws RepositoryException
    {
        BaseClass bclass = (BaseClass) ses.loadObject(nclass.getPath());
        if (bclass == null) {
            return null;
        }
        NodeIterator ni = nclass.getNodes(); // PropertyClasses
        while (ni.hasNext()) {
            Node npc = ni.nextNode();
            PropertyClass pc = (PropertyClass) ses.loadObject(npc.getPath());
            bclass.addField(pc.getName(), pc);
        }
        return bclass;
    }

    public void deleteXWikiDoc(final XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        try {
            executeWrite(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws RepositoryException, XWikiException
                {
                    session.removeObject(getDocPath(doc));
                    return null;
                }
            });

            // We need to ensure that the deleted document becomes the original document
            doc.setOriginalDocument((XWikiDocument) doc.clone());

        } catch (Exception e) {
            Object[] args = {doc.getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
                "Exception while deleting document {0}", e, args);
        }
    }

    public void cleanUp(XWikiContext context)
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#createWiki(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public void createWiki(String wikiName, XWikiContext context) throws XWikiException
    {
        try {
            this.jcr.initWorkspace(wikiName);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
                "Cannot create new xwiki workspace: " + wikiName, e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#isWikiNameAvailable(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public boolean isWikiNameAvailable(String wikiName, XWikiContext context) throws XWikiException
    {
        boolean available;

        try {
            this.jcr.getSession(wikiName);
            available = false;
        } catch (RepositoryException e) {
            available = true;
        }

        return available;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#deleteWiki(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public void deleteWiki(String wikiName, XWikiContext context) throws XWikiException
    {
        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
            "XWikiJcrStore.deleteWiki not implemented");
    }

    public boolean exists(final XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        try {
            return ((Boolean) executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws RepositoryException, XWikiException
                {
                    return new Boolean(session.isNodeExist(getDocPath(doc)));
                }
            })).booleanValue();
        } catch (Exception e) {
            Object[] args = {doc.getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
                "Exception while check exists document {0}", e, args);
        }
    }

    // Locks
    public XWikiLock loadLock(final long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        try {
            return (XWikiLock) executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    Node docNode = getDocNodeById(session, docId);
                    if (docNode == null) {
                        return null;
                    }
                    XWikiLock lock = (XWikiLock) session.loadObject(docNode.getPath() + "/lock");
                    if (lock == null) {
                        return null;
                    }
                    lock.setDocId(docId);
                    return lock;
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_LOCK, "Exception while loading lock", e);
        }
    }

    public void saveLock(final XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        try {
            final String docPath = getDocPathById(context, lock.getDocId());
            if (docPath == null) {
                return;
            }
            executeWrite(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    Node docNode = session.getNode(docPath);
                    Node lockNode = JcrUtil.getOrCreateSubNode(docNode, "lock", ntXWikiLock);
                    session.updateObject(lockNode, lock);
                    return null;
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_JCR_SAVING_LOCK, "Exception while locking document", e);
        }
    }

    public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        try {
            final String docPath = getDocPathById(context, lock.getDocId());
            executeWrite(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    session.removeObject(docPath + "/lock");
                    return null;
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_JCR_DELETING_LOCK, "Exception while deleting lock", e);
        }
    }

    public List loadLinks(final long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        final List links = new ArrayList();
        try {
            executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession ses) throws Exception
                {
                    Node docNode = getDocNodeById(ses, docId);
                    if (docNode == null) {
                        return null;
                    }
                    try {
                        Node linksNode = docNode.getNode("links");
                        NodeIterator ni = linksNode.getNodes();
                        while (ni.hasNext()) {
                            Node linkNode = ni.nextNode();
                            XWikiLink link = (XWikiLink) ses.loadObject(linkNode.getPath());
                            link.setLink(decode(linkNode.getName()));
                            links.add(link);
                        }
                    } catch (PathNotFoundException e) {
                    }
                    ;
                    return null;
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_LINKS, "Exception while loading links", e);
        }
        return links;
    }

    /**
     * @since 2.2M2
     */
    public List<DocumentReference> loadBacklinks(final DocumentReference documentReference, boolean bTransaction,
        XWikiContext context) throws XWikiException
    {
        final List<DocumentReference> backlinkReferences = new ArrayList<DocumentReference>();
        try {
            executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession ses) throws Exception
                {
                    Query q = ses.getWorkspace().getQueryManager().createQuery("//element("
                        + localEntityReferenceSerializer.serialize(documentReference) + ",xwiki:link)/@fullName",
                        Query.XPATH);
                    RowIterator ri = q.execute().getRows();
                    while (ri.hasNext()) {
                        Row row = ri.nextRow();
                        String s = row.getValues()[0].getString();
                        backlinkReferences.add(currentMixedDocumentReferenceResolver.resolve(s));
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_BACKLINKS, "Exception while loading backlinks", e);
        }
        return backlinkReferences;
    }

    /**
     * @deprecated since 2.2M2 use {@link #loadBacklinks(DocumentReference, boolean, XWikiContext)}
     */
    @Deprecated
    public List loadBacklinks(final String fullName, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        List<String> backlinkNames = new ArrayList<String>();
        List<DocumentReference> backlinkReferences = loadBacklinks(
            this.currentMixedDocumentReferenceResolver.resolve(fullName), bTransaction, context);
        for (DocumentReference backlinkReference : backlinkReferences) {
            backlinkNames.add(this.localEntityReferenceSerializer.serialize(backlinkReference));
        }
        return backlinkNames;
    }

    public void saveLinks(final XWikiDocument doc, final XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        try {
            // need to delete existing links before saving the page's one
            deleteLinks(doc.getId(), context, bTransaction);
            // necessary to blank links from doc
            context.remove("links");
            // call to RenderEngine and converting the list of links into a list of backlinks
            XWikiRenderer renderer = context.getWiki().getRenderingEngine().getRenderer("wiki");
            renderer.render(doc.getContent(), doc, doc, context);
            final List links = (List) context.get("links");
            if (links == null || links.size() == 0) {
                return;
            }
            executeWrite(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession ses) throws Exception
                {
                    Node docNode = ses.getNode(getDocPath(doc));
                    Node linksNode = JcrUtil.getOrCreateSubNode(docNode, "links", ntXWikiLinks);
                    clearNodeChildrens(linksNode);
                    for (int i = 0; i < links.size(); i++) {
                        XWikiLink link = new XWikiLink();
                        link.setDocId(doc.getId());
                        link.setLink((String) links.get(i));
                        link.setFullName(doc.getFullName());
                        String nodeName = link.getLink();
                        nodeName = encode(nodeName);
                        ses.insertObject(linksNode, nodeName, link);
                    }
                    ses.save();
                    return null;
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_JCR_SAVING_LINKS, "Exception while saving links", e);
        }
    }

    public void deleteLinks(final long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        try {
            final String docPath = getDocPathById(context, docId);
            if (docPath == null) {
                return;
            }
            executeWrite(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession ses) throws Exception
                {
                    try {
                        Node linksNode = (Node) ses.getItem(docPath + "/links");
                        if (linksNode != null) {
                            linksNode.remove();
                        }
                        ses.save();
                    } catch (PathNotFoundException e) {
                    }
                    ;
                    return null;
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_JCR_DELETING_LINKS, "Exception while deleting links", e);
        }
    }

    public List getClassList(XWikiContext context) throws XWikiException
    {
        try {
            return (List) executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    Filter filter =
                        session.getObjectQueryManager().createFilter(XWikiDocument.class).addNotNull("xWikiClassXML");
                    return searchStringAttribute(session, filter, "fullName");
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_SEARCH,
                "Exception while searching class list", e);
        }
    }

    public List getTranslationList(final XWikiDocument doc, final XWikiContext context) throws XWikiException
    {
        try {
            return (List) executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    Filter filter = session.getObjectQueryManager().createFilter(XWikiDocument.class);
                    filter.addEqualTo("name", doc.getName());
                    filter.addEqualTo("space", doc.getSpace());
                    filter.addNotEqualTo("language", "");
                    return searchStringAttribute(session, filter, "language");
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
                "Exception while getTranslationList", e);
        }
    }

    private List searchStringAttribute(XWikiJcrSession session, Filter filter, String returnatt)
        throws InvalidQueryException, RepositoryException
    {
        String sq =
            session.getObjectQueryManager().buildJCRExpression(session.getObjectQueryManager().createQuery(filter));
        sq += "/@" + returnatt;
        return searchStringAttribute(session, sq);
    }

    private List searchStringAttribute(XWikiJcrSession session, String xpath) throws InvalidQueryException,
        RepositoryException
    {
        List result = new ArrayList();
        RowIterator ri = session.getQueryManager().createQuery(xpath, Query.XPATH).execute().getRows();
        while (ri.hasNext()) {
            result.add(ri.nextRow().getValues()[0].getString());
        }
        return result;
    }

    private List searchNodeNames(XWikiJcrSession session, String xpath) throws InvalidQueryException,
        RepositoryException
    {
        List result = new ArrayList();
        NodeIterator ri = session.getQueryManager().createQuery(xpath, Query.XPATH).execute().getNodes();
        while (ri.hasNext()) {
            result.add(decode(ri.nextNode().getName()));
        }
        return result;
    }

    public List getAllDocuments(final XWikiContext context) throws XWikiException
    {
        try {
            return (List) executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    Filter filter = session.getObjectQueryManager().createFilter(XWikiDocument.class);
                    return searchStringAttribute(session, filter, "fullName");
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
                "Exception while getAllDocuments", e);
        }
    }

    public List getSpaces(XWikiContext context) throws XWikiException
    {
        try {
            return (List) executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    return searchNodeNames(session, "store/*");
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
                "Exception while getSpaces", e);
        }
    }

    public List getSpaceDocsName(final String spaceName, XWikiContext context) throws XWikiException
    {
        try {
            return (List) executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    Filter filter =
                        session.getObjectQueryManager().createFilter(XWikiDocument.class)
                            .addEqualTo("space", spaceName);
                    return searchStringAttribute(session, filter, "name");
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
                "Exception while getSpaceDocsName", e);
        }
    }

    public List listGroupsForUser(final String username, XWikiContext context) throws XWikiException
    {
        final String shortname = Util.getName(username);
        final List result = new ArrayList();
        try {
            executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    // XXX: if username contains "'" ?
                    String xpath =
                        "store/*/*/obj/XWiki/XWikiGroups[@xp:member='" + username + "' or @xp:member='" + shortname
                            + "']/jcr:deref(@doc, '*')/@fullName";
                    Query query = session.getQueryManager().createQuery(xpath, Query.XPATH);
                    RowIterator ni = query.execute().getRows();
                    while (ni.hasNext()) {
                        Row row = ni.nextRow();
                        result.add(row.getValues()[0].getString());
                    }
                    return null;
                }
            });
            return result;
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
                "Exception while listGroupsForUser", e);
        }
    }

    //
    public List getAllObjectsByClass(final Class cl, XWikiContext context) throws XWikiException
    {
        QueryManager qm = getObjectQueryManager(context);
        return getObjects(qm.createQuery(qm.createFilter(cl)), context);
    }

    public QueryManager getObjectQueryManager(XWikiContext context) throws XWikiException
    {
        // TODO: store query manager in class.
        try {
            return (QueryManager) executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    return session.getObjectQueryManager();
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
                "Exception while getObjectQueryManager", e);
        }
    }

    public List getObjects(final org.apache.portals.graffito.jcr.query.Query query, XWikiContext context)
        throws XWikiException
    {
        final List result = new ArrayList();
        try {
            executeRead(context, new JcrCallBack()
            {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    String xpath = session.getObjectQueryManager().buildJCRExpression(query);
                    NodeIterator ni = session.getQueryManager().createQuery(xpath, Query.XPATH).execute().getNodes();
                    while (ni.hasNext()) {
                        Node node = ni.nextNode();
                        Object object = session.loadObject(node.getPath());
                        result.add(object);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
                "Exception while getObjects", e);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public org.xwiki.query.QueryManager getQueryManager()
    {
        return this.queryManager;
    }

    public void notSupportedCall()
    {
        new NotSupportedException().printStackTrace();
    }

    // Not supported:
    public List getCustomMappingPropertyList(BaseClass bclass)
    {
        notSupportedCall();
        return null;
    }

    public boolean injectCustomMapping(BaseClass doc1class, XWikiContext xWikiContext) throws XWikiException
    {
        notSupportedCall();
        return false;
    }

    public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return false;
    }

    public void injectCustomMappings(XWikiContext context) throws XWikiException
    {
        notSupportedCall();
    }

    public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException
    {
        notSupportedCall();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, XWikiContext context)
        throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, boolean,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping,
        XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, int, int,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, int nb, int start,
        XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, boolean, int, int,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, int nb,
        int start, XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, int, int,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, int, int, java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start,
        List parameterValues, XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, List parameterValues, XWikiContext context)
        throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, boolean, int, int,
     *      java.util.List, com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        int nb, int start, List parameterValues, XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, int, int, java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, List parameterValues,
        XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, boolean, boolean, int,
     *      int, java.util.List, com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        boolean checkRight, int nb, int start, List parameterValues, XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStoreInterface#countDocuments(String, XWikiContext)
     */
    public int countDocuments(String wheresql, XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStoreInterface#countDocuments(String, List, XWikiContext)
     */
    public int countDocuments(String parametrizedSqlClause, List parameterValues, XWikiContext context)
        throws XWikiException
    {
        notSupportedCall();
        return 0;
    }

    /**
     * @since 2.2M2
     */
    public List<DocumentReference> searchDocumentReferences(String wheresql, XWikiContext context)
        throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * @deprecated since 2.2M2 use {@link #searchDocumentReferences(String, XWikiContext)}
     */
    @Deprecated
    public List<String> searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * @since 2.2M2
     */
    public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * @deprecated since 2.2M2 use {@link #searchDocumentReferences(String, int, int, XWikiContext)}
     */
    @Deprecated
    public List<String> searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * @since 2.2M2
     */
    public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start, String selectColumns,
        XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * @deprecated since 2.2M2 use {@link #searchDocumentReferences(String, int, int, String, XWikiContext)}
     */
    @Deprecated
    public List<String> searchDocumentsNames(String wheresql, int nb, int start, String selectColumns,
        XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * @since 2.2M2
     */
    public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, int nb, int start,
        List parameterValues, XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * @deprecated since 2.2M2 use {@link #searchDocumentReferences(String, int, int, List, XWikiContext)}
     */
    @Deprecated
    public List<String> searchDocumentsNames(String parametrizedSqlClause, int nb, int start, List parameterValues,
        XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * @since 2.2M2
     */
    public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, List parameterValues,
        XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * @deprecated since 2.2M2 use {@link #searchDocumentReferences(String, List, XWikiContext)}
     */
    @Deprecated
    public List<String> searchDocumentsNames(String parametrizedSqlClause, List parameterValues, XWikiContext context)
        throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context)
        throws XWikiException
    {
        notSupportedCall();
        return false;
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping,
        boolean checkRight, int nb, int start, XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, com.xpn.xwiki.XWikiContext)
     */
    public List search(String sql, int nb, int start, XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, java.lang.Object[][],
     *      com.xpn.xwiki.XWikiContext)
     */
    public List search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context)
        throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List search(String sql, int nb, int start, List parameterValues, XWikiContext context) throws XWikiException
    {
        notSupportedCall();
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, java.lang.Object[][],
     *      java.util.List, com.xpn.xwiki.XWikiContext)
     */
    public List search(String sql, int nb, int start, Object[][] whereParams, List parameterValues, XWikiContext context)
        throws XWikiException
    {
        notSupportedCall();
        return null;
    }
}
