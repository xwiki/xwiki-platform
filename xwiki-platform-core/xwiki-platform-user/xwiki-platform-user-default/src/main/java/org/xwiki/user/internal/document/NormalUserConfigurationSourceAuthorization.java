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
package org.xwiki.user.internal.document;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.BooleanUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationRight;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSourceAuthorization;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Check permissions for reading/writing configuration properties for normal users (i.e. users having a profile
 * document containing their configuration).
 *
 * @version $Id$
 * @since 12.4RC1
 */
@Component
@Named("normaluser")
@Singleton
public class NormalUserConfigurationSourceAuthorization extends AbstractDocumentConfigurationSourceAuthorization
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentResolver;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentResolver;

    @Override
    protected DocumentReference getDocumentReference()
    {
        return this.documentAccessBridge.getCurrentUserReference();
    }

    @Override
    public boolean hasAccess(String key, UserReference userReference, ConfigurationRight right)
    {
        boolean hasAccess;

        XWikiContext xcontext = this.contextProvider.get();
        boolean isInRenderingEngine = BooleanUtils.toBoolean((Boolean) xcontext.get("isInRenderingEngine"));

        // Check: Verify that the last author did not create some "honeypot" attack where you'd have someone without
        // the proper permission (but still having edit rights on a page) to write a script that calls the configuration
        // API and then waiting for someone with the proper permissions to view the page, thus making the configuration
        // calls work.
        // We only perform this check if the call is made inside the page content (hence isInRenderingEngine is true)
        // since if the call is made inside a velocity template the access should always be granted.
        if (isInRenderingEngine) {
            XWikiDocument currentDocument = xcontext.getDoc();
            if (currentDocument != null) {
                DocumentReference lastAuthorDocumentReference = currentDocument.getAuthorReference();
                DocumentReference originalUserReference = this.documentAccessBridge.getCurrentUserReference();
                try {
                    // Note: The userReference passed is the reference to the user for which we're retrieving or setting
                    // the configuration properties. Thus we set it as the current user so that the call to
                    // super.hasAccess() checks the permissions of the last author of current doc on that user's
                    // document.
                    xcontext.setUserReference(((DocumentUserReference) userReference).getReference());
                    hasAccess = super.hasAccess(key, this.documentResolver.resolve(lastAuthorDocumentReference), right);
                } finally {
                    xcontext.setUserReference(originalUserReference);
                }
            } else {
                // No current document, this is not really normal. To be extra safe, check that the current user has the
                // permissions.
                hasAccess = super.hasAccess(key, this.currentResolver.resolve(null), right);
            }
        } else {
            hasAccess = true;
        }
        return hasAccess;
    }
}
