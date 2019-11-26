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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.url.filesystem.FilesystemExportContext;

/**
 * Creates a new or returns an existing {@link FilesystemExportContext}. If a new one is created it's put in the
 * Execution Context and when it exists it's retrieved from the Execution Context.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Singleton
public class FilesystemExportContextProvider implements Provider<FilesystemExportContext>
{
    private static final String CONTEXT_KEY = "filesystemExportContext";

    @Inject
    private Execution execution;

    @Override
    public FilesystemExportContext get()
    {
        ExecutionContext ec = this.execution.getContext();
        FilesystemExportContext exportContext = (FilesystemExportContext) ec.getProperty(CONTEXT_KEY);
        if (exportContext == null) {
            exportContext = new FilesystemExportContext();
            newProperty(ec, exportContext);
        }
        return exportContext;
    }

    /**
     * @param ec the execution context in which to add the export context
     * @param exportContext the export context
     * @since 11.9RC1
     */
    public static void set(ExecutionContext ec, FilesystemExportContext exportContext)
    {
        FilesystemExportContext existingExportContext = (FilesystemExportContext) ec.getProperty(CONTEXT_KEY);
        if (existingExportContext != null) {
            // Remove the existing context to create a new property
            ec.removeProperty(CONTEXT_KEY);
        }

        newProperty(ec, exportContext);
    }

    private static void newProperty(ExecutionContext ec, FilesystemExportContext exportContext)
    {
        ec.newProperty(CONTEXT_KEY).inherited().initial(exportContext).declare();
    }
}
