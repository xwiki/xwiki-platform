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
package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import com.google.gwt.user.client.ui.FileUpload;

/**
 * IE implementation of the {@link FileNameExtractor}.
 * 
 * @version $Id$
 */
public class IEFileNameExtractor extends FileNameExtractor
{
    /**
     * {@inheritDoc}. Override default implementation because IE returns the whole path and we need to extract the
     * filename in it.
     */
    @Override
    public String getFileName(FileUpload fileUploadInput)
    {
        String path = fileUploadInput.getFilename();
        // extract the part after the last \ or :
        int backSlashIndex = path.lastIndexOf("\\");
        int columnIndex = path.lastIndexOf(":");
        return path.substring((backSlashIndex > columnIndex) ? backSlashIndex : columnIndex);
    }
}
