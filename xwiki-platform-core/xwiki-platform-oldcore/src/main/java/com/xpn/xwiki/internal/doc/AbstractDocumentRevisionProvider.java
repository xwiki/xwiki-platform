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
package com.xpn.xwiki.internal.doc;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Base class used by DocumentRevisionProvider implementations.
 * 
 * @version $Id$
 * @since 9.3rc1
 */
public abstract class AbstractDocumentRevisionProvider implements DocumentRevisionProvider
{
    @Override
    public XWikiDocument getRevision(XWikiDocument document, String revision) throws XWikiException
    {
        XWikiDocument newdoc;

        if (StringUtils.isEmpty(revision)) {
            newdoc = new XWikiDocument(document.getDocumentReference());
        } else if (revision.equals(document.getVersion())) {
            newdoc = document;
        } else {
            newdoc = getRevision(document.getDocumentReferenceWithLocale(), revision);
        }

        return newdoc;
    }
}
