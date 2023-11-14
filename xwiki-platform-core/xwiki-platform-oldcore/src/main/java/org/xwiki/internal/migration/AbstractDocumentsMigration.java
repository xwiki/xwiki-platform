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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.localization.LocaleUtils.toLocale;

/**
 * Allow to easily queue a document analysis task on a set documents to migrate. Sub-classes need to implement two
 * methods:
 * <ul>
 *     <li>{@link #selectDocuments()}: return the list of document references to queue for migration</li>
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
        List<DocumentReference> selectedDocuments = selectDocuments();
        logBeforeQueuingTasks(selectedDocuments);

        for (DocumentReference documentReference : selectedDocuments) {
            logBeforeQueuingTask(documentReference);
            this.taskManager.addTask(getXWikiContext().getWikiId(), new XWikiDocument(documentReference).getId(),
                getTaskType());
        }
    }

    /**
     * @return the id of the task type to queue documents to
     */
    protected abstract String getTaskType();

    /**
     * @return the list of document ids to migrate
     * @since 15.8RC1
     * @since 14.10.17
     * @since 15.5.3
     */
    protected abstract List<DocumentReference> selectDocuments() throws DataMigrationException;

    /**
     * Prints an info log with the number of queued documents and the type of the task.
     *
     * @param documents the full list of documents that will be queued
     * @since 15.8RC1
     * @since 14.10.17
     * @since 15.5.3
     */
    protected void logBeforeQueuingTasks(List<DocumentReference> documents)
    {
        this.logger.info("[{}] documents queued to task [{}]", documents.size(), getTaskType());
    }

    /**
     * Prints an info logs with an individual document and well as its queued task.
     *
     * @param documentReference a unique document reference that will be queued
     * @since 15.8RC1
     * @since 14.10.17
     * @since 15.5.3
     */
    protected void logBeforeQueuingTask(DocumentReference documentReference)
    {
        this.logger.info("document [{}] queued to task [{}]", documentReference, getTaskType());
    }

    /**
     * Resolves a document reference with the specified locale.
     *
     * @param documentReference the document reference to resolve
     * @param localeStr the locale in which to resolve the document reference
     * @return an optional containing the resolved document reference with its locale, or an empty optional if
     *     resolution fails
     * @since 15.8RC1
     * @since 14.10.17
     * @since 15.5.3
     */
    protected Optional<DocumentReference> resolveDocumentReference(String documentReference, String localeStr)
    {
        Optional<DocumentReference> optionalDocumentReference = parseLocale(localeStr)
            .map(locale -> new DocumentReference(this.documentReferenceResolver.resolve(documentReference), locale));
        if (optionalDocumentReference.isEmpty()) {
            this.logger.warn("Failed to resolve document reference [{}] with locale [{}]", documentReference,
                localeStr);
        }
        return optionalDocumentReference;
    }

    /**
     * @param locale a string representation of a locale
     * @return the resolved {@link Locale} or {@link Optional#empty()} in case of issue during the locale parsing
     * @since 15.8RC1
     * @since 14.10.17
     * @since 15.5.3
     */
    private Optional<Locale> parseLocale(String locale)
    {
        try {
            return Optional.ofNullable(toLocale(locale));
        } catch (IllegalArgumentException e) {
            this.logger.debug("Unable to resolve locale [{}]. Cause: [{}]", locale, getRootCauseMessage(e));
            return Optional.empty();
        }
    }
}
