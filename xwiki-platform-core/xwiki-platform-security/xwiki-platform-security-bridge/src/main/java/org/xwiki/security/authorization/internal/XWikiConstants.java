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
package org.xwiki.security.authorization.internal;

/**
 * This class contains constants for accessing various xwiki elements.
 *
 * @version $Id$
 * @since 4.0M2
 */
interface XWikiConstants
{
    /** Name of the space where user document and global preferences is stored. */
    String WIKI_SPACE = "XWiki";

    /** Name of document where wiki rights are stored. */
    String WIKI_DOC = "XWikiPreferences";

    /** Name of document where space rights are stored. */
    String SPACE_DOC = "WebPreferences";

    /** Name of group class. */
    String GROUP_CLASS = "XWiki.XWikiGroups";

    /** XWiki class for storing global rights. */
    String GLOBAL_CLASSNAME = "XWikiGlobalRights";

    /** XWiki class for storing rights. */
    String LOCAL_CLASSNAME = "XWikiRights";

    /** Field name for group in xwiki rights object. */
    String GROUPS_FIELD_NAME = "groups";

    /** Field name for users in xwiki rights object. */
    String USERS_FIELD_NAME  = "users";

    /** Field name for rights in xwiki rights object. */
    String ALLOW_FIELD_NAME  = "allow";

    /** The Guest username. */
    String GUEST_USER = "XWikiGuest";

    /** The Guest full name. */
    String GUEST_USER_FULLNAME = WIKI_SPACE + '.' + GUEST_USER;
}
