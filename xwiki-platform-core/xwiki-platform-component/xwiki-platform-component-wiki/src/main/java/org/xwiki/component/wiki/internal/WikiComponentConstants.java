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
     * The XClass defining a component implementation.
     */
    String COMPONENT_CLASS = "XWiki.ComponentClass";

    /**
     * The XClass defining a component injection.
     */
    String DEPENDENCY_CLASS = "XWiki.ComponentDependencyClass";

    /**
     * The XClass defining a component method.
     */
    String METHOD_CLASS = "XWiki.ComponentMethodClass";

    /**
     * The XClass defining a component interface implementation.
     */
    String INTERFACE_CLASS = "XWiki.ComponentInterfaceClass";

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
     * The scope of the {@link #COMPONENT_CLASS}.
     */
    String COMPONENT_SCOPE_FIELD = "scope";

    /**
     * The binding name property of {@link #DEPENDENCY_CLASS}.
     */
    String DEPENDENCY_BINDING_NAME_FIELD = "bindingName";
}
