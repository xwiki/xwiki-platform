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
package com.xpn.xwiki.internal.pdf;

import java.io.InputStream;
import java.io.OutputStream;

import org.xwiki.component.annotation.Role;

/**
 * Renders XSL-FO to supported output formats.
 * 
 * @version $Id$
 * @since 9.11
 */
@Role
public interface XSLFORenderer
{
    /**
     * Renders the XSL-FO from the input stream to the specified output format.
     * 
     * @param input the XSL-FO input
     * @param output where to write the output
     * @param outputFormat the output format
     * @throws Exception if XSL-FO rendering fails
     */
    void render(InputStream input, OutputStream output, String outputFormat) throws Exception;
}
