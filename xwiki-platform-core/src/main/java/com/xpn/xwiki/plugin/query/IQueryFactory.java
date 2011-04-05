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

package com.xpn.xwiki.plugin.query;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiStoreInterface;

import javax.jcr.ValueFactory;
import javax.jcr.query.InvalidQueryException;

/** AbstractFactory interface for XWiki Queries */
public interface IQueryFactory {
    /** create xpath query
     * @throws XWikiException */
    IQuery xpath(String q) throws XWikiException;

    /** create JCRSQL query
     * unsupported for now */
    IQuery ql(String q) throws XWikiException;

    /** create query for docs
     * @param docname - full document name (web/name | web.name). name may consist xpath []-selection. if any (name|web) - *
     * @param prop - return properties, separated by comma, property start with @, if null - return document
     * @param order - properties for sort, separated by ','; order: ascending/descending after prop. name, or +/- before. if null - not sort
     * @throws InvalidQueryException
     * */
    IQuery getDocs(String docname, String prop, String order) throws XWikiException;

    /** create query for child documents
     * @throws InvalidQueryException
     * @param web,docname must be without templates & [] select
     * @see getDocs */
    IQuery getChildDocs(String docname, String prop, String order) throws XWikiException;

    /** create query for attachments
     * @param attachname - name of attachment, may be *, *[]
     * @see getDocs
     * @throws InvalidQueryException
     */
    IQuery getAttachment(String docname, String attachname, String order) throws XWikiException;

    /** create query for objects
     * @param oclass - full name of object class (web/name | web.name).  if any(name|web) - *
     * @param prop. for flex-attributes use @xp:propname
     * @see getDocs
     * @throws InvalidQueryException
     */
    IQuery getObjects(String docname, String oclass, String prop, String order) throws XWikiException;

    /** Returns ValueFactory for creating correct jcr values
     * @return javax.jcr.ValueFactory */
    ValueFactory getValueFactory();

    XWikiContext getContext();
    XWikiStoreInterface getStore();

    /**
     * Generates an XPath query from a XWikiQuery Object
     * @param query
     * @return query xpath string
     */
    String makeQuery(XWikiQuery query) throws XWikiException;
}
