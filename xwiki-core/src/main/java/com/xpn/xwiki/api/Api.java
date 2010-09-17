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
package com.xpn.xwiki.api;

import java.util.ArrayList;
import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Base class for all API Objects. API Objects are the Java Objects that can be manipulated from Velocity or Groovy in
 * XWiki documents.
 * 
 * @version $Id$
 */
public class Api
{
    /**
     * The current context, needed by the underlying protected object.
     * 
     * @see #getXWikiContext()
     * @todo make this variable private after we agree on it on the mailing list. It'll break non-core plugins but
     *       better do it now rather than after the 1.0 release...
     */
    protected XWikiContext context;

    /**
     * @param context the XWiki Context object
     * @see #getXWikiContext()
     */
    public Api(XWikiContext context)
    {
        this.context = context;
    }

    /**
     * Get the current context. For the moment, this is a crucial part of the request lifecycle, as it is the only
     * access point to all the components needed for handling a request. Note: This method is protected so that users of
     * this API do not get to see the XWikiContext object which should not be exposed.
     * 
     * @return The XWiki Context object containing all information about the current XWiki instance, including
     *         information on the current request and response.
     */
    protected XWikiContext getXWikiContext()
    {
        return this.context;
    }

    /**
     * Check if the current document has programming rights, meaning that it was last saved by a user with the
     * programming right globally granted.
     * 
     * @return <tt>true</tt> if the current document has the Programming right or <tt>false</tt> otherwise.
     */
    public boolean hasProgrammingRights()
    {
        // There is never programming right after privileges have been dropped.
        if (hasDroppedPermissions()) {
            return false;
        }

        com.xpn.xwiki.XWiki xwiki = this.context.getWiki();
        return xwiki.getRightService().hasProgrammingRights(this.context);
    }

    /**
     * Check if the current user has administration rights on the current wiki.
     * 
     * @return <code>true</code> if the current user has the <code>admin</code> right or <code>false</code> otherwise.
     */
    public boolean hasAdminRights()
    {
        com.xpn.xwiki.XWiki xwiki = this.context.getWiki();
        return xwiki.getRightService().hasAdminRights(this.context);
    }

    /**
     * Check if the current user has an access level on a given document.
     * 
     * @param right The name of the right to verify (eg "programming", "admin", "register", etc).
     * @param docname The document for which to verify the right.
     * @return <tt>true</tt> if the current user has the specified right, <tt>false</tt> otherwise.
     * @exception XWikiException In case of an error finding the document or accessing groups information.
     */
    public boolean hasAccessLevel(String right, String docname) throws XWikiException
    {
        com.xpn.xwiki.XWiki xwiki = this.context.getWiki();
        return xwiki.getRightService().hasAccessLevel(right, this.context.getUser(), docname, this.context);
    }

    /**
     * Convert a list of internal representation of documents to public api documents.
     * 
     * @param xdocList the internal documents.
     * @return the plublic api documents.
     */
    protected List<Document> convert(List<XWikiDocument> xdocList)
    {
        List<Document> docList = new ArrayList<Document>();
        for (XWikiDocument xdoc : xdocList) {
            docList.add(xdoc.newDocument(this.context));
        }

        return docList;
    }

    /**
     * Convert an internal representation of document to public api document.
     * 
     * @param xdoc the internal document.
     * @return the plublic api document.
     */
    protected Document convert(XWikiDocument xdoc)
    {
        return xdoc == null ? null : xdoc.newDocument(this.context);
    }

    /**
     * Get the name of the content author of the current document for security checking.
     * If {@link Context#dropPermissions()} has been called then this will return the guest user no matter
     * who the real author is.
     * If there is no current document then the guest user is returned because there is no reason for script to
     * have any permission if does not exist in any document.
     *
     * @return the name of the document author or guest.
     */
    String getEffectiveScriptAuthorName()
    {
        if (!hasDroppedPermissions()) {
            final XWikiDocument doc = this.getXWikiContext().getDoc();
            if (doc != null) {
                return doc.getAuthor();
            }
        }
        return XWikiRightService.GUEST_USER;
    }

    /**
     * @return true if the code has dropped it's permissions by calling {@link Context#dropPermissions()}
     */
    private boolean hasDroppedPermissions()
    {
        return "true".equals(this.getXWikiContext().get("hasDroppedPermissions"));
    }
}
