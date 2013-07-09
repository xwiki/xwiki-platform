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
package org.xwiki.wikistream.utils;

import org.xwiki.wikistream.input.source.InputSource;
import org.xwiki.wikistream.output.target.OutputTarget;

public interface WikiStreamConstants
{
    // Standard properties

    /**
     * The standard name of the input stream property containing the {@link InputSource} to read.
     */
    String PROPERTY_SOURCE = "source";

    /**
     * The standard name of the output stream property containing the {@link OutputTarget} to write to.
     */
    String PROPERTY_TARGET = "target";

    /**
     * The standard name of the output stream property containing the encoding to use to convert to/from byte array and
     * {@link String}.
     */
    String PROPERTY_ENCODING = "encoding";

    /**
     * The standard name of the output stream property indicating if the output should be formatted (for example in a
     * XML syntax indent and organize elements in a human friendly way).
     */
    String PROPERTY_FORMAT = "format";
}
