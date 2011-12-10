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
package org.xwiki.extension.repository.internal;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.extension.InvalidExtensionException;

/**
 * Local repository storage serialization tool.
 * 
 * @version $Id$
 */
@ComponentRole
public interface ExtensionSerializer
{
    /**
     * Load local extension descriptor.
     * 
     * @param repository the repository
     * @param descriptor the descriptor content
     * @return the parsed local extension descriptor
     * @throws InvalidExtensionException error when trying to parse extension descriptor
     */
    DefaultLocalExtension loadDescriptor(DefaultLocalExtensionRepository repository, InputStream descriptor)
        throws InvalidExtensionException;

    /**
     * Save local extension descriptor.
     * 
     * @param extension the extension to save
     * @param fos the stream where to write the serialized version of the extension descriptor
     * @throws ParserConfigurationException error when serializing
     * @throws TransformerException error when serializing
     */
    void saveDescriptor(DefaultLocalExtension extension, OutputStream fos) throws ParserConfigurationException,
        TransformerException;
}
