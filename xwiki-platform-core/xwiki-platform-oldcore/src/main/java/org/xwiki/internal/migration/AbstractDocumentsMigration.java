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
import java.util.Locale;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.index.TaskManager;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

import static org.xwiki.localization.LocaleUtils.toLocale;

/**
 * Allow to easily queue a document analysis task on a set documents to migrate. Sub-classes need to implement two
 * methods:
 * <ul>
 *     <li>{@link #selectDocuments()}: return the list of document ids to queue for migration</li>
 *     <li>{@link #getTaskType()}: the type of the task to queue documents to</li>
 * </ul>
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.4.8
 * @since 14.10.4
 */
public abstract class AbstractDocumentsMigration extends AbstractHibernateDataMigration
{
    /**
     * A class representing a reference with a specific locale. This is useful as those information can be retrieved as
     * is from the database without further parsing/serializing.
     */
    protected static class ReferenceWithLocale
    {
        public final String reference;

        public final String locale;

        public ReferenceWithLocale(String reference, String locale)
        {
            this.reference = reference;
            this.locale = locale;
        }
    }

    @Inject
    protected Logger logger;

    @Inject
    @Named("current")
    protected DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private TaskManager taskManager;

    @Override
    protected void hibernateMigrate() throws DataMigrationException
    {
        List<ReferenceWithLocale> selectedDocuments = selectDocuments();
        logBeforeQueuingTasks(selectedDocuments);

        for (ReferenceWithLocale referenceWithLocale : selectedDocuments) {
            resolveLocale(referenceWithLocale).ifPresentOrElse(locale -> {
                XWikiDocument document =
                    new XWikiDocument(this.documentReferenceResolver.resolve(referenceWithLocale.reference),
                        locale);
                logBeforeQueuingTask(referenceWithLocale);
                this.taskManager.addTask(getXWikiContext().getWikiId(), document.getId(), getTaskType());
            }, () -> this.logger.warn("Unknown locale [{}]. Skipping document [{}]", referenceWithLocale.locale,
                referenceWithLocale.reference));
        }
    }

    /**
     * @return the id of the task type to queue documents to
     */
    protected abstract String getTaskType();

    /**
     * @return the list of document ids to migrate
     */
    protected abstract List<ReferenceWithLocale> selectDocuments() throws DataMigrationException;

    /**
     * Prints an info log with the number of queued documents and the type of the task.
     *
     * @param documents the full list of documents that will be queued
     */
    protected void logBeforeQueuingTasks(List<ReferenceWithLocale> documents)
    {
        this.logger.info("[{}] documents queued to task [{}]", documents.size(), getTaskType());
    }

    /**
     * Prints an info logs with an individual document and well as its queued task.
     *
     * @param reference a unique document reference that will be queued
     */
    protected void logBeforeQueuingTask(ReferenceWithLocale reference)
    {
        this.logger.info("document [{}] with locale [{}] queued to task [{}]", reference.reference, reference.locale,
            getTaskType());
    }

    private static Optional<Locale> resolveLocale(ReferenceWithLocale referenceWithLocale)
    {
        Optional<Locale> locale;
        try {
            locale = Optional.ofNullable(toLocale(referenceWithLocale.locale));
        } catch (IllegalArgumentException e) {
            locale = Optional.empty();
        }
        return locale;
    }
}
