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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;

/**
 * SuperDocument interface.
 * 
 * @version $Id$
 * @see XClassManager
 * @since Application Manager 1.0RC1
 */
public interface XObjectDocument
{
    /**
     * The separator between space name and document name in document full name.
     */
    String SPACE_DOC_SEPARATOR = ".";

    /**
     * The separator between wiki name and space name and document extended full name.
     */
    String WIKI_SPACE_SEPARATOR = ":";

    /**
     * Reload XWiki document from database using Document full name.
     * 
     * @param context the XWiki context.
     * @throws XWikiException error when initialize document.
     */
    void reload(XWikiContext context) throws XWikiException;

    /**
     * @return the class manager for this document.
     */
    XClassManager< ? extends XObjectDocument> getXClassManager();

    /**
     * @return true if this is a new document of this class (this document can exist but does not contains object of
     *         this class).
     */
    boolean isNew();

    /**
     * @return the document.
     */
    Document getDocumentApi();

    /**
     * @return the managed XWiki object.
     */
    Object getObjectApi();

    /**
     * @return the id of the managed XWiki object;
     */
    int getObjectId();
}
