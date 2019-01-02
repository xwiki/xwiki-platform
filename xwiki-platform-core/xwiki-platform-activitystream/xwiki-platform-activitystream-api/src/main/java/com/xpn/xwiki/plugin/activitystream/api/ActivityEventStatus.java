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
package com.xpn.xwiki.plugin.activitystream.api;

/**
 * Define a status for any couple of activity/entity.
 *
 * @version $Id$
 * @since 9.2RC1
 */
public interface ActivityEventStatus
{
    /**
     * @return the activity concerned by the status
     */
    ActivityEvent getActivityEvent();

    /**
     * @return the id of the entity (a user or a group) concerned by the status
     */
    String getEntityId();

    /**
     * @return either or nor the activity has been read by the entity
     */
    boolean isRead();
}
