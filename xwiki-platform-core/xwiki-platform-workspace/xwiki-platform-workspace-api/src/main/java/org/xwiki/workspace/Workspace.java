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
package org.xwiki.workspace;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.plugin.wikimanager.doc.Wiki;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;

/**
 * Provides some methods to access workspace parameters.
 * 
 * @version $Id$
 */
public interface Workspace
{
    /** Default workspace group space. */
    String WORKSPACE_GROUP_SPACE = "XWiki";

    /** Default workspace group page. */
    String WORKSPACE_GROUP_PAGE = "XWikiAllGroup";

    /** @return the wiki document descriptor */
    Wiki getWikiDocument();

    /** @return the XWikiServerClass object acting as wiki descriptor contained in the wiki document */
    XWikiServer getWikiDescriptor();

    /** @return the group document that defines user membership to the workspace */
    Document getGroupDocument();
}
