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
package org.xwiki.export.pdf.internal;

import java.io.InputStream;
import java.net.URL;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.export.pdf.PDFPrinter;

/**
 * Prints the content of a given URL using a headless Chrome web browser running inside a Docker container.
 * 
 * @version $Id$
 * @since 14.4.1
 * @since 14.5RC1
 */
@Component
@Singleton
@Named("docker")
public class DockerURL2PDFPrinter implements PDFPrinter<URL>
{
    @Override
    public InputStream print(URL input)
    {
        // TODO
        return null;
    }
}
