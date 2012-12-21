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
package com.xpn.xwiki;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

public class XWikiConstant
{
    public static final String TAG_CLASS = "XWiki.TagClass";

    public static final String TAG_CLASS_PROP_TAGS = "tags";

    /**
     * The class that holds the default edit mode for a document. The object of this class can be attached either to the
     * document itself or to an included sheet. If both are found, the one attached to the document is used.
     * 
     * @since 3.1M2
     */
    public static final EntityReference EDIT_MODE_CLASS = new EntityReference("EditModeClass", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    /** The context key which is used to signify that permissions have been dropped. */
    public static final String DROPPED_PERMISSIONS = "hasDroppedPermissions";
}
