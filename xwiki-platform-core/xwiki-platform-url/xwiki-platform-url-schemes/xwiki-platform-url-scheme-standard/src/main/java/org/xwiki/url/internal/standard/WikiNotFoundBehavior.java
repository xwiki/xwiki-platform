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
package org.xwiki.url.internal.standard;

/**
 * Represents the action to take when a subwiki is not found (ie there's no wiki descriptor for it). Valid actions
 * are for example:
 * <ul>
 *   <li>default to displaying the main wiki</li>
 *   <li>redirect to a vm to display an error</li>
 * </ul>
 *
 * @version $Id$
 * @since 5.1M1
 */
public enum WikiNotFoundBehavior
{
    /**
     * Default to the main wiki.
     */
    REDIRECT_TO_MAIN_WIKI,

    /**
     * Display an error page.
     */
    DISPLAY_ERROR;
}
