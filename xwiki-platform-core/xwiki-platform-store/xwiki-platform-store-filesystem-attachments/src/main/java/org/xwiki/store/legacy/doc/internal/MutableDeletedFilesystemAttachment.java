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
package org.xwiki.store.legacy.doc.internal;

import java.util.Date;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * A mutable version of a DeletedFilesystemAttachment, passed around and populated while loading.
 *
 * @version $Id$
 * @since 3.0M3
 */
public class MutableDeletedFilesystemAttachment extends DeletedFilesystemAttachment
{
    /**
     * The Constructor.
     */
    public MutableDeletedFilesystemAttachment()
    {
    }

    /**
     * @return an immutable DeletedFilesystemAttachment which clones this.
     */
    public DeletedFilesystemAttachment getImmutable()
    {
        final MutableDeletedFilesystemAttachment out = new MutableDeletedFilesystemAttachment();
        out.setId(this.getId());
        out.setDocId(this.getDocId());
        out.setDocName(this.getDocName());
        out.setFilename(this.getFilename());
        out.setDate(this.getDate());
        out.setDeleter(this.getDeleter());
        out.setAttachment(this.getAttachment(), null);
        return out;
    }

    @Override
    public void setId(long id)
    {
        super.setId(id);
    }

    @Override
    public void setDocId(long docId)
    {
        super.setId(docId);
    }

    @Override
    public void setDocName(String docName)
    {
        super.setDocName(docName);
    }

    @Override
    public void setFilename(String filename)
    {
        super.setFilename(filename);
    }

    @Override
    public void setDate(Date date)
    {
        super.setDate(date);
    }

    @Override
    public void setDeleter(String deleter)
    {
        super.setDeleter(deleter);
    }

    @Override
    public void setAttachment(XWikiAttachment attachment, XWikiContext context)
    {
        super.setAttachment(attachment, context);
    }
}
