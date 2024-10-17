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
package org.xwiki.internal.document;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link DocumentRequiredRightsManager}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultDocumentRequiredRightsManager implements DocumentRequiredRightsManager
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentRequiredRightsReader documentRequiredRightsReader;

    @Override
    public Optional<DocumentRequiredRights> getRequiredRights(DocumentReference documentReference)
        throws AuthorizationException
    {
        if (documentReference != null) {
            XWikiContext context = this.contextProvider.get();

            // Load the document.
            try {
                XWikiDocument document = context.getWiki().getDocument(documentReference, context);
                if (!document.isNew()) {
                    return Optional.of(this.documentRequiredRightsReader.readRequiredRights(document));
                }
            } catch (XWikiException e) {
                throw new AuthorizationException("Failed to load the document", e);
            }
        }

        return Optional.empty();
    }
}
