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
package org.xwiki.plugin.activitystream.api;

/**
 * The type of request. This list is extensible.
 */
public interface ActivityEventType
{
    String OTHER = "other";

    String CREATE = "create";

    String UPDATE = "update";

    String DELETE = "delete";

    String MOVE = "move";

    String CREATE_COMMENT = "createcomment";

    String CREATE_ATTACHMENT = "createattachment";

    String UPDATE_ATTACHMENT = "updateattachment";

    String DELETE_ATTACHMENT = "deleteattachment";

    String CREATE_USER = "createuser";

    String DELETE_USER = "deleteuser";

    String CREATE_SPACE = "createspace";

    String DELETE_SPACE = "deletespace";

    String CHANGE_RIGHTS = "changerights";

    String NEW_MEMBER = "newmember";
}
