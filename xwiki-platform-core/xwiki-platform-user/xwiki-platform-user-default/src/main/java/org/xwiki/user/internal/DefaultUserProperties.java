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
package org.xwiki.user.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.user.Editor;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserType;

import static org.xwiki.user.internal.UserPropertyConstants.ACTIVE;
import static org.xwiki.user.internal.UserPropertyConstants.DISPLAY_HIDDEN_DOCUMENTS;
import static org.xwiki.user.internal.UserPropertyConstants.EDITOR;
import static org.xwiki.user.internal.UserPropertyConstants.EMAIL;
import static org.xwiki.user.internal.UserPropertyConstants.EMAIL_CHECKED;
import static org.xwiki.user.internal.UserPropertyConstants.FIRST_NAME;
import static org.xwiki.user.internal.UserPropertyConstants.LAST_NAME;
import static org.xwiki.user.internal.UserPropertyConstants.USER_TYPE;

/**
 * Common code for implementing user properties.
 *
 * @version $Id$
 * @since 12.2
 */
public class DefaultUserProperties implements UserProperties
{
    /**
     * Preserve the order in the map so that it's the same as expected by the user.
     */
    protected Map<String, Object> unsavedProperties = new LinkedHashMap<>();

    private ConfigurationSource configurationSource;

    /**
     * @param configurationSource the component providing the user properties
     */
    public DefaultUserProperties(ConfigurationSource configurationSource)
    {
        this.configurationSource = configurationSource;
    }

    @Override
    public boolean displayHiddenDocuments()
    {
        return getProperty(DISPLAY_HIDDEN_DOCUMENTS, Boolean.class, false);
    }

    @Override
    public void setDisplayHiddenDocuments(boolean displayHiddenDocuments)
    {
        this.unsavedProperties.put(DISPLAY_HIDDEN_DOCUMENTS, displayHiddenDocuments);
    }

    @Override
    public boolean isActive()
    {
        return getProperty(ACTIVE, Boolean.class, false);
    }

    @Override
    public void setActive(boolean isActive)
    {
        this.unsavedProperties.put(ACTIVE, isActive);
    }

    @Override
    public String getFirstName()
    {
        return (String) getProperty(FIRST_NAME);
    }

    @Override
    public void setFirstName(String firstName)
    {
        this.unsavedProperties.put(FIRST_NAME, firstName);
    }

    @Override
    public String getLastName()
    {
        return (String) getProperty(LAST_NAME);
    }

    @Override
    public void setLastName(String lastName)
    {
        this.unsavedProperties.put(LAST_NAME, lastName);
    }

    @Override
    public InternetAddress getEmail()
    {
        return getProperty(EMAIL, InternetAddress.class);
    }

    @Override
    public void setEmail(InternetAddress emailAddress)
    {
        this.unsavedProperties.put(EMAIL, emailAddress);
    }

    @Override
    public UserType getType()
    {
        return UserType.fromString(getProperty(USER_TYPE));
    }

    @Override
    public void setType(UserType type)
    {
        this.unsavedProperties.put(USER_TYPE, type);
    }

    @Override
    public Editor getEditor()
    {
        return Editor.fromString(getProperty(EDITOR));
    }

    @Override
    public void setEditor(Editor editor)
    {
        this.unsavedProperties.put(EDITOR, editor);
    }

    @Override
    public boolean isEmailChecked()
    {
        return getProperty(EMAIL_CHECKED, Boolean.class, false);
    }

    @Override
    public void setEmailChecked(boolean isEmailChecked)
    {
        this.unsavedProperties.put(EMAIL_CHECKED, isEmailChecked);
    }

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
    public List<String> getKeys()
    {
        return getConfigurationSource().getKeys();
    }

    @Override
    public List<String> getKeys(String prefix)
    {
        return getConfigurationSource().getKeys(prefix);
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
    public boolean isEmpty(String prefix)
    {
        return getConfigurationSource().isEmpty(prefix);
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass, T defaultValue)
    {
        return getConfigurationSource().getProperty(key, valueClass, defaultValue);
    }

    @Override
    public <T> T getProperty(String key)
    {
        return getConfigurationSource().getProperty(key);
    }

    @Override
    public void setProperties(Map<String, Object> properties) throws ConfigurationSaveException
    {
        getConfigurationSource().setProperties(properties);
    }

    @Override
    public void save() throws ConfigurationSaveException
    {
        getConfigurationSource().setProperties(this.unsavedProperties);
        this.unsavedProperties.clear();
    }

    protected ConfigurationSource getConfigurationSource()
    {
        return this.configurationSource;
    }
}
