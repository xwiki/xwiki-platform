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

import java.util.Calendar;
import java.util.Date;

import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
/**
 * Information about deleted document in recycle bin. 
 * @version $Id: $
 */
public class DeletedDocument extends Api
{
    /**
     * original document.
     */
    private final XWikiDeletedDocument deldoc;

    /**
     * @param deldoc - original deleted document
     * @param context - used for Api
     */
    public DeletedDocument(XWikiDeletedDocument deldoc, XWikiContext context)
    {
        super(context);
        this.deldoc = deldoc;
    }

    /**
     * @return full name of document (ie: Main.WebHome)
     */
    public String getFullName()
    {
        return deldoc.getFullName();
    }
    
    /**
     * @return language of document
     */
    public String getLanguage()
    {
        return deldoc.getLanguage();
    }
    
    /**
     * @return date of delete action
     */
    public Date getDate()
    {
        return deldoc.getDate();
    }

    /**
     * @return user which delete document
     */
    public String getDeleter()
    {
        return deldoc.getDeleter();
    }
    
    /**
     * @return id of deleted document. id is unique only for this document.
     */
    public long getId()
    {
        return deldoc.getId();
    }

    /**
     * @return can current user restore this document from recycle bin
     * @throws XWikiException if any error
     */
    public boolean canUndelete() throws XWikiException
    {
        return hasAdminRights() || hasAccessLevel("undelete", getFullName());
    }

    /**
     * @return can current user permanently delete this document
     * @throws XWikiException if any error
     * @xwikicfg xwiki.store.recyclebin.adminWaitDays
     *      how many days should wait admin to delete document. 0 by default
     * @xwikicfg xwiki.store.recyclebin.waitDays
     *      how many days should wait user with "delete" right to delete document
     */
    public boolean canDelete() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(getFullName(), context);
        if (!hasAdminRights()
            && !getXWikiContext().getWiki().getRightService()
                .checkAccess("delete", doc, context)) {
            return false;
        }
        String waitdays;
        XWikiConfig config = getXWikiContext().getWiki().getConfig();
        if (hasAdminRights()) {
            waitdays = config.getProperty("xwiki.store.recyclebin.adminWaitDays", "0");
        } else {
            waitdays = config.getProperty("xwiki.store.recyclebin.waitDays", "7");
        }
        int seconds = (int) (Double.parseDouble(waitdays) * 24 * 60 * 60 + 0.5);
        Calendar cal = Calendar.getInstance();
        cal.setTime(getDate());
        cal.add(Calendar.SECOND, seconds);
        return cal.before(Calendar.getInstance());
    }

    /**
     * @return original deleted document if user has programming rights, else null.
     */
    public XWikiDeletedDocument getDeletedDocument()
    {
        if (hasProgrammingRights()) {
            return deldoc;
        } else {
            return null;
        }
    }
    
    /**
     * @return document, restored from recycle bin 
     * @throws XWikiException if error 
     */
    public Document getDocument() throws XWikiException
    {
        return new Document(deldoc.restoreDocument(null, context), context);
    }
}
