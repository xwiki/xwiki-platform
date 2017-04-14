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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The default implementation of {@link DocumentRevisionProvider}.
 * <p>
 * The main job of {@link DefaultDocumentRevisionProvider} is to call the right {@link DocumentRevisionProvider}
 * depending on the revision prefix.
 * 
 * @version $Id$
 * @since 9.3rc1
 */
public class DefaultDocumentRevisionProvider extends AbstractDocumentRevisionProvider
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named("database")
    private DocumentRevisionProvider databaseDocumentRevisionProvider;

    @Override
    public XWikiDocument getRevision(DocumentReference reference, String revision) throws XWikiException
    {
        // Parse the version
        String revisionPrefix;
        String shortRevision;
        if (revision != null) {
            int revisionPrefixIndex = revision.indexOf(':');
            revisionPrefix = revision.substring(0, revisionPrefixIndex);
            shortRevision = revisionPrefix.substring(revisionPrefixIndex + 1);
        } else {
            revisionPrefix = null;
            shortRevision = revision;
        }

        // Find the provider
        DocumentRevisionProvider provider;
        ComponentManager componentManager = this.componentManagerProvider.get();
        if (revision != null && componentManager.hasComponent(DocumentRevisionProvider.class, revisionPrefix)) {
            try {
                provider = componentManager.getInstance(DocumentRevisionProvider.class, revisionPrefix);
            } catch (ComponentLookupException e) {
                throw new XWikiException("Failed to get revision provider for revision [" + revision + "]", e);
            }
        } else {
            provider = this.databaseDocumentRevisionProvider;
        }

        // Load the document revision
        return provider.getRevision(reference, shortRevision);
    }
}
