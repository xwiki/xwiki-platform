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
package org.xwiki.query.internal;

/**
 * Represents characters for which no escaping will be performed, i.e. they'll behave as they would behave in a SQL
 * like clause as is (for example {@code %} will mean any char, etc). This is interesting when you don't control the
 * input and you wish special LIKE characters to keep their meanings but you still wish that the SQL escape character
 * used be defined (and not default to {@code \} for MySQL for example).
 *
 * @version $Id$
 * @since 8.4.5
 * @since 9.3RC1
 */
public class LikeParameterPart extends ParameterPart
{
    /**
     * @param part the literals that will be escaped
     */
    public LikeParameterPart(String part)
    {
        super(part);
    }
}
