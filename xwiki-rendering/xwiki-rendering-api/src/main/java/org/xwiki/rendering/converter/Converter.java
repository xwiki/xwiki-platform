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
package org.xwiki.rendering.converter;

import java.io.Reader;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Convert source content in a given Syntax to another Syntax.
 *
 * @version $Id$
 * @since 2.0M3
 */
@ComponentRole
public interface Converter
{
    /**
     * Converts content from a Syntax to another and execute all registered Macro Transformations on the parsed
     * content.
     * 
     * @param source the content to be converted
     * @param sourceSyntax the Syntax in which the content is represented
     * @param targetSyntax the Syntax to which to convert to
     * @param printer the printer that will receive the result of the conversion
     * @throws ConversionException in case of a conversion error (invalid Syntax, etc)
     */
    void convert(Reader source, Syntax sourceSyntax, Syntax targetSyntax, WikiPrinter printer)
        throws ConversionException;
}
