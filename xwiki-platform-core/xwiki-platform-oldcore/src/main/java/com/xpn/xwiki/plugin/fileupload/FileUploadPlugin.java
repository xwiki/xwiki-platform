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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidator;
import org.xwiki.environment.Environment;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.internal.fileupload.FileUploadUtils;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.web.Utils;

/**
 * Plugin that offers access to uploaded files. The uploaded files are automatically parsed and preserved as a list of
 * {@link FileItem}s.
 *
 * @version $Id$
 */
public class FileUploadPlugin extends XWikiDefaultPlugin
{
    /**
     * The name of the plugin; the key that can be used to retrieve this plugin from the context.
     *
     * @see XWikiPluginInterface#getName()
     */
    public static final String PLUGIN_NAME = "fileupload";

    /**
     * The context name of the uploaded file list. It can be used to retrieve the list of uploaded files from the
     * context. Note that the order of the list is not guaranteed and might depend of the servlet engine used.
     */
    public static final String FILE_LIST_KEY = "fileuploadlist";

    /**
     * The name of the parameter that can be set in the global XWiki preferences to override the default maximum file
     * size.
     */
    public static final String UPLOAD_MAXSIZE_PARAMETER = "upload_maxsize";

    /**
     * The default maximum size for uploaded documents. This limit can be changed using the {@code upload_maxsize}
     * XWiki preference.
     */
    public static final long UPLOAD_DEFAULT_MAXSIZE = 33554432L;

    /**
     * The name of the parameter that can be set in the global XWiki preferences to override the default size threshold
     * for on-disk storage.
     */
    public static final String UPLOAD_SIZETHRESHOLD_PARAMETER = "upload_sizethreshold";

    /**
     * Log object to log messages in this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadPlugin.class);

    /**
     * The default maximum size for in-memory stored uploaded documents. If a file is larger than this limit, it will be
     * stored on disk until the current request finishes. This limit can be changed using the
     * {@code upload_sizethreshold} XWiki preference.
     */
    private static final long UPLOAD_DEFAULT_SIZETHRESHOLD = 100000L;

    private Environment environment;

    private String temporaryDirectory;

    private AttachmentValidator attachmentValidator;

    /**
     * @param name the plugin name
     * @param className the plugin classname (used in logs for example)
     * @param context the XWiki Context
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public FileUploadPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
    }

    @Override
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new FileUploadPluginApi((FileUploadPlugin) plugin, context);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Make sure we don't leave files in temp directories and in memory.
     * </p>
     */
    @Override
    public void endRendering(XWikiContext context)
    {
        // we used to call cleanFileList here but we should not anymore as endRendering is called to
        // many times and empties the file upload list. This is handled by XWikiAction and
        // XWikiPortlet which clean up lists in a finally block
    }

    /**
     * Deletes all temporary files of the upload.
     *
     * @param context Context of the request.
     * @see FileUploadPluginApi#cleanFileList()
     */
    public void cleanFileList(XWikiContext context)
    {
        LOGGER.debug("Cleaning uploaded files");

        List<FileItem> fileuploadlist = getFileItems(context);
        if (fileuploadlist != null) {
            for (FileItem item : fileuploadlist) {
                try {
                    item.delete();
                } catch (Exception ex) {
                    LOGGER.warn("Exception cleaning uploaded files", ex);
                }
            }
            context.remove(FILE_LIST_KEY);
        }
    }

    /**
     * Loads the list of uploaded files in the context if there are any uploaded files.
     *
     * @param context Context of the request.
     * @throws XWikiException An XWikiException is thrown if the request could not be parsed.
     * @throws AttachmentValidationException in case of error when validating the attachment (e.g., the maximum
     *     filesize is reached)
     * @see FileUploadPluginApi#loadFileList()
     */
    public void loadFileList(XWikiContext context) throws XWikiException, AttachmentValidationException
    {
        XWiki xwiki = context.getWiki();
        loadFileList(
            xwiki.getSpacePreferenceAsLong(UPLOAD_MAXSIZE_PARAMETER, UPLOAD_DEFAULT_MAXSIZE, context),
            (int) xwiki.getSpacePreferenceAsLong(UPLOAD_SIZETHRESHOLD_PARAMETER, UPLOAD_DEFAULT_SIZETHRESHOLD, context),
            getTemporaryDirectory(xwiki), context);
    }

    /**
     * Loads the list of uploaded files in the context if there are any uploaded files.
     *
     * @param uploadMaxSize Maximum size of the uploaded files.
     * @param uploadSizeThreshold the threshold over which the file data should be stored on disk, and not in
     *     memory.
     * @param tempdir Temporary directory to store the uploaded files that are not kept in memory.
     * @param context Context of the request.
     * @throws XWikiException if the request could not be parsed, or the maximum file size was reached
     * @throws AttachmentValidationException in case of error when validating the attachment (e.g., the maximum
     *     filesize is reached)
     * @see FileUploadPluginApi#loadFileList(long, int, String)
     */
    public void loadFileList(long uploadMaxSize, int uploadSizeThreshold, String tempdir, XWikiContext context)
        throws XWikiException, AttachmentValidationException
    {
        LOGGER.debug("Loading uploaded files");

        // If we already have a file list then loadFileList was already called
        // Continuing would empty the list.. We need to stop.
        if (context.get(FILE_LIST_KEY) != null) {
            LOGGER.debug("Called loadFileList twice");

            return;
        }

        Collection<FileItem> fileItems =
            FileUploadUtils.getFileItems(uploadMaxSize, uploadSizeThreshold, tempdir, context.getRequest(),
                getAttachmentValidator());
        List<FileItem> items;
        if (fileItems instanceof List) {
            items = (List<FileItem>) fileItems;
        } else {
            items = new ArrayList<>(fileItems);
        }

        // We store the file list in the context
        context.put(FILE_LIST_KEY, items);
    }

    /**
     * Allows to retrieve the current list of uploaded files, as a list of {@link FileItem}s.
     * {@link #loadFileList(XWikiContext)} needs to be called beforehand. Note that the order of this list is not
     * guaranteed and might be different depending on the servlet engine used.
     *
     * @param context Context of the request.
     * @return A list of FileItem elements.
     * @see FileUploadPluginApi#getFileItems()
     */
    public List<FileItem> getFileItems(XWikiContext context)
    {
        return (List<FileItem>) context.get(FILE_LIST_KEY);
    }

    /**
     * Allows to retrieve the contents of an uploaded file as a sequence of bytes. {@link #loadFileList(XWikiContext)}
     * needs to be called beforehand.
     *
     * @param formfieldName The name of the form field.
     * @param context Context of the request.
     * @return The contents of the file.
     * @throws XWikiException if the data could not be read.
     * @see FileUploadPluginApi#getFileItemData(String)
     */
    public byte[] getFileItemData(String formfieldName, XWikiContext context) throws XWikiException
    {
        int size = getFileItemSize(formfieldName, context);

        if (size == 0) {
            return null;
        }

        byte[] data = new byte[size];

        try {
            InputStream fileis = getFileItemInputStream(formfieldName, context);
            if (fileis != null) {
                fileis.read(data);
                fileis.close();
            }
        } catch (java.lang.OutOfMemoryError e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_JAVA_HEAP_SPACE,
                "Java Heap Space, Out of memory exception", e);
        } catch (IOException ie) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_UPLOAD_FILE_EXCEPTION, "Exception while reading uploaded parsed file",
                ie);
        }

        return data;
    }

    /**
     * Allows to retrieve the contents of an uploaded file as a stream. {@link #loadFileList(XWikiContext)} needs to be
     * called beforehand.
     *
     * @param formfieldName The name of the form field.
     * @param context Context of the request.
     * @return a InputStream on the file content
     * @throws IOException if I/O problem occurs
     * @since 2.3M2
     */
    public InputStream getFileItemInputStream(String formfieldName, XWikiContext context) throws IOException
    {
        FileItem fileitem = getFile(formfieldName, context);

        if (fileitem == null) {
            return null;
        }

        return fileitem.getInputStream();
    }

    /**
     * Retrieve the size of a file content in byte. {@link #loadFileList(XWikiContext)} needs to be called beforehand.
     *
     * @param formfieldName The name of the form field.
     * @param context Context of the request.
     * @return the size of the file in byte
     * @since 2.3M2
     */
    public int getFileItemSize(String formfieldName, XWikiContext context)
    {
        FileItem fileitem = getFile(formfieldName, context);

        if (fileitem == null) {
            return 0;
        }

        return ((int) fileitem.getSize());
    }

    /**
     * Allows to retrieve the contents of an uploaded file as a string. {@link #loadFileList(XWikiContext)} needs to be
     * called beforehand.
     *
     * @param formfieldName The name of the form field.
     * @param context Context of the request.
     * @return The contents of the file.
     * @throws XWikiException if the data could not be read.
     * @see FileUploadPluginApi#getFileItemAsString(String)
     */
    public String getFileItemAsString(String formfieldName, XWikiContext context) throws XWikiException
    {
        byte[] data = getFileItemData(formfieldName, context);
        if (data == null) {
            return null;
        }

        return new String(data);
    }

    /**
     * Allows to retrieve the contents of an uploaded file as a string. {@link #loadFileList(XWikiContext)} needs to be
     * called beforehand.
     *
     * @deprecated not well named, use {@link #getFileItemAsString(String, com.xpn.xwiki.XWikiContext)}
     * @param formfieldName The name of the form field.
     * @param context Context of the request.
     * @return The contents of the file.
     * @throws XWikiException Exception is thrown if the data could not be read.
     * @see FileUploadPluginApi#getFileItemAsString(String)
     */
    @Deprecated
    public String getFileItem(String formfieldName, XWikiContext context) throws XWikiException
    {
        return getFileItemAsString(formfieldName, context);
    }

    /**
     * Retrieves the list of FileItem names. {@link #loadFileList(XWikiContext)} needs to be called beforehand.
     *
     * @param context Context of the request
     * @return List of strings of the item names
     */
    public List<String> getFileItemNames(XWikiContext context)
    {
        List<String> itemnames = new ArrayList<String>();
        List<FileItem> fileuploadlist = getFileItems(context);
        if (fileuploadlist == null) {
            return itemnames;
        }

        for (FileItem item : fileuploadlist) {
            itemnames.add(item.getFieldName());
        }

        return itemnames;
    }

    /**
     * Get the name of the file uploaded for a form field.
     *
     * @param formfieldName The name of the form field.
     * @param context Context of the request.
     * @return The file name, or {@code null} if no file was uploaded for that form field.
     */
    public String getFileName(String formfieldName, XWikiContext context)
    {
        FileItem fileitem = getFile(formfieldName, context);

        // We need to strip the file path. See http://commons.apache.org/fileupload/faq.html#whole-path-from-IE
        return (fileitem == null) ? null : FilenameUtils.getName(fileitem.getName());
    }

    /**
     * Return the FileItem corresponding to the file uploaded for a form field.
     *
     * @param formfieldName The name of the form field.
     * @param context Context of the request.
     * @return The corresponding FileItem, or {@code null} if no file was uploaded for that form field.
     */
    public FileItem getFile(String formfieldName, XWikiContext context)
    {
        LOGGER.debug("Searching file uploaded for field " + formfieldName);

        List<FileItem> fileuploadlist = getFileItems(context);
        if (fileuploadlist == null) {
            return null;
        }

        FileItem fileitem = null;
        for (FileItem item : fileuploadlist) {
            if (formfieldName.equals(item.getFieldName())) {
                fileitem = item;
                LOGGER.debug("Found uploaded file!");
                break;
            }
        }

        return fileitem;
    }

    private Environment getEnvironment()
    {
        if (this.environment == null) {
            this.environment = Utils.getComponent(Environment.class);
        }
        return this.environment;
    }

    private AttachmentValidator getAttachmentValidator()
    {
        if (this.attachmentValidator == null) {
            this.attachmentValidator = Utils.getComponent(AttachmentValidator.class);
        }
        return this.attachmentValidator;
    }

    private String getTemporaryDirectory(XWiki xwiki)
    {
        if (this.temporaryDirectory == null) {
            String tmpDirectory = xwiki.Param("xwiki.upload.tempdir");
            File tmpDirectoryFile;
            if (StringUtils.isEmpty(tmpDirectory)) {
                tmpDirectoryFile = new File(getEnvironment().getTemporaryDirectory(), "fileuploads");
            } else {
                tmpDirectoryFile = new File(tmpDirectory);
            }
            tmpDirectoryFile.mkdirs();
            this.temporaryDirectory = tmpDirectoryFile.getAbsolutePath();

        }
        return this.temporaryDirectory;
    }
}
