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
package org.xwiki.webdav.test;

import org.apache.jackrabbit.webdav.DavServletResponse;
import org.junit.After;
import org.junit.Test;

/**
 * Test case for nested temporary files in webdav interface.
 * 
 * @version $Id$
 * @since 1.8RC2
 */
public class TempFileTest extends AbstractWebDAVTest
{
    /**
     * Temporary working directory for all tests. 
     */
    public static final String TEMP_ROOT = ROOT + "/.temp";
    
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        mkCol(TEMP_ROOT, DavServletResponse.SC_CREATED);
    }

    /**
     * Test creating, moving and deleting a (temporary) collection resource under another temporary collection.
     */
    @Test
    public void testTempCollectionOperations() throws Exception
    {
        String tempCollectionUrl = TEMP_ROOT + "/temp";
        // Create.
        mkCol(tempCollectionUrl, DavServletResponse.SC_CREATED);
        // Rename (move): Not allowed.
        move(tempCollectionUrl, "/xwiki/webdav/.temp/temp2", DavServletResponse.SC_FORBIDDEN);
        // Delete.
        delete(tempCollectionUrl, DavServletResponse.SC_NO_CONTENT);
    }
    
    /**
     * Test creating, spooling, moving and deleting (temporary) files under a temporary collection.
     */
    @Test
    public void testTempFileOperations() throws Exception
    {
        String tempFileUrl = TEMP_ROOT + "/temp.txt";
        String destinationUrl = TEMP_ROOT + "/temp2.txt";
        String relativeDestinationUrl = "/xwiki/webdav/.temp/temp2.txt";
        // Create.
        put(tempFileUrl, "Content", DavServletResponse.SC_CREATED);
        // Spool.
        get(tempFileUrl, DavServletResponse.SC_OK);
        // Rename (move).
        move(tempFileUrl, relativeDestinationUrl, DavServletResponse.SC_CREATED);
        // Spool.
        get(destinationUrl, DavServletResponse.SC_OK);
        // Delete.
        delete(destinationUrl, DavServletResponse.SC_NO_CONTENT);
    }

    @After
    public void tearDown() throws Exception
    {
        delete(TEMP_ROOT, DavServletResponse.SC_NO_CONTENT);
    }    
}
