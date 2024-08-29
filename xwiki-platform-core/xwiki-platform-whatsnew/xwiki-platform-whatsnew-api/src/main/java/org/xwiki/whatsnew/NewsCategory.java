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
package org.xwiki.whatsnew;

import org.xwiki.stability.Unstable;

/**
 * The different categories of news we support.
 *
 * @version $Id$
 * @since 15.1RC1
 */
@Unstable
public enum NewsCategory
{
    /**
     * The unknown category.
     */
    UNKNOWN,

    /**
     * News for admin users.
     */
    ADMIN_USER,

    /**
     * News for simple users (vs advanced users).
     */
    SIMPLE_USER,

    /**
     * News for advanced users (e.g. scripting news, etc).
     */
    ADVANCED_USER,

    /**
     * News about XWiki Extensions.
     */
    EXTENSION
}
