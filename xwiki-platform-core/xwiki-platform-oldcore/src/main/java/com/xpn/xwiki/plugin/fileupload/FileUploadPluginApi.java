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

package com.xpn.xwiki.plugin.fileupload;

import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.xwiki.attachment.validation.AttachmentValidationException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * Plugin that offers access to uploaded files. The uploaded files are automatically parsed and preserved as a list of
 * {@link org.apache.commons.fileupload.FileItem}s. This is the wrapper accessible from in-document scripts.
 *
 * @version $Id$
 */
public class FileUploadPluginApi extends PluginApi<FileUploadPlugin>
{

    /**
     * API constructor.
     *
     * @param plugin The wrapped plugin object.
     * @param context Context of the request.
     * @see PluginApi#PluginApi(com.xpn.xwiki.plugin.XWikiPluginInterface, XWikiContext)
     */
    public FileUploadPluginApi(FileUploadPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * Deletes all temporary files of the upload.
     */
    public void cleanFileList()
    {
        getProtectedPlugin().cleanFileList(getXWikiContext());
    }

    /**
     * Loads the list of uploaded files in the context if there are any uploaded files.
     *
     * @throws XWikiException if the request could not be parsed, or the maximum file size was reached
     * @throws AttachmentValidationException in case of error when validating the attachment (e.g., the maximum
     *     filesize is reached)
     */
    public void loadFileList() throws XWikiException, AttachmentValidationException
    {
        getProtectedPlugin().loadFileList(getXWikiContext());
    }

    /**
     * Loads the list of uploaded files in the context if there are any uploaded files.
     *
     * @param uploadMaxSize Maximum size of the uploaded files.
     * @param uploadSizeThreashold Threashold over which the file data should be stored on disk, and not in memory.
     * @param tempdir Temporary directory to store the uploaded files that are not kept in memory.
     * @throws XWikiException if the request could not be parsed, or the maximum file size was reached.
     * @throws AttachmentValidationException in case of error when validating the attachment (e.g., the maximum
     *     filesize is reached)
     */
    public void loadFileList(long uploadMaxSize, int uploadSizeThreashold, String tempdir)
        throws XWikiException, AttachmentValidationException
    {
        getProtectedPlugin().loadFileList(uploadMaxSize, uploadSizeThreashold, tempdir, getXWikiContext());
    }

    /**
     * Allows to retrieve the current list of uploaded files, as a list of {@link FileItem}s. {@link #loadFileList()}
     * needs to be called beforehand
     *
     * @return A list of FileItem elements.
     */
    public List<FileItem> getFileItems()
    {
        return getProtectedPlugin().getFileItems(getXWikiContext());
    }

    /**
     * Allows to retrieve the contents of an uploaded file as a sequence of bytes. {@link #loadFileList()} needs to be
     * called beforehand. This method returns the contents of the first matching FileItem with the formfieldName. If you
     * are dealing with multiple files with the same formfieldName you should use {@link #getFileItems()}
     *
     * @param formfieldName The name of the form field.
     * @return The contents of the file.
     * @throws XWikiException if the data could not be read.
     */
    public byte[] getFileItemData(String formfieldName) throws XWikiException
    {
        return getProtectedPlugin().getFileItemData(formfieldName, getXWikiContext());
    }

    /**
     * Allows to retrieve the contents of an uploaded file as a string. {@link #loadFileList()} needs to be called
     * beforehand. This method returns the contents of the first matching FileItem with the formfieldName. If you are
     * dealing with multiple files with the same form field name you should use {@link #getFileItems()}
     *
     * @deprecated not well named, use {@link #getFileItemAsString(String)}
     * @param formfieldName The name of the form field.
     * @return The contents of the file.
     * @throws XWikiException Exception is thrown if the data could not be read.
     */
    @Deprecated
    public String getFileItem(String formfieldName) throws XWikiException
    {
        return getProtectedPlugin().getFileItemAsString(formfieldName, getXWikiContext());
    }

    /**
     * Allows to retrieve the contents of an uploaded file as a string. {@link #loadFileList()} needs to be called
     * beforehand. This method returns the contents of the first matching FileItem with the formfieldName. If you are
     * dealing with multiple files with the same form field name you should use {@link #getFileItems()}
     *
     * @param formfieldName The name of the form field.
     * @return The contents of the file.
     * @throws XWikiException if the data could not be read.
     */
    public String getFileItemAsString(String formfieldName) throws XWikiException
    {
        return getProtectedPlugin().getFileItemAsString(formfieldName, getXWikiContext());
    }

    /**
     * Retrieves the list of FileItem names. {@link #loadFileList()} needs to be called beforehand.
     *
     * @return List of strings of the item names
     */
    public List<String> getFileItemNames()
    {
        return getProtectedPlugin().getFileItemNames(getXWikiContext());
    }

    /**
     * This method returns the name of the first matching file with the formfieldName. If you are dealing with multiple
     * files with the same form field name you should use {@link #getFileItemNames()}
     *
     * @param formfieldName The name of the form field.
     * @return The file name, or {@code null} if no file was uploaded for that form field.
     */
    public String getFileName(String formfieldName)
    {
        return getProtectedPlugin().getFileName(formfieldName, getXWikiContext());
    }
}
