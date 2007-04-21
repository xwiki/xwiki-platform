/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.xmlrpc;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Represents an Attachment as described in the <a href="Confluence specification">
 * http://confluence.atlassian.com/display/DOC/Remote+API+Specification</a>.
 * 
 * @todo We're missing the Comment field as described in the Confluence specification.
 * @version $Id: $
 */
public class Attachment
{
    /**
     * @see #getId()
     */
    private static final String ID = "id";

    /**
     * @see #getPageId()
     */
    private static final String PAGE_ID = "pageId";

    /**
     * @see #getTitle()
     */
    private static final String TITLE = "title";

    /**
     * @see #getFileName()
     */
    private static final String FILE_NAME = "fileName";

    /**
     * @see #getFileSize()
     */
    private static final String FILE_SIZE = "fileSize";

    /**
     * @see #getContentType()
     */
    private static final String CONTENT_TYPE = "contentType";

    /**
     * @see #getCreated()
     */
    private static final String CREATED = "created";

    /**
     * @see #getCreator()
     */
    private static final String CREATOR = "creator";

    /**
     * @see #getUrl()
     */
    private static final String URL = "url";

    /**
     * @see #getId()
     */
    private String id;

    /**
     * @see #getPageId()
     */
    private String pageId;

    /**
     * @see #getTitle()
     */
    private String title;

    /**
     * @see #getFileName()
     */
    private String fileName;

    /**
     * @see #getFileSize()
     */
    private String fileSize;

    /**
     * @see #getContentType()
     */
    private String contentType;

    /**
     * @see #getCreated()
     */
    private Date created;

    /**
     * @see #getCreator()
     */
    private String creator;

    /**
     * @see #getUrl()
     */
    private String url;

    /**
     * @param doc the (@link com.xpn.xwiki.XWikiDocument) object, used to create the Attachment
     *            object. The reason we need its that some information for creating that object is
     *            available only from the XWikiDocument object and not in the passed XWikiAttachment
     *            object
     * @param attachment the (@link com.xpn.xwiki.XWikiAttachment) object, used to create the
     *            Attachment object
     * @param context the {@link com.xpn.xwiki.XWikiContext} object, used to get access to XWiki
     *            primitives for loading documents
     */
    public Attachment(XWikiDocument doc, XWikiAttachment attachment, XWikiContext context)
    {
        setId(attachment.getFilename());
        setPageId(doc.getFullName());
        setTitle(attachment.getComment());
        setFileName(attachment.getFilename());
        setFileSize("" + attachment.getFilesize());
        setCreator(attachment.getAuthor());
        setCreated(attachment.getDate());
        setContentType(context.getWiki().getEngineContext().getMimeType(attachment.getFilename()));
        setUrl(doc.getAttachmentURL(attachment.getFilename(), "download", context));
    }

    /**
     * @return the Attachment object represented by a Map. The Map keys are the XML-RPC ids and the
     *         values are the property values. This map will be used to build a XML-RPC message.
     */
    Map getParameters()
    {
        Map params = new HashMap();
        params.put(ID, getId());
        params.put(PAGE_ID, getPageId());
        params.put(TITLE, getTitle());
        params.put(FILE_NAME, getFileName());
        params.put(FILE_SIZE, getFileSize());
        params.put(CONTENT_TYPE, (getContentType() != null) ? getContentType() : "");
        params.put(CREATED, getCreated());
        params.put(CREATOR, getCreator());
        params.put(URL, getUrl());
        return params;
    }

    /**
     * @return the Mime content type of the attachment. The value cannot be null as this is a
     *         required element in the Confluence specification.
     */
    public String getContentType()
    {
        return this.contentType;
    }

    /**
     * @param contentType the content type of the attachment
     * @see #getContentType()
     */
    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    /**
     * @return the creation date of the attachment
     */
    public Date getCreated()
    {
        return this.created;
    }

    /**
     * @param created the creation date of the attachment
     * @see #getCreated()
     */
    public void setCreated(Date created)
    {
        this.created = created;
    }

    /**
     * @return the name of the person who has created the attachment in the Wiki
     */
    public String getCreator()
    {
        return this.creator;
    }

    /**
     * @param creator the name of the person who has created the attachment in the Wiki
     * @see #getCreator()
     */
    public void setCreator(String creator)
    {
        this.creator = creator;
    }

    /**
     * @return the name of the file attached. For example "myattachment.jpg"
     */
    public String getFileName()
    {
        return this.fileName;
    }

    /**
     * @param fileName the name of the file attached. For example "myattachment.jpg"
     * @see #getFileName()
     */
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    /**
     * @return the attachment size in bytes
     */
    public String getFileSize()
    {
        return this.fileSize;
    }

    /**
     * @param fileSize the attachment size in bytes
     * @see #getFileSize()
     */
    public void setFileSize(String fileSize)
    {
        this.fileSize = fileSize;
    }

    /**
     * @return the id of the attachment (the id is the filename of the XWikiAttachment object used
     *         to construct this Attachment object)
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id the id of the attachment (the id is the filename of the XWikiAttachment object used
     *            to construct this Attachment object)
     * @see #getId()
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the full name of the document to which this attachment is attached to. For example
     *         "Space.Doc".
     */
    public String getPageId()
    {
        return this.pageId;
    }

    /**
     * @param pageId the full name of the document to which this attachment is attached to. For
     *            example "Space.Doc".
     */
    public void setPageId(String pageId)
    {
        this.pageId = pageId;
    }

    /**
     * @return the attachment title (we're using the comment associated with the attachment, this is
     *         the comment entered by the person who attached the object)
     */
    public String getTitle()
    {
        return this.title;
    }

    /**
     * @param title the attachment title (we're using the comment associated with the attachment,
     *            this is the comment entered by the person who attached the object)
     * @see #getTitle()
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * @return the url to download the attachment
     */
    public String getUrl()
    {
        return this.url;
    }

    /**
     * @param url the url to download the attachment
     * @see #getUrl()
     */
    public void setUrl(String url)
    {
        this.url = url;
    }
}
