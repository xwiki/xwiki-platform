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
package org.xwiki.wiki.workspacesmigrator.internal;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;

/**
 * Component to restore some document from a XAR attached to a wiki page.
 *
 * @version $Id$
 * @since 5.3RC1
 */
@Role
public interface DocumentRestorerFromAttachedXAR
{
    /**
     * Restore the desired documents from a xar.
     * @param docReference reference of the document that hold the XAR as attachment.
     * @param attachmentName the name of the attachment
     * @param documentsToRestore the list of documents we need to restore
     * @throws XWikiException if problems occur
     */
    void restoreDocumentFromAttachedXAR(DocumentReference docReference, String attachmentName,
            List<DocumentReference> documentsToRestore) throws XWikiException;
}
