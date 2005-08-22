/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 7 nov. 2004
 * Time: 16:46:12
 */
package com.xpn.xwiki.plugin.fileupload;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.plugin.calendar.CalendarPlugin;
import com.xpn.xwiki.plugin.calendar.CalendarParams;
import com.xpn.xwiki.plugin.calendar.CalendarData;
import com.xpn.xwiki.plugin.calendar.CalendarEvent;
import com.xpn.xwiki.plugin.laszlo.LaszloPlugin;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.text.SimpleDateFormat;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.DefaultFileItem;

public class FileUploadPluginApi extends PluginApi {

    public FileUploadPluginApi(FileUploadPlugin plugin, XWikiContext context) {
        super(plugin, context);
    }

    public FileUploadPlugin getFileUploadPlugin() {
        return (FileUploadPlugin) getPlugin();
    }

    /**
     *  Deletes all temporary files of the upload
     */
    public void cleanFileList() {
        getFileUploadPlugin().cleanFileList(context);
    }

    /**
     * Allows to load the file list in the context if there is a file upload
     * Default uploadMaxSize, uploadSizeThreashold and temporary directory are used
     * @throws XWikiException An XWikiException is thrown if the request could not be parser
     */
    public void loadFileList() throws XWikiException {
        getFileUploadPlugin().loadFileList(context);
    }

    /**
     * Allows to load the file list in the context if there is a file upload
     *
     * @param uploadMaxSize Maximum size of the request
     * @param uploadSizeThreashold  Threashold over which the data should be on disk and not in memory
     * @param tempdir Temporary Directory to store temp data
     * @throws XWikiException An XWikiException is thrown if the request could not be parser
     */
    public void loadFileList(long uploadMaxSize, int uploadSizeThreashold, String tempdir) throws XWikiException {
        getFileUploadPlugin().loadFileList(uploadMaxSize, uploadSizeThreashold, tempdir, context);
    }

    /**
     * Allows to retrieve the current FileItem list
     * loadFileList needs to be called beforehand
     * @return a list of FileItem elements
     */
    public List getFileItems() {
        return getFileUploadPlugin().getFileItems(context);
    }

    /**
     * Allows to retrieve the data of FileItem named name
     * loadFileList needs to be called beforehand

     * @param name Name of the item
     * @return byte[] of the data
     * @throws XWikiException Exception is thrown if the data could not be read
     */
    public byte[] getFileItemData(String name) throws XWikiException {
        return getFileUploadPlugin().getFileItemData(name, context);
    }

    /**
     * Allows to retrieve the data of FileItem named name
     * loadFileList needs to be called beforehand

     * @param name Name of the item
     * @return String of the data
     * @throws XWikiException Exception is thrown if the data could not be read
     */
    public String getFileItem(String name) throws XWikiException {
        return getFileUploadPlugin().getFileItem(name, context);
    }

    /**
     * Allows to retrieve the list of FileItem names
     * loadFileList needs to be called beforehand

     * @return List of strings of the item names
     */
    public List getFileItemNames() {
        return getFileUploadPlugin().getFileItemNames(context);
    }

    /**
     * Get file name from FileItem
     * @param name of the field
     * @return  The file name
     */
    public String getFileName(String name) {
        return getFileUploadPlugin().getFileName(name, context);
    }
}
