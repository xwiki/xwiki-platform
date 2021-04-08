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

import org.xwiki.configuration.ConfigurationRight;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.ConfigurationSourceAuthorization;
import org.xwiki.configuration.internal.ConfigurationSourceDecorator;
import org.xwiki.text.StringUtils;

/**
 * Configuration source decorator that performs permission checks for reading and writing user configuration
 * properties.
 *
 * @version $Id$
 * @since 12.4RC1
 */
public class SecureDocumentConfigurationSource extends ConfigurationSourceDecorator
{
    private ConfigurationSourceAuthorization authorization;

    private DocumentUserReference userReference;

    /**
     * @param userReference the user for which we're reading or writing the configuration properties
     * @param internalConfigurationSource the wrapped configuration source to call after the checks have been done
     * @param authorization the component to use to perform the checks on the configuration source
     */
    public SecureDocumentConfigurationSource(DocumentUserReference userReference,
        ConfigurationSource internalConfigurationSource, ConfigurationSourceAuthorization authorization)
    {
        super(internalConfigurationSource);
        this.userReference = userReference;
        this.authorization = authorization;
    }

    @Override
    public <T> T getProperty(String key, T defaultValue)
    {
        T value;
        if (hasAccess(key, ConfigurationRight.READ)) {
            value = getWrappedConfigurationSource().getProperty(key, defaultValue);
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
            value = getWrappedConfigurationSource().getProperty(key, valueClass);
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
            value = getWrappedConfigurationSource().getProperty(key);
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
            value = getWrappedConfigurationSource().getProperty(key, valueClass, defaultValue);
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

    private boolean hasAccess(String key, ConfigurationRight configurationRight)
    {
        return this.authorization.hasAccess(key, this.userReference, configurationRight);
    }
}
