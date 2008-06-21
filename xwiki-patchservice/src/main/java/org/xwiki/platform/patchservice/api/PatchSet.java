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

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A <tt>PatchSet</tt> groups several patches together, for easier transfer between hosts. A patchset must contain
 * patches affecting only one document.
 * 
 * @see RWPatchSet
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public interface PatchSet extends XmlSerializable
{
    /**
     * Get the list of patches included in this set.
     * 
     * @return The list of patches.
     */
    List<Patch> getPatches();

    /**
     * Apply this patch set on a document.
     * 
     * @param doc The document being patched.
     * @param context The XWiki context, needed for some document operations.
     * @throws XWikiException If the patchset cannot be applied on the document.
     */
    void apply(XWikiDocument doc, XWikiContext context) throws XWikiException;
}
