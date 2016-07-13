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
package org.xwiki.url.internal.filesystem;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.filesystem.FilesystemExportContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FilesystemExportContextProvider}.
 *
 * @version $Id$
 * @since 7.2M1
 */
public class FilesystemExportContextProviderTest
{
    @Rule
    public MockitoComponentMockingRule<FilesystemExportContextProvider> mocker =
        new MockitoComponentMockingRule<>(FilesystemExportContextProvider.class);

    @Test
    public void getWhenExportContextNotInEC() throws Exception
    {
        ExecutionContext ec = new ExecutionContext();
        Execution execution = this.mocker.getInstance(Execution.class);
        when(execution.getContext()).thenReturn(ec);

        FilesystemExportContext fec = this.mocker.getComponentUnderTest().get();
        assertSame(fec, ec.getProperty("filesystemExportContext"));
    }

    @Test
    public void getWhenExportContextAlreadyInEC() throws Exception
    {
        ExecutionContext ec = new ExecutionContext();
        FilesystemExportContext fec = new FilesystemExportContext();
        ec.setProperty("filesystemExportContext", fec);
        Execution execution = this.mocker.getInstance(Execution.class);
        when(execution.getContext()).thenReturn(ec);

        FilesystemExportContext newfec = this.mocker.getComponentUnderTest().get();
        assertSame(newfec, ec.getProperty("filesystemExportContext"));
        assertSame(newfec, fec);
    }
}
