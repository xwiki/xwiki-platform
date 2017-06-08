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

package org.xwiki.test.escaping.suite;

import java.io.Reader;


/**
 * Defines a file test that can be run by the {@link ArchiveSuite}.
 * <p>
 * {@link ArchiveSuite} reads files from an archive and generates a {@link FileTest} for each of them.
 * The implementations can decide whether the given file can be tested and how it should be tested.</p>
 * <p>
 * All test implementations should have one default constructor. The initialization method
 * {@link #initialize(String, Reader)} is guaranteed to be called only once, before any other public methods
 * are called.</p>
 * <p>
 * Note that the {@link Reader} passed to {@link #initialize(String, Reader)} is invalidated right after the
 * initialization phase.</p>
 * 
 * @version $Id$
 * @since 2.5M1
 */
public interface FileTest
{
    /**
     * Initialize the test. If this method returns false, the test is not run at all (is not counted
     * as a success or failure).
     * 
     * @param name file name to use
     * @param reader the reader associated with the file data, should not be used after initialization
     * @return true if the test was initialized successfully and should be executed, false otherwise
     */
    boolean initialize(String name, final Reader reader);
}

