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
 * File name extractor to use for extracting the name of the file in a {@link FileUpload}, to be implemented browser
 * specific.
 * 
 * @version $Id$
 */
public class FileNameExtractor
{    
    /**
     * Returns the currently set value of the passed {@link FileUpload}.
     * 
     * @param fileUploadInput the file input to obtain the file name for
     * @return the file name
     */
    public String getFileName(FileUpload fileUploadInput)
    {
        return fileUploadInput.getFilename(); 
    }
}
