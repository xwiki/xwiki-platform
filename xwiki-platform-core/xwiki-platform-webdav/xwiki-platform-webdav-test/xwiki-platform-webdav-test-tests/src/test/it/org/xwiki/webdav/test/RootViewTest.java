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
import org.junit.Test;

/**
 * Test case for webdav root view.
 * 
 * @version $Id$
 * @since 1.8RC2
 */
public class RootViewTest extends AbstractWebDAVTest
{
    /**
     * Test PROPFIND request on webdav root.
     */
    @Test
    public void testPropFind() throws Exception
    {
        propFind(ROOT, 1, DavServletResponse.SC_MULTI_STATUS);
    }

    /**
     * Test creating a collection resource (directory) under webdav root.
     */
    @Test
    public void testCreateCollection() throws Exception
    {
        mkCol(ROOT + "/collection", DavServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Test creating an ordinary resource (file) under webdav root.
     */
    @Test
    public void testCreateFile() throws Exception
    {
        put(ROOT + "/test.txt", "Content", DavServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Test creating, moving and deleting a temporary collection resource under webdav root.
     */
    @Test
    public void testTempCollectionOperations() throws Exception
    {
        String tempCollectionUrl = ROOT + "/.temp";
        // Create.
        mkCol(tempCollectionUrl, DavServletResponse.SC_CREATED);
        // Invalid rename (move).
        move(tempCollectionUrl, "/xwiki/webdav/temp2", DavServletResponse.SC_METHOD_NOT_ALLOWED);
        // Valid rename (move): renaming temporary collections is not allowed at the moment.
        move(tempCollectionUrl, "/xwiki/webdav/.temp2", DavServletResponse.SC_FORBIDDEN);
        // Delete.
        delete(tempCollectionUrl, DavServletResponse.SC_NO_CONTENT);
    }

    /**
     * Test creating, moving and deleting a temporary (file) resource under webdav root.
     */
    @Test
    public void testTempFileOperations() throws Exception
    {
        String tempFileUrl = ROOT + "/temp.txt~";
        String destinationUrl = ROOT + "/.temp.txt";
        String relativeDestinationUrl = "/xwiki/webdav/.temp.txt";
        // Create.
        put(tempFileUrl, "Content", DavServletResponse.SC_CREATED);
        // Spool.
        get(tempFileUrl, DavServletResponse.SC_OK);
        // Invalid rename (move).
        move(tempFileUrl, "/xwiki/webdav/temp.txt", DavServletResponse.SC_METHOD_NOT_ALLOWED);
        // Valid rename (move).
        move(tempFileUrl, relativeDestinationUrl, DavServletResponse.SC_CREATED);
        // Spool.
        get(destinationUrl, DavServletResponse.SC_OK);
        // Delete.
        delete(destinationUrl, DavServletResponse.SC_NO_CONTENT);
    }

    /**
     * Test renaming each of base views.
     */
    @Test
    public void testMoveBaseViews() throws Exception
    {
        String invalidDestination = "/xwiki/webdav/target";
        String validDestination = "/xwiki/webdav/.temp";
        for (String baseView : BASE_VIEWS) {
            move(baseView, invalidDestination, DavServletResponse.SC_METHOD_NOT_ALLOWED);
            move(baseView, validDestination, DavServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }
    
    /**
     * Test deleting each of base views.
     */
    @Test
    public void testDeleteBaseViews() throws Exception
    {
        for (String baseView : BASE_VIEWS) {
            delete(baseView, DavServletResponse.SC_FORBIDDEN);
        }
    }
}
