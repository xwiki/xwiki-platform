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
package org.xwiki.extension.repository.xwiki.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.version.Version;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.rendering.listener.reference.ResourceReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Role
public interface RepositoryManager
{
    XWikiDocument getExistingExtensionDocumentById(String extensionId) throws QueryException, XWikiException;

    BaseObject getExtensionVersion(XWikiDocument document, Version version);

    void validateExtension(XWikiDocument document, boolean readOnly) throws XWikiException;

    void validateExtensions() throws QueryException, XWikiException;

    ResourceReference getDownloadReference(XWikiDocument document, BaseObject extensionVersionObject);

    DocumentReference importExtension(String extensionId, ExtensionRepository repository, Version.Type type)
        throws QueryException, XWikiException, ResolveException;
}
