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
package org.xwiki.xar.internal.model;

/**
 * @version $Id$
 * @since 5.4M1
 */
public class XarModel
{
    public static final String PATH_PACKAGE = "package.xml";

    public static final String ELEMENT_PACKAGE = "package";

    public static final String ELEMENT_INFOS = "infos";

    public static final String ELEMENT_INFOS_NAME = "name";

    public static final String ELEMENT_INFOS_DESCRIPTION = "description";

    public static final String ELEMENT_INFOS_LICENSE = "licence";

    public static final String ELEMENT_INFOS_AUTHOR = "author";

    public static final String ELEMENT_INFOS_VERSION = "version";

    public static final String ELEMENT_INFOS_ISBACKUPPACK = "backupPack";

    public static final String ELEMENT_INFOS_ISPRESERVEVERSION = "preserveVersion";

    public static final String ELEMENT_INFOS_EXTENSIONID = "extensionId";

    public static final String ELEMENT_FILES = "files";

    public static final String ELEMENT_FILES_FILE = "file";

    @Deprecated
    public static final String ELEMENT_FILES_FILES = ELEMENT_FILES_FILE;

    public static final String ATTRIBUTE_DEFAULTACTION = "defaultAction";

    public static final String ATTRIBUTE_LOCALE = "language";

    /**
     * @since 10.3
     */
    public static final String ATTRIBUTE_TYPE = "type";

    // action

    /**
     * @deprecated kept for retro compatibility reason (since this used to exist in XAR format) but this method never
     *             really been used in XWiki Standard.
     */
    public static final int ACTION_NOT_DEFINED = -1;

    /**
     * @deprecated kept for retro compatibility reason (since this used to exist in XAR format) but this method never
     *             really been used in XWiki Standard.
     */
    public static final int ACTION_OVERWRITE = 0;

    /**
     * @deprecated kept for retro compatibility reason (since this used to exist in XAR format) but this method never
     *             really been used in XWiki Standard.
     */
    public static final int ACTION_SKIP = 1;

    /**
     * @deprecated kept for retro compatibility reason (since this used to exist in XAR format) but this method never
     *             really been used in XWiki Standard.
     */
    public static final int ACTION_MERGE = 2;
}
