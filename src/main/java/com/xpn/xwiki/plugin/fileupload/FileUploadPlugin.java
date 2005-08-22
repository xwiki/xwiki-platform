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
 * Original Code from: the JRoller Project
 * Date: 8 oct. 2004
 * Time: 15:04:30
 */
package com.xpn.xwiki.plugin.fileupload;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.DefaultFileItem;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;

public class FileUploadPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface {
    private static Log mLogger =
            LogFactory.getFactory().getInstance(FileUploadPlugin.class);

    private static final long UPLOAD_DEFAULT_MAXSIZE = 10000000L;
    private static final long UPLOAD_DEFAULT_SIZETHRESHOLD = 100000L;

    public FileUploadPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
    }

    /**
     *  Allow to get the plugin name
     * @return plugin name
     */
    public String getName() {
        return "fileupload";
    }

    public void init(XWikiContext context) {
    }

    public void virtualInit(XWikiContext context) {
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new FileUploadPluginApi((FileUploadPlugin) plugin, context);
    }
    
    /**
     *  endRendering to make sure we don't leave files in temp directories
     * @param context Context of the request
    */
    public void endRendering(XWikiContext context) {
        cleanFileList(context);
    }

    /**
     *  Deletes all temporary files of the upload
     * @param context Context of the request
     */
    public void cleanFileList(XWikiContext context) {
        List fileuploadlist = (List) context.get("fileuploadlist");
        if (fileuploadlist!=null) {
             for (int i=0;i<fileuploadlist.size();i++) {
                 try {
                     FileItem item = (FileItem) fileuploadlist.get(i);
                     item.delete();
                 } catch (Exception e) {
                 }
             }
            context.remove("fileuploadlist");
        }
    }

    /**
     * Allows to load the file list in the context if there is a file upload
     * Default uploadMaxSize, uploadSizeThreashold and temporary directory are used
     * @param context Context of the request
     * @throws XWikiException An XWikiException is thrown if the request could not be parser
     */
    public void loadFileList(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        loadFileList(xwiki.getXWikiPreferenceAsLong("upload_maxsize", UPLOAD_DEFAULT_MAXSIZE, context),
                (int)xwiki.getXWikiPreferenceAsLong("upload_sizethreshold", UPLOAD_DEFAULT_SIZETHRESHOLD, context),
                xwiki.Param("xwiki.upload.tempdir"), context);
    }

    /**
     * Allows to load the file list in the context if there is a file upload
     *
     * @param uploadMaxSize Maximum size of the request
     * @param uploadSizeThreashold  Threashold over which the data should be on disk and not in memory
     * @param tempdir Temporary Directory to store temp data
     * @param context Context of the request
     * @throws XWikiException An XWikiException is thrown if the request could not be parser
     */
    public void loadFileList(long uploadMaxSize, int uploadSizeThreashold, String tempdir, XWikiContext context) throws XWikiException {
        // Get the FileUpload Data
        DiskFileUpload fileupload = new DiskFileUpload();
        fileupload.setSizeMax(uploadMaxSize);
        fileupload.setSizeThreshold(uploadSizeThreashold);
        context.put("fileupload", fileupload);

        if (tempdir!=null) {
            fileupload.setRepositoryPath(tempdir);
            (new File(tempdir)).mkdirs();
        }
        else {
            fileupload.setRepositoryPath(".");
        }

        try {
            XWikiRequest request = context.getRequest();
            List list = fileupload.parseRequest(request.getHttpServletRequest());
            // We store the file list in the context
            context.put("fileuploadlist", list);
        } catch (FileUploadException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_UPLOAD_PARSE_EXCEPTION,
                    "Exception while parsing uploaded file", e);
        }
    }

    /**
     * Allows to retrieve the current FileItem list
     * loadFileList needs to be called beforehand
     * @param context Context of the request
     * @return a list of FileItem elements
     */
    public List getFileItems(XWikiContext context) {
        return (List) context.get("fileuploadlist");
    }

    /**
     * Allows to retrieve the data of FileItem named name
     * loadFileList needs to be called beforehand

     * @param name Name of the item
     * @param context Context of the request
     * @return byte[] of the data
     * @throws XWikiException Exception is thrown if the data could not be read
     */
    public byte[] getFileItemData(String name, XWikiContext context) throws XWikiException {
        List fileuploadlist = getFileItems(context);
        if (fileuploadlist==null) {
            return null;
        }

        DefaultFileItem  fileitem = null;
        for (int i=0;i<fileuploadlist.size();i++) {
            DefaultFileItem item = (DefaultFileItem) fileuploadlist.get(i);
            if (name.equals(item.getFieldName())) {
                fileitem = item;
                break;
            }
        }

        if (fileitem==null)
            return null;

        byte[] data = new byte[(int)fileitem.getSize()];
        InputStream fileis = null;
        try {
            fileis = fileitem.getInputStream();
            fileis.read(data);
            fileis.close();
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_UPLOAD_FILE_EXCEPTION,
                    "Exception while reading uploaded parsed file", e);
        }
        return data;
    }

    /**
     * Allows to retrieve the data of FileItem named name
     * loadFileList needs to be called beforehand

     * @param name Name of the item
     * @param context Context of the request
     * @return String of the data
     * @throws XWikiException Exception is thrown if the data could not be read
     * @param name
     * @param context
     * @return
     * @throws XWikiException
     */
    public String getFileItem(String name, XWikiContext context) throws XWikiException {
        byte[] data = getFileItemData(name, context);
        if (data==null)
            return null;
        else
            return new String(data);
    }

    /**
     * Allows to retrieve the list of FileItem names
     * loadFileList needs to be called beforehand

     * @param context Context of the request
     * @return List of strings of the item names
     */
    public List getFileItemNames(XWikiContext context) {
        List itemnames = new ArrayList();
        List fileuploadlist = getFileItems(context);
        if (fileuploadlist==null) {
            return itemnames;
        }

        for (int i=0;i<fileuploadlist.size();i++) {
            DefaultFileItem item = (DefaultFileItem) fileuploadlist.get(i);
            itemnames.add(item.getFieldName());
        }
        return itemnames;
    }

    /**
     * Get file name from FileItem
     * @param name of the field
     * @param context Context of the request
     * @return  The file name
     */
    public String getFileName(String name, XWikiContext context) {
        List fileuploadlist = getFileItems(context);
        if (fileuploadlist==null) {
            return null;
        }

        DefaultFileItem  fileitem = null;
        for (int i=0;i<fileuploadlist.size();i++) {
            DefaultFileItem item = (DefaultFileItem) fileuploadlist.get(i);
            if (name.equals(item.getFieldName())) {
                fileitem = item;
                break;
            }
        }

        if (fileitem==null)
            return null;

        return fileitem.getName();
    }

}
