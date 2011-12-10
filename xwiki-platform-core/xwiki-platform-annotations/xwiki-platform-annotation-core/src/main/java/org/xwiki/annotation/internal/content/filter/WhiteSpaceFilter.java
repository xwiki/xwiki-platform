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
package org.xwiki.annotation.internal.content.filter;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.annotation.content.filter.Filter;
import org.xwiki.component.annotation.Component;

/**
 * Filters white spaces out of a text.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Named("whitespace")
@Singleton
public class WhiteSpaceFilter implements Filter
{
    @Override
    public boolean accept(Character c)
    {
        // check it not to be a whitespace but also not a space (should cover all whitespaces + unbreakable spaces)
        return !Character.isWhitespace(c) && !Character.isSpaceChar(c);
    }
}
