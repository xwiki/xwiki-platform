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
package org.xwiki.mentions;

/**
 * List of the different display style of the user mentions.
 *
 * @version $Id$
 * @since 12.5RC1
 */
public enum DisplayStyle
{
    /**
     * Displays the first and last name of the mentioned user.
     */
    FULL_NAME,

    /**
     * Displays only the first name of the mentioned user.
     */
    FIRST_NAME,

    /**
     * Displays the login of the mentioned user.
     */
    LOGIN;

    /**
     * @param style the display style (e.g., login)
     * @return the result of {@link DisplayStyle#valueOf(String)} or {@link #FULL_NAME} if style is {@code null}
     * @since 13.10.7
     * @since 14.4.2
     * @since 14.5
     */
    public static DisplayStyle getOrDefault(String style)
    {
        if (style == null) {
            return FULL_NAME;
        }
        return DisplayStyle.valueOf(style);
    }
}
