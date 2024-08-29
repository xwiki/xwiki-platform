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
package com.xpn.xwiki.store.migration.hibernate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-19125: Copy the author reference value to the new original metadata author reference.
 *
 * @since 14.0RC1
 * @version $Id$
 */
@Component
@Named("R140000000XWIKI19125")
@Singleton
public class R140000000XWIKI19125DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Copy author value to the new originalMetadataAuthorReference column";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(140000000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        Integer updatedDocuments = getStore().executeWrite(getXWikiContext(), this::updateDocuments);
        this.logger.info("[{}] documents updated on database [{}]", updatedDocuments, getXWikiContext().getWikiId());
    }

    private int updateDocuments(Session session)
    {
        Query query =
            session.createQuery("update XWikiDocument doc set doc.originalMetadataAuthorReference = doc.author");
        return query.executeUpdate();
    }
}
