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
package org.xwiki.internal.migration;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.security.internal.XWikiLegacyPasswordEncoder;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration in charge of re-encoding legacy passwords to enhance their security.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("180800000XWIKI24357")
public class R180800000XWIKI24357DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Re-hash all passwords value with stronger algorithm.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(180800000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        XWiki wiki = getXWikiContext().getWiki();
        XWikiStoreInterface store = wiki.getStore();
        XWikiLegacyPasswordEncoder encoder = new XWikiLegacyPasswordEncoder();
        XWikiHibernateStore hibernateStore = wiki.getHibernateStore();

        int batchSize = 100;
        try {
            List<Object> countResult = store.getQueryManager()
                .createQuery("select count(*) from PasswordProperty where length(value) > 0",
                    Query.HQL)
                .execute();
            logger.info("Found [{}] passwords to check for possible re-hashing.", countResult.get(0));

            int offset = 0;
            List<Object[]> results;
            do {
                Query query =
                    store.getQueryManager()
                        .createQuery("select id.id, id.name, value from PasswordProperty where length(value) > 0 "
                                + "order by id",
                            Query.HQL)
                        .setLimit(batchSize)
                        .setOffset(offset);
                results = query.execute();
                offset += results.size();
                if (!results.isEmpty()) {
                    logger.info("Processing a batch of [{}] passwords.", results.size());
                    int processed = 0;
                    hibernateStore.beginTransaction(getXWikiContext());
                    for (Object[] result : results) {
                        if (updatePassword(
                            (Long) result[0],
                            String.valueOf(result[1]),
                            String.valueOf(result[2]),
                            encoder,
                            hibernateStore))
                        {
                            processed++;
                        }
                    }
                    hibernateStore.endTransaction(getXWikiContext(), true);
                    logger.info("[{}] passwords updated.", processed);
                }
            } while (!results.isEmpty());
        } catch (QueryException e) {
            throw new DataMigrationException("Error while performing query to access passwords", e);
        }
    }

    private boolean updatePassword(Long id, String name, String value, XWikiLegacyPasswordEncoder encoder,
        XWikiHibernateStore hibernateStore) throws XWikiException
    {
        if (value.startsWith("hash:")) {
            String newValue = encoder.reencodePassword(value);
            int result = hibernateStore.executeWrite(getXWikiContext(), session ->
                session.createQuery("update PasswordProperty set value = :value where id.id = :id and id.name "
                    + "= "
                    + ":name")
                .setParameter("value", newValue)
                .setParameter("id", id)
                .setParameter("name", name)
                .executeUpdate());
            return result == 1;
        }
        return false;
    }
}
