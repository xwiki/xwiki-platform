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
package org.xwiki.extension.xar;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Return an instance of {@link XWikiDocument} which is the result of a merge based on the provided input.
 * 
 * @version $Id$
 * @since 10.3RC1
 */
@Role
@Unstable
public interface XWikiDocumentMerger
{
    /**
     * @param currentDocument the document currently in database
     * @param previousDocument the standard version of the document
     * @param nextDocument the new standard version of the document
     * @param configuration the configuration of the merge
     * @return the result of the merge
     * @throws XarExtensionExtension when failing to execute the merge
     */
    XWikiDocument merge(XWikiDocument currentDocument, XWikiDocument previousDocument, XWikiDocument nextDocument,
        XWikiDocumentMergerConfiguration configuration) throws XarExtensionExtension;
}
