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
package org.xwiki.export.pdf;

import java.io.IOException;
import java.io.InputStream;

import org.xwiki.component.annotation.Role;

/**
 * Generic interface to print some data as PDF.
 * 
 * @version $Id$
 * @param <T> the input data type
 * @since 14.4.2
 * @since 14.5
 */
@Role
public interface PDFPrinter<T>
{
    /**
     * Prints the specified data as PDF.
     * 
     * @param input the data to be printed as PDF
     * @return the PDF input stream
     */
    InputStream print(T input) throws IOException;

    /**
     * @return {@code true} if this PDF printer is ready to be used, {@code false} otherwise
     * @since 14.8
     */
    default boolean isAvailable()
    {
        return true;
    }
}
