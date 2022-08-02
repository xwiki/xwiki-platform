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
package org.xwiki.extension.xar.internal.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.XarExtensionConfiguration;
import org.xwiki.extension.xar.XarExtensionConfiguration.DocumentProtection;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.extension.xar.security.ProtectionLevel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserType;

import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Various internal tools to deal with XAR extensions documents security.
 * 
 * @version $Id$
 * @since 1.5RC1
 */
@Component(roles = XarSecurityTool.class)
@Singleton
public class XarSecurityTool
{
    @Inject
    private XarExtensionConfiguration configuration;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository installedXARs;

    @Inject
    private Provider<AuthorizationManager> authorizationProvider;

    @Inject
    private UserPropertiesResolver userPropertiesResolver;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    // Not injected directly to not create a loop
    private AuthorizationManager authorization;

    private AuthorizationManager getAuthorization()
    {
        if (this.authorization == null) {
            this.authorization = this.authorizationProvider.get();
        }

        return this.authorization;
    }

    /**
     * @param right the right to test
     * @param userReference the reference of the user
     * @param documentReference the reference of the document
     * @return The kind of protection to apply.
     */
    public ProtectionLevel getProtectionLevel(Right right, DocumentReference userReference,
        DocumentReference documentReference)
    {
        DocumentProtection protection = this.configuration.getDocumentProtection();

        if (protection != DocumentProtection.NONE
            && !((XarInstalledExtensionRepository) this.installedXARs).isAllowed(documentReference, right)) {
            if (protection.isDeny() && !XWikiRightService.isSuperAdmin(userReference)) {
                // Check access
                if (!getAuthorization().hasAccess(right, userReference, documentReference)) {
                    return ProtectionLevel.DENY;
                }

                // Deal with implied rights
                if (isForcedDeny(protection, userReference)) {
                    return ProtectionLevel.DENY;
                }
            }

            return ProtectionLevel.WARNING;
        }

        return ProtectionLevel.NONE;
    }

    private boolean isForcedDeny(DocumentProtection protection, DocumentReference userReference)
    {
        return protection.isForced() && (!protection.isSimple() || isSimpleUser(userReference));
    }

    /**
     * @param userReference the reference of the user
     * @return boolean if the passed user is not an advanced user
     */
    public boolean isSimpleUser(DocumentReference userReference)
    {
        return UserType.SIMPLE == this.userPropertiesResolver.resolve(
            this.userReferenceResolver.resolve(userReference)).getType();
    }
}
