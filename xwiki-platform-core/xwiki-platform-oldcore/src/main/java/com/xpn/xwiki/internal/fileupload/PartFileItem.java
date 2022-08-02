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
package com.xpn.xwiki.internal.fileupload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.http.Part;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;

/**
 * Implement a {@link FileItem} based on a {@link Part}.
 * 
 * @version $Id$
 * @since 13.0
 */
public class PartFileItem implements FileItem
{
    private final Part part;

    private final FileItemHeaders headers;

    /**
     * @param part the servlet part
     */
    public PartFileItem(Part part)
    {
        this.part = part;
        this.headers = new PartFileItemHeaders(part);
    }

    @Override
    public FileItemHeaders getHeaders()
    {
        return this.headers;
    }

    @Override
    public void setHeaders(FileItemHeaders headers)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        return this.part.getInputStream();
    }

    @Override
    public String getContentType()
    {
        return this.part.getContentType();
    }

    @Override
    public String getName()
    {
        return this.part.getSubmittedFileName();
    }

    @Override
    public boolean isInMemory()
    {
        return false;
    }

    @Override
    public long getSize()
    {
        return this.part.getSize();
    }

    @Override
    public byte[] get()
    {
        byte[] fileData = new byte[(int) getSize()];

        try (InputStream is = this.part.getInputStream()) {
            IOUtils.readFully(is, fileData);
        } catch (IOException e) {
            fileData = null;
        }

        return fileData;
    }

    @Override
    public String getString(String encoding) throws UnsupportedEncodingException
    {
        try (InputStream is = getInputStream()) {
            return IOUtils.toString(is, encoding);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getString()
    {
        String encoding = getEncoding();
        if (encoding == null) {
            encoding = DiskFileItem.DEFAULT_CHARSET;
        }

        // Try with the encoding
        try {
            return getString(encoding);
        } catch (UnsupportedEncodingException e) {
            // Fallback
        }

        // Fallback on whatever is the default system encoding
        try (InputStream is = getInputStream()) {
            return new String(IOUtils.toByteArray(is));
        } catch (IOException e) {
            return null;
        }
    }

    private String getEncoding()
    {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        // Parameter parser can handle null input
        Map<String, String> params = parser.parse(getContentType(), ';');
        return params.get("charset");
    }

    @Override
    public void write(File file) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete()
    {
        try {
            this.part.delete();
        } catch (IOException e) {
            // TODO: log something ?
        }
    }

    @Override
    public String getFieldName()
    {
        return this.part.getName();
    }

    @Override
    public void setFieldName(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFormField()
    {
        // Assume files have a file name and form fields don't
        return this.part.getSubmittedFileName() == null;
    }

    @Override
    public void setFormField(boolean state)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        throw new UnsupportedOperationException();
    }
}
