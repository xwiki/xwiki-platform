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

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.ConfigurationSourceAuthorization;
import org.xwiki.security.authorization.AuthorizationManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Get the user properties (including all inherited ones) for a {@link DocumentUserReference}, by extracting them from
 * the {@code XWiki.XWikiUsers} object.
 *
 * @version $Id$
 * @since 12.2
 */
@Component
@Named("secure/all/org.xwiki.user.internal.document.DocumentUserReference")
@Singleton
public class SecureAllDocumentUserPropertiesResolver extends AbstractDocumentUserPropertiesResolver
{
    @Inject
    @Named("all")
    private ConfigurationSource configurationSource;

    @Override
    protected ConfigurationSource getConfigurationSource(DocumentUserReference userReference,
        ConfigurationSourceAuthorization authorization, AuthorizationManager authorizationManager,
        Provider<XWikiContext> contextProvider)
    {
        return new SecureDocumentConfigurationSource(userReference, new CurrentUserConfigurationSource(userReference,
            this.configurationSource, contextProvider), authorization);
    }
}
