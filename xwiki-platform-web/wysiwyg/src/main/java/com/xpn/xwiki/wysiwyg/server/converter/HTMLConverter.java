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
package com.xpn.xwiki.wysiwyg.server.converter;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Converts HTML to/from a specific syntax.
 * 
 * @version $Id$
 */
@ComponentRole
public interface HTMLConverter
{
    /**
     * Converts to HTML the specified source text. The converter expects the source text to have a specific known
     * syntax.
     * 
     * @param source The text to be converted. Its syntax is priorly known by the converter.
     * @return The HTML result of the conversion.
     */
    String toHTML(String source);

    /**
     * Converts the specified HTML text to a specific syntax, priorly known by the converter.
     * 
     * @param html The HTML text to be converted.
     * @return The result on the conversion.
     */
    String fromHTML(String html);
}
