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
package org.xwiki.test.docker.junit5;

import java.io.File;
import java.io.IOException;

import org.xwiki.test.docker.internal.junit5.servletengine.ServletContainerExecutor;

/**
 * Allows functional tests to exchange files with the running XWiki instance regardless of whether it runs on the host
 * (e.g. Jetty standalone) or inside a Docker container that can't see the test runner's filesystem. This is typically
 * used to provide input files to a server-side operation (e.g. a {@code file:} filter conversion source) and to read
 * back the files it produces (e.g. a {@code file:} conversion target).
 * <p>
 * An instance is injected as a test method parameter by the {@code @UITest} extension.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
public class ServletEngineFileManager
{
    private final ServletContainerExecutor executor;

    /**
     * @param executor the executor of the servlet engine hosting the tested XWiki instance
     */
    public ServletEngineFileManager(ServletContainerExecutor executor)
    {
        this.executor = executor;
    }

    /**
     * Make the content of the passed host file readable by the running XWiki instance.
     *
     * @param hostFile the file located on the host (e.g. a test resource)
     * @return the path, as seen by the running XWiki instance, where the file content is now available (e.g. to be
     *         used in a {@code file:} filter source reference)
     * @throws IOException if the file can't be made available to the server
     */
    public String copyFileToServer(File hostFile) throws IOException
    {
        return this.executor.copyFileToServer(hostFile);
    }

    /**
     * @param fileName the name of a file
     * @return the path, as seen by the running XWiki instance, of a file with the given name located in the directory
     *         used to exchange files with XWiki; use it e.g. as the target of a server-side operation before reading
     *         the result back with {@link #copyFileFromServer(String)}
     * @throws IOException if the exchange directory can't be created
     */
    public String getServerFilePath(String fileName) throws IOException
    {
        return this.executor.getServerFilePath(fileName);
    }

    /**
     * Retrieve to the host a file produced by the running XWiki instance.
     *
     * @param serverPath the path of the file as seen by the running XWiki instance (typically obtained from
     *            {@link #getServerFilePath(String)})
     * @return the file on the host, ready to be read by the test
     * @throws IOException if the file can't be retrieved from the server
     */
    public File copyFileFromServer(String serverPath) throws IOException
    {
        return this.executor.copyFileFromServer(serverPath);
    }
}
