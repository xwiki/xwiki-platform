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
package org.xwiki.officeimporter.converter;

import java.io.File;
import java.util.Set;

import org.xwiki.stability.Unstable;

/**
 * Provide the result of an office file conversion.
 * This interface mainly returns the paths of the various files created during the conversion, but it also allows to
 * cleanup all files created.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Unstable
public interface OfficeConverterResult
{
    /**
     * @return the output file of the conversion.
     */
    File getOutputFile();

    /**
     * @return the directory containing all output files.
     */
    File getOutputDirectory();

    /**
     * @return all files created during the conversion, including {@link #getOutputFile()}.
     */
    Set<File> getAllFiles();

    /**
     * Allows to completely delete {@link #getOutputDirectory()}.
     */
    void cleanup();
}
