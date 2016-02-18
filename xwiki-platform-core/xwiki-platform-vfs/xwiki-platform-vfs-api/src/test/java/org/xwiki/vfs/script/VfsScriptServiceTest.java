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
package org.xwiki.vfs.script;

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsManager;
import org.xwiki.vfs.VfsResourceReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VfsScriptService}.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class VfsScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<VfsScriptService> mocker =
        new MockitoComponentMockingRule<>(VfsScriptService.class);

    @Test
    public void url() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(
            URI.create("attach:xwiki:space.page@attachment"), "path1/path2/test.txt");

        VfsManager manager = this.mocker.getInstance(VfsManager.class);
        when(manager.getURL(reference)).thenReturn("/generated/url");

        assertEquals("/generated/url",
            this.mocker.getComponentUnderTest().url(
                new VfsResourceReference(URI.create("attach:xwiki:space.page@attachment"), "path1/path2/test.txt")));
    }

    @Test
    public void urlError() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(
            URI.create("attach:xwiki:space.page@attachment"), "path1/path2/test.txt");

        VfsManager manager = this.mocker.getInstance(VfsManager.class);
        when(manager.getURL(reference)).thenThrow(new VfsException("error"));

        assertNull(this.mocker.getComponentUnderTest().url(
            new VfsResourceReference(URI.create("attach:xwiki:space.page@attachment"), "path1/path2/test.txt")));
    }
}
