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
package org.xwiki.platform.patchservice.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The <tt>PatchCreator</tt> is a helper class, which generates {@link Patch patches} from two versions of a document,
 * by comparing the document versions and splitting the differences into basic {@link Operation operations}.
 * 
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public interface PatchCreator
{
    void init(XWikiContext context);

    /**
     * Create a {@link Patch} which, when applied on oldDoc, will result in newDoc.
     * 
     * @param oldDoc The initial version of the document.
     * @param newDoc The updated version of the document.
     * @return A {@link Patch patch} reflecting the changes between the two versions.
     */
    Patch getPatch(XWikiDocument oldDoc, XWikiDocument newDoc, XWikiContext context);
}
