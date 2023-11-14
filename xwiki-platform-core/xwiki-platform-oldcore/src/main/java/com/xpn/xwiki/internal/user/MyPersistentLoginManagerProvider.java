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
package com.xpn.xwiki.internal.user;

import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.securityfilter.authenticator.persistent.PersistentLoginManagerInterface;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.security.authentication.AuthenticationConfiguration;

import com.xpn.xwiki.user.impl.xwiki.MyPersistentLoginManager;

/**
 * Default provider of {@link MyPersistentLoginManager}.
 * 
 * @version $Id$
 * @since 14.10
 */
@Component
@Singleton
public class MyPersistentLoginManagerProvider implements Provider<PersistentLoginManagerInterface>
{
    @Inject
    @Named("xwikicfg")
    private ConfigurationSource config;

    @Inject
    private AuthenticationConfiguration authenticationConfiguration;

    @Override
    public PersistentLoginManagerInterface get()
    {
        MyPersistentLoginManager persistentLoginManager = new MyPersistentLoginManager();

        setIfSpecified("cookieprefix", persistentLoginManager::setCookiePrefix);
        setIfSpecified("cookiepath", persistentLoginManager::setCookiePath);
        persistentLoginManager.setCookieDomains(
            this.authenticationConfiguration.getCookieDomains().toArray(new String[0]));
        setIfSpecified("cookielife", persistentLoginManager::setCookieLife);
        setIfSpecified("protection", persistentLoginManager::setProtection);
        setIfSpecified("useip", persistentLoginManager::setUseIP);
        setIfSpecified("encryptionalgorithm", persistentLoginManager::setEncryptionAlgorithm);
        setIfSpecified("encryptionmode", persistentLoginManager::setEncryptionMode);
        setIfSpecified("encryptionpadding", persistentLoginManager::setEncryptionPadding);

        persistentLoginManager.setValidationKey(this.authenticationConfiguration.getValidationKey());
        persistentLoginManager.setEncryptionKey(this.authenticationConfiguration.getEncryptionKey());

        return persistentLoginManager;
    }

    private void setIfSpecified(String property, Consumer<String> setter)
    {
        String value = this.config.getProperty("xwiki.authentication." + property);
        if (value != null) {
            setter.accept(value);
        }
    }
}
