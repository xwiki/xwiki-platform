/*
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
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
 * Created on 02.02.2005
 */
package net.jkraemer.xwiki.plugins.emailnotify;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Information about a modification made to a wiki page.
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class PageModification
{
    private final PageData pageData;
    private final Date     modificationDate;
    private final String   modifier;
    private boolean        dontSendNow = false;

    /**
     * Copy-Constructor. copies everything but the don't send flag.
     * @param original
     */
    public PageModification (PageModification original)
    {
        this.pageData = original.pageData;
        this.modificationDate = original.modificationDate;
        this.modifier = original.modifier;
    }

    /**
     * 
     */
    public PageModification (XWikiDocument document, XWikiDocument oldDocument, PageData pd)
    {
        this.pageData = pd;
        modificationDate = document.getDate ();
        modifier = document.getAuthor ();
    }

    public Date getModificationDate ()
    {
        return modificationDate;
    }

    public String getModifier ()
    {
        return modifier;
    }

    public boolean isDontSendNow ()
    {
        return dontSendNow;
    }

    public void setDontSendNow (boolean dontSendNow)
    {
        this.dontSendNow = dontSendNow;
    }

    public PageData getPageData ()
    {
        return pageData;
    }

    public String toString ()
    {
        return new ToStringBuilder (this).append ("page", pageData.getFullPageName ()).append ("modifier",
                                                                                               modifier)
                .append ("mod-date", modificationDate).toString ();
    }

}
