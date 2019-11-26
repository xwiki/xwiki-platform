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
package org.xwiki.security.internal;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * This class contains constants for accessing various xwiki elements.
 *
 * @version $Id$
 * @since 4.0M2
 */
public interface XWikiConstants
{
    /** Name of the space where user document and global preferences is stored. */
    String XWIKI_SPACE = "XWiki";

    /** Name of document where wiki rights are stored. */
    String WIKI_DOC = "XWikiPreferences";

    /**
     * Relative reference of the space where user document and global preferences is stored.
     * 
     * @since 7.2M1
     */
    EntityReference XWIKI_SPACE_REFERENCE = new EntityReference(XWIKI_SPACE, EntityType.SPACE);

    /**
     * XWiki.XWikiPreferences reference.
     * 
     * @since 7.2M1
     */
    LocalDocumentReference WIKI_DOC_REFERENCE = new LocalDocumentReference(XWIKI_SPACE, WIKI_DOC);

    /** Name of document where space rights are stored. */
    String SPACE_DOC = "WebPreferences";

    /** Prefix of wiki descriptor documents. */
    String WIKI_DESCRIPTOR_PREFIX = "XWikiServer";

    /**
     * Name of the group class.
     * 
     * @since 11.8RC1
     */
    String GROUP_CLASS_NAME = "XWikiGroups";

    /** Full name of group class. */
    String GROUP_CLASS = XWIKI_SPACE + '.' + GROUP_CLASS_NAME;

    /**
     * The reference of he group class.
     * 
     * @since 11.8RC1
     */
    LocalDocumentReference GROUP_CLASS_REFERENCE = new LocalDocumentReference(XWIKI_SPACE, GROUP_CLASS_NAME);

    /** XWiki class for storing global rights. */
    String GLOBAL_CLASSNAME = "XWikiGlobalRights";

    /**
     * XWiki class for storing global rights.
     * 
     * @since 11.8RC1
     */
    String GLOBAL_CLASS = XWIKI_SPACE + '.' + GLOBAL_CLASSNAME;

    /**
     * The reference of the class for storing global rights.
     * 
     * @since 11.8RC1
     */
    LocalDocumentReference GLOBAL_CLASS_REFERENCE = new LocalDocumentReference(XWIKI_SPACE, GLOBAL_CLASSNAME);

    /** XWiki class for storing rights. */
    String LOCAL_CLASSNAME = "XWikiRights";

    /**
     * XWiki class for storing rights.
     * 
     * @since 11.8RC1
     */
    String LOCAL_CLASS = XWIKI_SPACE + '.' + LOCAL_CLASSNAME;

    /**
     * The reference of the class for storing rights.
     * 
     * @since 11.8RC1
     */
    LocalDocumentReference LOCAL_CLASS_REFERENCE = new LocalDocumentReference(XWIKI_SPACE, LOCAL_CLASSNAME);

    /** Field name for level in xwiki rights object. */
    String LEVELS_FIELD_NAME = "levels";

    /** Field name for group in xwiki rights object. */
    String GROUPS_FIELD_NAME = "groups";

    /** Field name for users in xwiki rights object. */
    String USERS_FIELD_NAME = "users";

    /** Field name for rights in xwiki rights object. */
    String ALLOW_FIELD_NAME = "allow";

    /** The Guest username. */
    String GUEST_USER = "XWikiGuest";

    /** The Guest full name. */
    String GUEST_USER_FULLNAME = XWIKI_SPACE + '.' + GUEST_USER;
}
