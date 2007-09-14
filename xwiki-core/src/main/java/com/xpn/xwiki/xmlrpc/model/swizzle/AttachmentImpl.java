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
 *
 */

package com.xpn.xwiki.xmlrpc.model.swizzle;

import java.util.Date;
import java.util.Map;

import org.codehaus.swizzle.confluence.MapConvertor;
import org.codehaus.swizzle.confluence.SwizzleConversionException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.xmlrpc.model.Attachment;


/**
 * @author hritcu
 *
 */
public class AttachmentImpl implements Attachment
{
    
    private org.codehaus.swizzle.confluence.Attachment target;
    
    public AttachmentImpl()
    {
        target = new org.codehaus.swizzle.confluence.Attachment();
    }

    public AttachmentImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.Attachment(map);
    }
    
    public AttachmentImpl(Map map, MapConvertor convertor) throws XWikiException
    {
        Map typeMap = org.codehaus.swizzle.confluence.Attachment.FIELD_TYPES;
        try {
            target = new org.codehaus.swizzle.confluence.Attachment(convertor.revert(map, typeMap));
        } catch (SwizzleConversionException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC, 0, e.getMessage(), e);
        }
    }

    public AttachmentImpl(org.codehaus.swizzle.confluence.Attachment attachment)
    {
        target = attachment;
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#getComment()
     */
    public String getComment()
    {
        return target.getComment();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#getContentType()
     */
    public String getContentType()
    {
        return target.getContentType();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#getCreated()
     */
    public Date getCreated()
    {
        return target.getCreated();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#getCreator()
     */
    public String getCreator()
    {
        return target.getCreator();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#getFileName()
     */
    public String getFileName()
    {
        return target.getFileName();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#getFileSize()
     */
    public int getFileSize()
    {
        return target.getFileSize();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#getId()
     */
    public String getId()
    {
        return target.getId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#getPageId()
     */
    public String getPageId()
    {
        return target.getPageId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#getTitle()
     */
    public String getTitle()
    {
        return target.getTitle();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#getUrl()
     */
    public String getUrl()
    {
        return target.getUrl();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#setComment(java.lang.String)
     */
    public void setComment(String comment)
    {
        target.setComment(comment);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#setContentType(java.lang.String)
     */
    public void setContentType(String contentType)
    {
        target.setContentType(contentType);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#setCreated(java.util.Date)
     */
    public void setCreated(Date created)
    {
        target.setCreated(created);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#setCreator(java.lang.String)
     */
    public void setCreator(String creator)
    {
        target.setCreator(creator);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#setFileName(java.lang.String)
     */
    public void setFileName(String fileName)
    {
        target.setFileName(fileName);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#setFileSize(int)
     */
    public void setFileSize(int fileSize)
    {
        target.setFileSize(fileSize);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#setId(java.lang.String)
     */
    public void setId(String id)
    {
        target.setId(id);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#setPageId(java.lang.String)
     */
    public void setPageId(String pageId)
    {
        target.setPageId(pageId);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#setTitle(java.lang.String)
     */
    public void setTitle(String title)
    {
        target.setTitle(title);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Attachment#setUrl(java.lang.String)
     */
    public void setUrl(String url)
    {
        target.setUrl(url);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.MapObject#toMap(java.lang.String)
     */
    public Map toMap()
    {
        return target.toMap();
    }

    public org.codehaus.swizzle.confluence.Attachment getTarget()
    {
        return target;
    }
}
