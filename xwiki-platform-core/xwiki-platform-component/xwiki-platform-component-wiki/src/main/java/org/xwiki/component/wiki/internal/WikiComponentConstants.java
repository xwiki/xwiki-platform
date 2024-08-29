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
package org.xwiki.component.wiki.internal;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Constants for XClasses and XProperties.
 *
 * @version $Id$
 * @since 4.2M3
 */
public interface WikiComponentConstants
{
    /**
     * Author of the XClass documents.
     */
    String CLASS_AUTHOR = XWikiRightService.SUPERADMIN_USER;

    /**
     * The name of the space where the wiki component xlcasses are located.
     * 
     * @since 11.4
     */
    String CLASS_SPACE_NAME = "XWiki";

    /**
     * Space of the XClass documents.
     */
    EntityReference CLASS_SPACE_REFERENCE = new EntityReference(CLASS_SPACE_NAME, EntityType.SPACE);

    /**
     * The XClass defining a component implementation.
     */
    String COMPONENT_CLASS_NAME = "ComponentClass";

    /**
     * The XClass defining a component implementation.
     */
    String COMPONENT_CLASS = CLASS_SPACE_NAME + '.' + COMPONENT_CLASS_NAME;

    /**
     * The XClass defining a component implementation.
     */
    LocalDocumentReference COMPONENT_CLASS_REFERENCE =
        new LocalDocumentReference(COMPONENT_CLASS_NAME, CLASS_SPACE_REFERENCE);

    /**
     * The name of the XClass defining a component injection.
     * 
     * @since 11.4
     */
    String DEPENDENCY_CLASS_NAME = "ComponentDependencyClass";

    /**
     * The XClass defining a component injection.
     */
    String DEPENDENCY_CLASS = CLASS_SPACE_NAME + '.' + DEPENDENCY_CLASS_NAME;

    /**
     * The XClass defining a component injection.
     */
    LocalDocumentReference DEPENDENCY_CLASS_REFERENCE =
        new LocalDocumentReference(DEPENDENCY_CLASS_NAME, CLASS_SPACE_REFERENCE);

    /**
     * The name of the XClass defining a component method.
     * 
     * @since 11.4
     */
    String METHOD_CLASS_NAME = "ComponentMethodClass";

    /**
     * The XClass defining a component method.
     */
    String METHOD_CLASS = CLASS_SPACE_NAME + '.' + METHOD_CLASS_NAME;

    /**
     * The XClass defining a component method.
     */
    LocalDocumentReference METHOD_CLASS_REFERENCE =
        new LocalDocumentReference(METHOD_CLASS_NAME, CLASS_SPACE_REFERENCE);

    /**
     * The name of the XClass defining a component interface implementation.
     * 
     * @since 11.4
     */
    String INTERFACE_CLASS_NAME = "ComponentInterfaceClass";

    /**
     * The XClass defining a component interface implementation.
     */
    String INTERFACE_CLASS = CLASS_SPACE_NAME + '.' + INTERFACE_CLASS_NAME;

    /**
     * The XClass defining a component interface implementation.
     */
    LocalDocumentReference INTERFACE_CLASS_REFERENCE =
        new LocalDocumentReference(INTERFACE_CLASS_NAME, CLASS_SPACE_REFERENCE);

    /**
     * The name property of the {@link #INTERFACE_CLASS} XClass.
     */
    String INTERFACE_NAME_FIELD = "name";

    /**
     * The name property of the {@link #METHOD_CLASS} XClass.
     */
    String METHOD_NAME_FIELD = INTERFACE_NAME_FIELD;

    /**
     * The code property of the {@link #METHOD_CLASS} XClass.
     */
    String METHOD_CODE_FIELD = "code";

    /**
     * The role hint property of both {@link #COMPONENT_CLASS} and {@link #DEPENDENCY_CLASS}.
     */
    String COMPONENT_ROLE_HINT_FIELD = "roleHint";

    /**
     * The role type property of both {@link #COMPONENT_CLASS} and {@link #DEPENDENCY_CLASS}.
     */
    String COMPONENT_ROLE_TYPE_FIELD = "roleType";

    /**
     * The role hint priority property of both {@link #COMPONENT_CLASS}.
     * 
     * @since 15.4RC1
     */
    String COMPONENT_ROLE_HINT_PRIORITY_FIELD = "roleHintPriority";

    /**
     * The role type priority property of both {@link #COMPONENT_CLASS}.
     * 
     * @since 15.4RC1
     */
    String COMPONENT_ROLE_TYPE_PRIORITY_FIELD = "roleTypePriority";

    /**
     * The scope of the {@link #COMPONENT_CLASS}.
     */
    String COMPONENT_SCOPE_FIELD = "scope";

    /**
     * The binding name property of {@link #DEPENDENCY_CLASS}.
     */
    String DEPENDENCY_BINDING_NAME_FIELD = "bindingName";
}
