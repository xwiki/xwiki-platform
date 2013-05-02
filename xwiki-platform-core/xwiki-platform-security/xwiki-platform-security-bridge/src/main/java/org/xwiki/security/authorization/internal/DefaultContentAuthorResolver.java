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
package org.xwiki.security.authorization.internal;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.XWikiDocument;

import javax.inject.Singleton;

/**
 * Bridge component for resolving content author on behalf of the AuthorizationContextFactory.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Singleton
public class DefaultContentAuthorResolver implements ContentAuthorResolver
{

    @Override
    public DocumentReference resolveContentAuthor(DocumentModelBridge document)
    {
        final XWikiDocument xwikiDocument = (XWikiDocument) document;

        if (xwikiDocument == null || xwikiDocument.isNew() || xwikiDocument.isContentDirty()
            || xwikiDocument.isMetaDataDirty()) {
            return null;
        }

        return xwikiDocument.getContentAuthorReference();
    }
}
