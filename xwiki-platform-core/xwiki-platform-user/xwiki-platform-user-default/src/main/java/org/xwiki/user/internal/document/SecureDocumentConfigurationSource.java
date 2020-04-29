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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Provider;

import org.xwiki.configuration.ConfigurationRight;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.ConfigurationSourceAuthorization;
import org.xwiki.configuration.internal.ConfigurationSourceDecorator;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.xwiki.user.internal.UserPropertyConstants.EMAIL;

/**
 * Configuration source decorator that performs permission checks for reading and writing user configuration
 * properties. Also handles the special case of the user email property (if the last current document's author has
 * Programming Rights, display the email in full, otherwise obfuscate it).
 *
 * @version $Id$
 * @since 12.4RC1
 */
public class SecureDocumentConfigurationSource extends ConfigurationSourceDecorator
{
    private Provider<XWikiContext> contextProvider;

    private ConfigurationSourceAuthorization authorization;

    private AuthorizationManager authorizationManager;

    private DocumentUserReference userReference;

    /**
     * @param userReference the user for which we're reading or writing the configuration properties
     * @param internalConfigurationSource the wrapped configuration source to call after the checks have been done
     * @param authorization the component to use to perform the checks on the configuration source
     * @param authorizationManager the component to use to perform permissions checks for handling the email use case
     * @param contextProvider the component to use to get the current document for the email use case
     */
    public SecureDocumentConfigurationSource(DocumentUserReference userReference,
        ConfigurationSource internalConfigurationSource, ConfigurationSourceAuthorization authorization,
        AuthorizationManager authorizationManager, Provider<XWikiContext> contextProvider)
    {
        super(internalConfigurationSource);
        this.userReference = userReference;
        this.authorization = authorization;
        this.authorizationManager = authorizationManager;
        this.contextProvider = contextProvider;
    }

    @Override
    public <T> T getProperty(String key, T defaultValue)
    {
        T value;
        if (hasAccess(key, ConfigurationRight.READ)) {
            value = execute(key, () -> getWrappedConfigurationSource().getProperty(key, defaultValue));
        } else {
            value = defaultValue;
        }
        return value;
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass)
    {
        T value;
        if (hasAccess(key, ConfigurationRight.READ)) {
            value = execute(key, () -> getWrappedConfigurationSource().getProperty(key, valueClass));
        } else {
            // Return defaults
            value = null;
        }
        return value;
    }

    @Override
    public <T> T getProperty(String key)
    {
        T value;
        if (hasAccess(key, ConfigurationRight.READ)) {
            value = execute(key, () -> getWrappedConfigurationSource().getProperty(key));
        } else {
            // Return defaults
            value = null;
        }
        return value;
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass, T defaultValue)
    {
        T value;
        if (hasAccess(key, ConfigurationRight.READ)) {
            value = execute(key,
                () -> getWrappedConfigurationSource().getProperty(key, valueClass, defaultValue));
        } else {
            value = defaultValue;
        }
        return value;
    }

    @Override
    public void setProperties(Map<String, Object> properties) throws ConfigurationSaveException
    {
        List<String> notAccessibleKeys = new ArrayList<>();
        boolean hasAccess = true;
        for (String key : properties.keySet()) {
            boolean access = hasAccess(key, ConfigurationRight.WRITE);
            if (!access) {
                notAccessibleKeys.add(String.format("[%s]", key));
            }
            hasAccess = hasAccess && access;
        }
        if (hasAccess) {
            this.getWrappedConfigurationSource().setProperties(properties);
        } else {
            throw new ConfigurationSaveException(String.format("No permission for user [%s] to modify keys [%s]",
                this.userReference, StringUtils.join(notAccessibleKeys, ',')));
        }
    }

    private <T> T execute(String key, Supplier<T> supplier)
    {
        T result;

        if (EMAIL.equals(key)) {
            // What we return depends on the permissions of the last author of the current document. If that author has
            // PR rights on the current document then return the full email, otherwise return an obfuscated version.
            XWikiContext xcontext = this.contextProvider.get();
            XWikiDocument currentDocument = xcontext.getDoc();
            if (currentDocument != null) {
                DocumentReference lastAuthorDocumentReference = currentDocument.getAuthorReference();
                if (this.authorizationManager.hasAccess(Right.PROGRAM, lastAuthorDocumentReference,
                    currentDocument.getDocumentReference()))
                {
                    result = supplier.get();
                } else {
                    // Obfuscate the email
                    result = (T) obfuscateEmail((String) supplier.get());
                }
            } else {
                // Obfuscate the email to be safe
                result = (T) obfuscateEmail((String) supplier.get());
            }
        } else {
            result = supplier.get();
        }

        return result;
    }

    private String obfuscateEmail(String email)
    {
        return email.replaceAll("^(.).*@", "$1...@");
    }

    private boolean hasAccess(String key, ConfigurationRight configurationRight)
    {
        return this.authorization.hasAccess(key, this.userReference, configurationRight);
    }
}
