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
 * The priorities possible for an Activity Event. This allows to filter complex events by only
 * keeping the most high level event or by getting page level event.
 */
public interface ActivityEventPriority
{
    /**
     * The event is a storage level event (low level)
     */
    int STORAGE = 10;

    /**
     * The event is a notification level event (low level)
     */
    int NOTIFICATION = 20;

    /**
     * The event is a action level event (high level)
     */
    int ACTION = 30;

    /**
     * The event is a program level event (highest level)
     */
    int PROGRAM = 40;
}
