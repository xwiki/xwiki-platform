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
package org.xwiki.user;

import javax.mail.internet.InternetAddress;

import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Represents all the properties of an XWiki user. It can represent direct properites or inherited properties.
 * Note that it's independent from where users are stored, and should remain that way, so that we can switch the user
 * store in the future.
 *
 * @version $Id$
 * @since 12.2
 */
public interface UserProperties extends ConfigurationSource
{
    /**
     * @return true if the user is configured to display hidden documents in the wiki
     */
    boolean displayHiddenDocuments();

    /**
     * @param displayHiddenDocuments see {@link #displayHiddenDocuments()}
     */
    void setDisplayHiddenDocuments(boolean displayHiddenDocuments);

    /**
     * @return true if the user is active in the wiki. An active user can log in.
     */
    boolean isActive();

    /**
     * @param isActive see {@link #isActive()}
     */
    void setActive(boolean isActive);

    /**
     * @return the first name of the user or null if not set
     */
    String getFirstName();

    /**
     * @param firstName see {@link #getFirstName()}
     */
    void setFirstName(String firstName);

    /**
     * @return the last name of the user or null if not set
     */
    String getLastName();

    /**
     * @param lastName see {@link #getLastName()}
     */
    void setLastName(String lastName);

    /**
     * @return the email address of the user and null if not set
     */
    InternetAddress getEmail();

    /**
     * @param email see {@link #getEmail()}
     */
    void setEmail(InternetAddress email);

    /**
     * @return the type of the user (simple user, advanced user)
     * @see <a href="https://bit.ly/37TUlCp">user profile</a>
     */
    UserType getType();

    /**
     * @param type see {@link #getType()}
     */
    void setType(UserType type);

    /**
     * @return the default editor to use when editing content for this user (text editor, wysiwyg editor)
     */
    Editor getEditor();

    /**
     * @param editor see {@link #getEditor()}
     */
    void setEditor(Editor editor);

    /**
     * @return true if the user's email has been checked. In some configurations, users must have had their emails
     *         verified before they can access the wiki. Also, disabled users must have their emails checked to be
     *         able to view pages.
     */
    boolean isEmailChecked();

    /**
     * @param isEmailChecked see {@link #isEmailChecked()}
     */
    void setEmailChecked(boolean isEmailChecked);

    /**
     * Persist the various {@code setXXX()} calls made since the last call to this method.
     *
     * @throws ConfigurationSaveException in case of an error during the save
     */
    void save() throws ConfigurationSaveException;
}
