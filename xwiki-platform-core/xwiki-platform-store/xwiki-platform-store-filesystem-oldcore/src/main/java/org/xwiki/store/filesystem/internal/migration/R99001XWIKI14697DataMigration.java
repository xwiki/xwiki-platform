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

package org.xwiki.store.filesystem.internal.migration;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;

import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-14697. Make sure all attachments have the right archive store id.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Component
@Named("R99001XWIKI14697")
@Singleton
public class R99001XWIKI14697DataMigration extends AbstractXWIKI14697DataMigration
{
    /**
     * The default constructor.
     */
    public R99001XWIKI14697DataMigration()
    {
        super("XWikiAttachmentArchive", "archiveStore");
    }

    @Override
    public String getDescription()
    {
        return "Make sure all attachments have the right archive store id.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(99001);
    }

    @Override
    protected boolean isFile(AttachmentReference attachmentReference)
    {
        return this.fstools.attachmentArchiveExist(attachmentReference);
    }
}
