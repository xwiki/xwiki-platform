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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Give each document translation the syntax of its original document when they differ.
 * <p>
 * The content of a translation is displayed with the syntax of the translation. Some translations are stored with a
 * syntax that doesn't match their content: e.g. a translation created from in-place editing, which doesn't submit the
 * syntax, got the default syntax of the wiki while its content is written in the syntax of the original document.
 * Before XWIKI-15074 (DefaultDocumentDisplayer does not take into account isContentTranslated()) the content of a
 * translation was displayed with the syntax of the original document, so aligning the syntaxes keeps the existing
 * translations displayed the same way.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Component
@Singleton
@Named("180900000XWIKI15074")
public class R180900000XWIKI15074DataMigration extends AbstractHibernateDataMigration
{
    private static final int BATCH_SIZE = 100;

    // Empty values are stored either as empty strings or as null (Oracle stores empty strings as null), so
    // "length(...) > 0" is the portable way of selecting the non-empty ones. A translation with an empty syntax is
    // loaded with the xwiki/1.0 syntax, so it's selected too when the original document has a syntax.
    private static final String MISMATCHED_TRANSLATIONS_QUERY = "select translation.id, original.syntaxId "
        + "from XWikiDocument translation, XWikiDocument original "
        + "where translation.fullName = original.fullName and length(translation.language) > 0 "
        + "and (original.language is null or original.language = '') "
        + "and length(trim(original.syntaxId)) > 0 "
        + "and (translation.syntaxId is null or length(trim(translation.syntaxId)) = 0 "
        + "or translation.syntaxId <> original.syntaxId) "
        + "order by translation.id";

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Give each document translation the syntax of the original document when they differ.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(180900000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();

        int total = 0;
        List<Object[]> results;
        do {
            // No offset: the updated translations don't match the query anymore.
            results = getMismatchedTranslations(context);
            if (!results.isEmpty()) {
                int updated = 0;
                hibernateStore.beginTransaction(context);
                try {
                    for (Object[] result : results) {
                        updated += updateSyntax((Long) result[0], (String) result[1], hibernateStore, context);
                    }
                } finally {
                    hibernateStore.endTransaction(context, true);
                }
                if (updated == 0) {
                    // Nothing could be updated, so the next query would return the same translations.
                    throw new DataMigrationException(String.format(
                        "Failed to update the syntax of the [%s] translations with id [%s] and the following ones.",
                        results.size(), results.getFirst()[0]));
                }
                total += updated;
            }
        } while (!results.isEmpty());

        this.logger.info("Updated the syntax of [{}] document translations to match their original document.",
            total);
    }

    private List<Object[]> getMismatchedTranslations(XWikiContext context) throws DataMigrationException
    {
        try {
            return context.getWiki().getStore().getQueryManager()
                .createQuery(MISMATCHED_TRANSLATIONS_QUERY, Query.HQL).setLimit(BATCH_SIZE)
                .execute();
        } catch (QueryException e) {
            throw new DataMigrationException("Failed to find the translations with a syntax different from the one"
                + " of their original document", e);
        }
    }

    private int updateSyntax(Long id, String syntaxId, XWikiHibernateStore hibernateStore, XWikiContext context)
        throws XWikiException
    {
        return hibernateStore.executeWrite(context,
            session -> session.createQuery("update XWikiDocument set syntaxId = :syntaxId where id = :id")
                .setParameter("syntaxId", syntaxId).setParameter("id", id).executeUpdate());
    }
}
