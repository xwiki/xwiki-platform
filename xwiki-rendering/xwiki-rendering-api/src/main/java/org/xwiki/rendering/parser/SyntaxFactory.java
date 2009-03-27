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
package org.xwiki.rendering.parser;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;

/**
 * @version $Id$
 * @since 1.5M2
 */
@ComponentRole
public interface SyntaxFactory
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = SyntaxFactory.class.getName();

    Syntax createSyntaxFromIdString(String syntaxAsIdString) throws ParseException;

    /**
     * @return a list of all Syntaxes for which there's a {@link Parser} available
     */
    List<Syntax> getAvailableSyntaxes();
}
