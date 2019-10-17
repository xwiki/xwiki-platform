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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.filesystem.FilesystemExportContext;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FilesystemExportContextProvider}.
 *
 * @version $Id$
 * @since 7.2M1
 */
@ComponentTest
public class FilesystemExportContextProviderTest
{
    @MockComponent
    private Execution execution;

    @InjectMockComponents
    private FilesystemExportContextProvider provider;

    private ExecutionContext ec = new ExecutionContext();

    @BeforeEach
    public void beforeEach()
    {
        when(this.execution.getContext()).thenReturn(ec);
    }

    @Test
    public void getWhenExportContextNotInEC() throws Exception
    {
        FilesystemExportContext fec = this.provider.get();
        assertSame(fec, ec.getProperty("filesystemExportContext"));
    }

    @Test
    public void getWhenExportContextAlreadyInEC() throws Exception
    {
        FilesystemExportContext fec = new FilesystemExportContext();
        ec.setProperty("filesystemExportContext", fec);

        FilesystemExportContext newfec = this.provider.get();
        assertSame(newfec, ec.getProperty("filesystemExportContext"));
        assertSame(newfec, fec);
    }

    @Test
    public void set() throws Exception
    {
        FilesystemExportContext fec = new FilesystemExportContext();
        FilesystemExportContextProvider.set(this.ec, fec);

        assertSame(fec, this.provider.get());

        FilesystemExportContext fec2 = new FilesystemExportContext();
        FilesystemExportContextProvider.set(this.ec, fec2);

        assertSame(fec2, this.provider.get());
    }
}
