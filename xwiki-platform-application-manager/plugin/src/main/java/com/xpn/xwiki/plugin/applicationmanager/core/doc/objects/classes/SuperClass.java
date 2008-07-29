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

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Interface that give way to manage a XWiki class.
 * <ul>
 * <li>assume that the XWiki class exist in the context we are working
 * <li>search in documents that contains this class with conditions on class fields
 * <li>support the XWiki norm about spaces and documents naming
 * </ul>
 * 
 * @version $Id: $
 * @todo See http://jira.xwiki.org/jira/browse/XWIKI-1576. When that issue is applied in XWiki Core and when this plugin
 *       moves to the version of XWiki Core where it was applied then remove this class.
 * @deprecated Use {@link XClassManager} since 1.0 RC1
 */
@Deprecated
public interface SuperClass extends XClassManager<SuperDocument>
{
    /**
     * Get document by full name from item name <code>itemName</code>.
     * 
     * @param itemName the full name of the item.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param validate indicate if it return new {@link SuperDocument} or throw exception if wiki descriptor does not
     *            exist.
     * @param context the XWiki context.
     * @return the document.
     * @throws XWikiException error when getting document from the database.
     * @see #getItemDefaultName(String)
     * @see #getItemDocumentDefaultFullName(String, XWikiContext)
     * @future XA2 : rename to getDocumentObject.
     */
    SuperDocument getSuperDocument(String itemName, int objectId, boolean validate, XWikiContext context)
        throws XWikiException;

    /**
     * Create new super document containing object of class {@link #getClassFullName()}. If document already exist it is
     * returned with new object if it does not contains any.
     * 
     * @param doc the XWiki document to manage.
     * @param objId the id of the XWiki object included in the document to manage.
     * @param context the XWiki context.
     * @return a new SuperDocument instance.
     * @throws XWikiException error when calling SuperDocument implementation constructor.
     * @future XA2 : rename to newDocumentObject.
     */
    SuperDocument newSuperDocument(XWikiDocument doc, int objId, XWikiContext context) throws XWikiException;

    /**
     * Create new super document containing object of class {@link #getClassFullName()}. If document already exist it is
     * returned with new object if it does not contains any.
     * 
     * @param context the XWiki context.
     * @return a new SuperDocument instance.
     * @throws XWikiException error when calling SuperDocument implementation constructor.
     * @future XA2 : rename to newDocumentObject.
     */
    SuperDocument newSuperDocument(XWikiContext context) throws XWikiException;

    /**
     * Create new super document containing object of class {@link #getClassFullName()}. If document already exist it is
     * returned with new object if it does not contains any.
     * 
     * @param docFullName the full name of document to manage.
     * @param objId the id of the XWiki object included in the document to manage.
     * @param context the XWiki context.
     * @return a new SuperDocument instance.
     * @throws XWikiException error when calling SuperDocument implementation constructor.
     * @see #getClassFullName()
     * @future XA2 : rename to newDocumentObject.
     */
    SuperDocument newSuperDocument(String docFullName, int objId, XWikiContext context) throws XWikiException;

    /**
     * Create new super documents for each object of class {@link #getClassFullName()} for provided
     * {@link XWikiDocument} and return it.
     * 
     * @param documents the list of {@link XWikiDocument}.
     * @param context the XWiki context.
     * @return the list of {@link SuperDocument}.
     * @throws XWikiException error when calling SuperDocument implementation constructor.
     * @future XA2 : rename to newDocumentObjectList.
     */
    List newSuperDocumentList(XWikiDocument documents, XWikiContext context) throws XWikiException;

    /**
     * Create new super document for each object of class {@link #getClassFullName()} of each {@link XWikiDocument} in
     * the list and return it.
     * 
     * @param documents the list of {@link XWikiDocument}.
     * @param context the XWiki context.
     * @return the list of {@link SuperDocument}.
     * @throws XWikiException error when calling SuperDocument implementation constructor.
     * @future XA2 : rename to newDocumentObjectList.
     */
    List newSuperDocumentList(List documents, XWikiContext context) throws XWikiException;
}
