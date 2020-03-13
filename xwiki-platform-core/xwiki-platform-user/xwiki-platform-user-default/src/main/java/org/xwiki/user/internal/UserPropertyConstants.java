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

import org.xwiki.user.User;

/**
 * Constants representing user configuration property names.
 *
 * @version $Id$
 * @since 12.2RC1
 */
public final class UserPropertyConstants
{
    /**
     * See {@link User#displayHiddenDocuments()}.
     */
    public static final String DISPLAY_HIDDEN_DOCUMENTS = "displayHiddenDocuments";

    /**
     * See {@link User#isActive()}.
     */
    public static final String ACTIVE = "active";

    /**
     * See {@link User#getFirstName()}.
     */
    public static final String FIRST_NAME = "first_name";

    /**
     * See {@link User#getLastName()}.
     */
    public static final String LAST_NAME = "last_name";

    /**
     * See {@link User#getEmail()}.
     */
    public static final String EMAIL = "email";

    /**
     * See {@link User#isEmailChecked()}.
     */
    public static final String EMAIL_CHECKED = "email_checked";

    /**
     * See {@link User#getType()}.
     */
    public static final String USER_TYPE = "usertype";

    /**
     * See {@link User#getEditor()}.
     */
    public static final String EDITOR = "editor";

    private UserPropertyConstants()
    {
        // Utility classes should not have a public or default constructor.
    }
}
