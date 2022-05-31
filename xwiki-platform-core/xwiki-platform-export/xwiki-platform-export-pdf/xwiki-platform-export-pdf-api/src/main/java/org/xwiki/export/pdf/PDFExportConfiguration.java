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
package org.xwiki.export.pdf;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * PDF export configuration options.
 * 
 * @version $Id$
 * @since 14.4.1
 * @since 14.5RC1
 */
@Role
@Unstable
public interface PDFExportConfiguration
{
    /**
     * @return the Docker image used to create the Docker container running the headless Chrome web browser
     */
    String getChromeDockerImage();

    /**
     * @return the name of the Docker container running the headless Chrome web browser used to print web pages to PDF
     */
    String getChromeDockerContainerName();

    /**
     * @return the domain used to access the host running the Docker container, from within the Docker container (i.e.
     *         by the headless Chrome web browser)
     */
    String getChromeDockerHostName();

    /**
     * @return the port number used for communicating with the headless Chrome web browser
     */
    int getChromeRemoteDebuggingPort();
}
