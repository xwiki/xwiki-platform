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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.AbstractConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Configuration source for any users (normal, superadmin, or guest users). This is mostly a legacy-helper so that
 * old usages of the "user" Configuration Source will work also for superadmin and guest users. The User API must be
 * used otherwise.
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Component
@Named("user")
@Singleton
public class UserPreferencesConfigurationSource extends AbstractConfigurationSource
{
    @Inject
    @Named("normaluser")
    private ConfigurationSource normalUserConfigurationSource;

    @Inject
    @Named("superadminuser")
    private ConfigurationSource superAdminConfigurationSource;

    @Inject
    @Named("guestuser")
    private ConfigurationSource guestConfigurationSource;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Override
    public <T> T getProperty(String key, T defaultValue)
    {
        return getConfigurationSource().getProperty(key, defaultValue);
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass)
    {
        return getConfigurationSource().getProperty(key, valueClass);
    }

    @Override
    public <T> T getProperty(String key)
    {
        return getConfigurationSource().getProperty(key);
    }

    @Override
    public List<String> getKeys()
    {
        return getConfigurationSource().getKeys();
    }

    @Override
    public boolean containsKey(String key)
    {
        return getConfigurationSource().containsKey(key);
    }

    @Override
    public boolean isEmpty()
    {
        return getConfigurationSource().isEmpty();
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass, T defaultValue)
    {
        return getConfigurationSource().getProperty(key, valueClass, defaultValue);
    }

    private ConfigurationSource getConfigurationSource()
    {
        ConfigurationSource configurationSource;
        UserReference userReference = this.userReferenceResolver.resolve(getDocumentReference());
        if (SuperAdminUserReference.INSTANCE == userReference) {
            configurationSource = this.superAdminConfigurationSource;
        } else if (GuestUserReference.INSTANCE == userReference) {
            configurationSource = this.guestConfigurationSource;
        } else {
            configurationSource = this.normalUserConfigurationSource;
        }
        return configurationSource;
    }

    private DocumentReference getDocumentReference()
    {
        return this.documentAccessBridge.getCurrentUserReference();
    }
}
