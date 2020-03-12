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
public interface UserPropertyConstants
{
    /**
     * See {@link User#displayHiddenDocuments()}.
     */
    String DISPLAY_HIDDEN_DOCUMENTS = "displayHiddenDocuments";

    /**
     * See {@link User#isActive()}.
     */
    String ACTIVE = "active";

    /**
     * See {@link User#getFirstName()}.
     */
    String FIRST_NAME = "first_name";

    /**
     * See {@link User#getLastName()}.
     */
    String LAST_NAME = "last_name";

    /**
     * See {@link User#getEmail()}.
     */
    String EMAIL = "email";

    /**
     * See {@link User#isEmailChecked()}.
     */
    String EMAIL_CHECKED = "email_checked";

    /**
     * See {@link User#getType()}.
     */
    String USER_TYPE = "usertype";

    /**
     * See {@link User#getEditor()}.
     */
    String EDITOR = "editor";
}
