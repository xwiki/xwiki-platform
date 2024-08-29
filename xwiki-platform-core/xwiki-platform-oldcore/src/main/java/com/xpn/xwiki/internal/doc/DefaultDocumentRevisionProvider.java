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
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;

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
@Component
@Singleton
public class DefaultDocumentRevisionProvider extends AbstractDocumentRevisionProvider
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named("database")
    private DocumentRevisionProvider databaseDocumentRevisionProvider;

    private Pair<String, String> parseRevision(String revision)
    {
        String revisionPrefix = null;
        if (revision != null) {
            int revisionPrefixIndex = revision.indexOf(':');
            if (revisionPrefixIndex > 0) {
                revisionPrefix = revision.substring(0, revisionPrefixIndex);
            }
        }
        String shortRevision;
        if (revisionPrefix != null) {
            shortRevision = revision.substring(revisionPrefix.length() + 1);
        } else {
            shortRevision = revision;
        }
        return Pair.of(revisionPrefix, shortRevision);
    }

    private DocumentRevisionProvider getProvider(String revisionPrefix) throws XWikiException
    {
        // Find the provider
        DocumentRevisionProvider provider = this.databaseDocumentRevisionProvider;
        if (revisionPrefix != null) {
            ComponentManager componentManager = this.componentManagerProvider.get();
            if (componentManager.hasComponent(DocumentRevisionProvider.class, revisionPrefix)) {
                try {
                    provider = componentManager.getInstance(DocumentRevisionProvider.class, revisionPrefix);
                } catch (ComponentLookupException e) {
                    throw new XWikiException("Failed to get revision provider for revision [" + revisionPrefix + "]",
                        e);
                }
            }
        }
        return provider;
    }

    @Override
    public XWikiDocument getRevision(DocumentReference reference, String revision) throws XWikiException
    {
        Pair<String, String> parsedRevision = parseRevision(revision);

        // Load the document revision
        return getProvider(parsedRevision.getLeft()).getRevision(reference, parsedRevision.getRight());
    }

    @Override
    public void checkAccess(Right right, UserReference userReference, DocumentReference documentReference,
        String revision) throws AuthorizationException, XWikiException
    {
        Pair<String, String> parsedRevision = parseRevision(revision);

        getProvider(parsedRevision.getLeft())
            .checkAccess(right, userReference, documentReference, parsedRevision.getRight());
    }
}
