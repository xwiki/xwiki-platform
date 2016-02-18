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
package com.xpn.xwiki.web;

import java.net.URL;

import org.xwiki.component.annotation.Role;
import org.xwiki.url.filesystem.FilesystemExportContext;

import com.xpn.xwiki.XWikiContext;

/**
 * Allows code to override how to handle creating URLs for specific Entity resource actions. Used by the
 * {@link com.xpn.xwiki.web.ExportURLFactory} class.
 *
 * @version $Id$
 * @since 6.2RC1
 */
@Role
public interface ExportURLFactoryActionHandler
{
    /**
     * @param spaces a serialized space reference which can contain one or several spaces (e.g. "space1.space2"). If
     *        a space name contains a dot (".") it must be passed escaped as in "space1\.with\.dot.space2"
     * @param name the page name pointed to
     * @param queryString the optional query string
     * @param anchor the optional anchor
     * @param wikiId the wiki id pointed to
     * @param context the XWiki Context
     * @param exportContext the Context containing states of the export
     * @return the URL to generate at export
     * @throws Exception in case of an error
     * @since 7.2M1
     */
    URL createURL(String spaces, String name, String queryString, String anchor, String wikiId,
        XWikiContext context, FilesystemExportContext exportContext) throws Exception;
}
