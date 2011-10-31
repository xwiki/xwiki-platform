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
package com.xpn.xwiki.gwt.api.client;

import com.google.gwt.user.client.rpc.IsSerializable;


public class Attachment  implements IsSerializable {
    private boolean isImage;
    private long id;
    private int size;
    private String filename;
    private String author;
    private String versionName;
    private long attDate;
    private String mimeType;
    private String downloadUrl;

    /**
     *
     * @return the document Id of the attachment
     */
    public long getId() {
        return id;
    }


    /**
     *
     * @return the Attachment size
     */
    public int getFilesize() {
        return size;
    }

    /**
     *
     * @return the attachment name
     */
    public String getFilename() {
        return filename;
    }

    /**
     *
     * @return the login of the person who attach the file
     */
    public String getAuthor() {
        return author;
    }

    /**
     *
     * @return the last version number of the document
     */
    public String getVersion() {
        return versionName;
    }

        /**
     *
     * @return the date of the last uploaded version
     */
    public long getDate() {
        return attDate;
    }

    /**
     *
     * @return the mimetype of the attachment
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     *
     * @return true if it's an image
     */
    public boolean isImage() {
        return isImage;
    }


    public void setImage(boolean image) {
        isImage = image;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setFilesize(int size) {
        this.size = size;
    }


    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public void setAttDate(long attDate) {
        this.attDate = attDate;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
