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
 */

package com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * {@inheritDoc}
 * 
 * @see AbstractXClassManager
 * @version $Id: $
 * @deprecated Use {@link AbstractXClassManager} since 1.0 RC1
 */
@Deprecated
public abstract class AbstractSuperClass extends AbstractXClassManager<SuperDocument> implements SuperClass
{
    /**
     * Constructor for AbstractSuperClass.
     * 
     * @param prefix the prefix of class document.
     * @see #AbstractSuperClass(String, String)
     * @see #AbstractSuperClass(String, String, boolean)
     */
    protected AbstractSuperClass(String prefix)
    {
        super(prefix);
    }

    /**
     * Constructor for AbstractSuperClass.
     * 
     * @param spaceprefix the space prefix of class document.
     * @param prefix the prefix of class document.
     * @see #AbstractSuperClass(String)
     * @see #AbstractSuperClass(String, String, boolean)
     */
    protected AbstractSuperClass(String spaceprefix, String prefix)
    {
        super(spaceprefix, prefix);
    }

    /**
     * Constructor for AbstractSuperClass.
     * 
     * @param spaceprefix the space of class document.
     * @param prefix the prefix of class document.
     * @param dispatch Indicate if it had to use standard XWiki applications space names.
     * @see #AbstractSuperClass(String)
     * @see #AbstractSuperClass(String, String)
     */
    protected AbstractSuperClass(String spaceprefix, String prefix, boolean dispatch)
    {
        super(spaceprefix, prefix, dispatch);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass#getSuperDocument(java.lang.String,
     *      int, boolean, com.xpn.xwiki.XWikiContext)
     */
    public SuperDocument getSuperDocument(String itemName, int objectId, boolean validate, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument doc = context.getWiki().getDocument(getItemDocumentDefaultFullName(itemName, context), context);

        if (doc.isNew() || !isInstance(doc)) {
            throw new SuperDocumentDoesNotExistException(itemName + " object does not exist");
        }

        return newSuperDocument(doc, objectId, context);
    }

    /**
     * Find all XWikiDocument containing object of this XWiki class.
     * 
     * @param context the XWiki context.
     * @return the list of found {@link SuperDocument}.
     * @throws XWikiException error when searching for document in database.
     * @see #getClassFullName()
     */
    public List searchSuperDocuments(XWikiContext context) throws XWikiException
    {
        return searchSuperDocumentsByFields(null, context);
    }

    /**
     * Search in instances of this document class.
     * 
     * @param fieldName the name of field.
     * @param fieldValue the value of field.
     * @param fieldType the type of field.
     * @param context the XWiki context.
     * @return the list of found {@link SuperDocument}.
     * @throws XWikiException error when searching for documents from in database.
     */
    public List searchSuperDocumentsByField(String fieldName, String fieldValue, String fieldType, XWikiContext context)
        throws XWikiException
    {
        String[][] fieldDescriptors = new String[][] {{fieldName, fieldType, fieldValue}};

        return searchSuperDocumentsByFields(fieldDescriptors, context);
    }

    /**
     * Search in instances of this document class.
     * 
     * @param fieldDescriptors the list of fields name/value constraints. Format : [[fieldName1, typeField1,
     *            valueField1][fieldName2, typeField2, valueField2]].
     * @param context the XWiki context.
     * @return the list of found {@link SuperDocument}.
     * @throws XWikiException error when searching for documents from in database.
     */
    public List searchSuperDocumentsByFields(String[][] fieldDescriptors, XWikiContext context) throws XWikiException
    {
        check(context);

        List parameterValues = new ArrayList();
        String where = createWhereClause(fieldDescriptors, parameterValues);

        return newSuperDocumentList(context.getWiki().getStore().searchDocuments(where, parameterValues, context),
            context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass#newSuperDocument(com.xpn.xwiki.doc.XWikiDocument,
     *      int, com.xpn.xwiki.XWikiContext)
     */
    public SuperDocument newSuperDocument(XWikiDocument doc, int objId, XWikiContext context) throws XWikiException
    {
        return new DefaultSuperDocument(this, doc, objId, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass#newSuperDocument(java.lang.String,
     *      int, com.xpn.xwiki.XWikiContext)
     */
    public SuperDocument newSuperDocument(String docFullName, int objId, XWikiContext context) throws XWikiException
    {
        return newSuperDocument(context.getWiki().getDocument(docFullName, context), objId, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#newSuperDocument(com.xpn.xwiki.XWikiContext)
     */
    public SuperDocument newSuperDocument(XWikiContext context) throws XWikiException
    {
        return newSuperDocument(new XWikiDocument(), 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass#newSuperDocumentList(com.xpn.xwiki.doc.XWikiDocument,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List newSuperDocumentList(XWikiDocument document, XWikiContext context) throws XWikiException
    {
        List documents = new ArrayList(1);
        documents.add(document);

        return newSuperDocumentList(documents, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass#newSuperDocumentList(java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List newSuperDocumentList(List documents, XWikiContext context) throws XWikiException
    {
        List list = new ArrayList(documents.size());
        for (Iterator it = documents.iterator(); it.hasNext();) {
            XWikiDocument doc = (XWikiDocument) it.next();
            List objects = doc.getObjects(getClassFullName());

            for (Iterator itObject = objects.iterator(); itObject.hasNext();) {
                BaseObject bobject = (BaseObject) itObject.next();
                if (bobject != null) {
                    list.add(newSuperDocument(doc, bobject.getNumber(), context));
                }
            }
        }

        return list;
    }
}
