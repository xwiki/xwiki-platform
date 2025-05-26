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
import java.util.function.BooleanSupplier;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

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
     * @throws IOException if an error occurs while printing the data
     */
    InputStream print(T input) throws IOException;

    /**
     * Prints the specified data as PDF. The process can be canceled using the provided supplier.
     * 
     * @param input the data to be printed as PDF
     * @param isCanceled a supplier that indicates whether the process should be canceled
     * @return the PDF input stream
     * @throws IOException if an error occurs while printing the data
     * @since 16.10.8
     * @since 17.4.0RC1
     */
    @Unstable
    default InputStream print(T input, BooleanSupplier isCanceled) throws IOException
    {
        return print(input);
    }

    /**
     * @return {@code true} if this PDF printer is ready to be used, {@code false} otherwise
     * @since 14.8
     */
    default boolean isAvailable()
    {
        return true;
    }
}
