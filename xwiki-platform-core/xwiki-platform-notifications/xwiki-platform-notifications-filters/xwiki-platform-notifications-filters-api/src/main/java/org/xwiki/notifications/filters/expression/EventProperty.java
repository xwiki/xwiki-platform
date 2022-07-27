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
package org.xwiki.notifications.filters.expression;

import org.xwiki.stability.Unstable;

/**
 * The several properties you can have in an {@link org.xwiki.eventstream.Event}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public enum EventProperty
{
    /**
     * The ID of the event.
     */
    ID,
    /**
     * The group ID of the event.
     */
    GROUP_ID,
    /**
     * The stream the event belongs to.
     */
    STREAM,
    /**
     * The date of the event.
     */
    DATE,
    /**
     * The importance of the event.
     */
    IMPORTANCE,
    /**
     * The type of the event.
     */
    TYPE,
    /**
     * The application the event belongs to.
     */
    APPLICATION,
    /**
     * The user who created the event.
     */
    USER,
    /**
     * The wiki concerned by the event.
     */
    WIKI,
    /**
     * The space concerned by the event.
     */
    SPACE,
    /**
     * The page concerned by the event.
     */
    PAGE,
    /**
     * Either of the event is hidden or not.
     */
    HIDDEN,
    /**
     * The URL of the page concerned by the event.
     */
    URL,
    /**
     * The title of the page concerned by the event.
     */
    TITLE,
    /**
     * The content of the event.
     */
    BODY,
    /**
     * The version of the page concerned by the event.
     */
    DOCUMENT_VERSION,
    /**
     * The unique identifier of the instance in the cluster.
     * 
     * @since 14.7RC1
     */
    @Unstable
    REMOTE_OBSERVATION_ID
}
