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

import java.util.List;
import java.util.function.Supplier;

import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.user.Editor;
import org.xwiki.user.User;
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
 * Common code for implementing Users.
 *
 * @version $Id$
 * @since 12.2RC1
 */
public abstract class AbstractUser implements User
{
    private ConfigurationSource userConfigurationSource;

    /**
     * @param userConfigurationSource the component providing the user configuration data
     */
    public AbstractUser(ConfigurationSource userConfigurationSource)
    {
        this.userConfigurationSource = userConfigurationSource;
    }

    @Override
    public boolean displayHiddenDocuments()
    {
        return getProperty(DISPLAY_HIDDEN_DOCUMENTS, Boolean.class, false);
    }

    @Override
    public boolean isActive()
    {
        return getProperty(ACTIVE, Boolean.class, false);
    }

    @Override
    public String getFirstName()
    {
        return (String) getProperty(FIRST_NAME);
    }

    @Override
    public String getLastName()
    {
        return (String) getProperty(LAST_NAME);
    }

    @Override
    public String getEmail()
    {
        return (String) getProperty(EMAIL);
    }

    @Override
    public UserType getType()
    {
        return UserType.fromString(getProperty(USER_TYPE));
    }

    @Override
    public Editor getEditor()
    {
        return Editor.fromString(getProperty(EDITOR));
    }

    @Override
    public boolean isEmailChecked()
    {
        return getProperty(EMAIL_CHECKED, Boolean.class, false);
    }

    @Override
    public <T> T getProperty(String key, T defaultValue)
    {
        return execute(() -> getUserConfigurationSource().getProperty(key, defaultValue));
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass)
    {
        return execute(() -> getUserConfigurationSource().getProperty(key, valueClass));
    }

    @Override
    public List<String> getKeys()
    {
        return execute(() -> getUserConfigurationSource().getKeys());
    }

    @Override
    public boolean containsKey(String key)
    {
        return execute(() -> getUserConfigurationSource().containsKey(key));
    }

    @Override
    public boolean isEmpty()
    {
        return execute(() -> getUserConfigurationSource().isEmpty());
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass, T defaultValue)
    {
        return execute(() -> getUserConfigurationSource().getProperty(key, valueClass, defaultValue));
    }

    @Override
    public <T> T getProperty(String key)
    {
        return execute(() -> getUserConfigurationSource().getProperty(key));
    }

    protected ConfigurationSource getUserConfigurationSource()
    {
        return this.userConfigurationSource;
    }

    protected <T> T execute(Supplier<T> supplier)
    {
        return supplier.get();
    }
}
