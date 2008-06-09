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
package com.xpn.xwiki.api;

import java.util.Date;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

/**
 * API object for get info about some version of Document.
 * 
 * @version $Id$
 */
public class RevisionInfo extends Api
{
    /** used for get all information. */
    private XWikiRCSNodeInfo nodeInfo;

    /**
     * @param nodeInfo - from that nodeinfo all information is getting
     * @param context - needed for {@link Api}
     */
    public RevisionInfo(XWikiRCSNodeInfo nodeInfo, XWikiContext context)
    {
        super(context);
        this.nodeInfo = nodeInfo;
    }

    /** @return version of this revision */
    public String getVersion()
    {
        return nodeInfo.getId().getVersion().toString();
    }

    /** @return date of this revision */
    public Date getDate()
    {
        return nodeInfo.getDate();
    }

    /** @return author of this revision */
    public String getAuthor()
    {
        return nodeInfo.getAuthor();
    }

    /** @return revision comment */
    public String getComment()
    {
        return nodeInfo.getComment();
    }

    /** @return is revision is minor */
    public boolean isMinorEdit()
    {
        return nodeInfo.isMinorEdit();
    }
}
