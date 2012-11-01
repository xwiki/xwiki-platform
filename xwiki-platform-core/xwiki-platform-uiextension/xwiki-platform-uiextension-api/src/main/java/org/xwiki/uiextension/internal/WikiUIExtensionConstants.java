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
package org.xwiki.uiextension.internal;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * Constants for XClasses and XProperties.
 *
 * @version $Id$
 * @since 4.2M3
 */

public interface WikiUIExtensionConstants
{
    /**
     * Extension XClass reference.
     */
    EntityReference UI_EXTENSION_CLASS = new EntityReference(new EntityReference("UIExtensionClass",
        EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE)));

    /**
     * Extension Point ID property.
     */
    String EXTENSION_POINT_ID_PROPERTY = "extensionPointId";

    /**
     * Extension ID property.
     */
    String ID_PROPERTY = "name";

    /**
     * Extension content property.
     */
    String CONTENT_PROPERTY = "content";

    /**
     * Extension parameters property.
     */
    String PARAMETERS_PROPERTY = "parameters";
}
