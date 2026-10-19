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
package org.xwiki.mail.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailResender;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStorageConfiguration;
import org.xwiki.mail.MailStoreException;
import org.xwiki.mail.internal.DefaultMailResult;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;

/**
 * Expose Mail Storage API to scripts.
 *
 * @version $Id$
 * @since 6.4M3
 * @deprecated use {@link MailStorageScriptService} instead
 */
@Component
@Named("mailstorage")
@Singleton
@Deprecated(since = "12.4RC1")
public class DeprecatedMailStorageScriptService extends AbstractMailScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String ERROR_KEY = "scriptservice.mailstorage.error";

    private static final String NOT_ADMIN_ERROR = "The author of the script needs Admin rights on the wiki to %s";

    private static final String RESEND_ACTION = "resend mails";

    private static final String LOAD_ACTION = "load mail statuses";

    private static final String NOT_FROM_CURRENT_WIKI_ERROR = "The mail [%s] was not sent from the current wiki";

    private static final String NOT_FROM_CURRENT_WIKI_BATCH_ERROR =
        "The mail [%s] of the batch [%s] was not sent from the current wiki";

    @Inject
    @Named("filesystem")
    private MailContentStore mailContentStore;

    @Inject
    @Named("database")
    private MailStatusStore mailStatusStore;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private MailStorageConfiguration storageConfiguration;

    @Inject
    @Named("database")
    private MailResender mailResender;

    /**
     * Resend the serialized MimeMessage synchronously.
     *
     * @param batchId the name of the directory that contains serialized MimeMessage
     * @param uniqueMessageId the unique id of the serialized MimeMessage
     * @return the result and status of the send batch; null if not allowed or if an error occurred when getting the
     *         message from the store
     */
    public ScriptMailResult resend(String batchId, String uniqueMessageId)
    {
        ScriptMailResult result = resendAsynchronously(batchId, uniqueMessageId);
        if (result != null) {
            // Wait for the message to have been resent before returning
            result.getStatusResult().waitTillProcessed(Long.MAX_VALUE);
        }
        return result;
    }

    /**
     * Resend the serialized MimeMessage asynchronously.
     *
     * @param batchId the name of the directory that contains serialized MimeMessage
     * @param uniqueMessageId the unique id of the serialized MimeMessage
     * @return the result and status of the send batch; null if not allowed or if an error occurred when getting the
     *         message from the store
     * @since 9.3RC1
     */
    public ScriptMailResult resendAsynchronously(String batchId, String uniqueMessageId)
    {
        try {
            if (!canAccessMail(batchId, uniqueMessageId, RESEND_ACTION)) {
                return null;
            }
            MailStatusResult statusResult = this.mailResender.resendAsynchronously(batchId, uniqueMessageId);
            return new ScriptMailResult(new DefaultMailResult(batchId), statusResult);
        } catch (MailStoreException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
            return null;
        }
    }

    /**
     * Resends all mails matching the passed filter map.
     *
     * @param filterMap the map of Mail Status parameters to match (e.g. "state", "wiki", "batchId", etc)
     * @param offset the number of rows to skip (0 means don't skip any row)
     * @param count the number of rows to return. If 0 then all rows are returned
     * @return the mail results for the resent mails and null if not allowed or if an error occurred while loading
     *         the mail statuses from the store
     * @since 9.3RC1
     */
    public List<ScriptMailResult> resendAsynchronously(Map<String, Object> filterMap, int offset, int count)
    {
        if (!checkAdminAccess(RESEND_ACTION)) {
            return null;
        }

        List<Pair<MailStatus, MailStatusResult>> results;
        try {
            results = this.mailResender.resendAsynchronously(normalizeFilterMap(filterMap), offset, count);
        } catch (MailStoreException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
            return null;
        }

        List<ScriptMailResult> scriptResults = new ArrayList<>();
        for (Pair<MailStatus, MailStatusResult> result : results) {
            scriptResults.add(new ScriptMailResult(
                new DefaultMailResult(result.getLeft().getBatchId()), result.getRight()));
        }
        return scriptResults;
    }

    /**
     * Load message status for the message matching the given message Id.
     *
     * @param uniqueMessageId the unique identifier of the message.
     * @return the loaded {@link org.xwiki.mail.MailStatus} or null if not allowed or an error happens
     * @since 7.1M2
     */
    public MailStatus load(String uniqueMessageId)
    {
        if (!checkAdminAccess(LOAD_ACTION)) {
            return null;
        }

        try {
            MailStatus status = this.mailStatusStore.load(uniqueMessageId);
            if (status != null && !isFromCurrentWiki(status)) {
                setError(new MailStoreException(String.format(NOT_FROM_CURRENT_WIKI_ERROR, uniqueMessageId)));
                return null;
            }
            return status;
        } catch (MailStoreException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
            return null;
        }
    }

    /**
     * Loads all message statuses matching the passed filters.
     *
     * @param filterMap the map of Mail Status parameters to match (e.g. "status", "wiki", "batchId", etc)
     * @param offset the number of rows to skip (0 means don't skip any row)
     * @param count the number of rows to return. If 0 then all rows are returned
     * @param sortField the name of the field used to order returned status
     * @param sortAscending when true, sort is done in ascending order of sortField, else in descending order
     * @return the loaded {@link org.xwiki.mail.MailStatus} instances or null if not allowed or an error happens
     * @since 7.1M2
     */
    public List<MailStatus> load(Map<String, Object> filterMap, int offset, int count, String sortField,
        boolean sortAscending)
    {
        if (!checkAdminAccess(LOAD_ACTION)) {
            return null;
        }

        try {
            return this.mailStatusStore.load(normalizeFilterMap(filterMap), offset, count, sortField, sortAscending);
        } catch (MailStoreException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
            return null;
        }
    }

    /**
     * Count the number of message statuses matching the passed filters.
     *
     * @param filterMap the map of Mail Status parameters to match (e.g. "status", "wiki", "batchId", etc)
     * @return the number of mail statuses or 0 if not allowed or an error happens
     */
    public long count(Map<String, Object> filterMap)
    {
        if (!checkAdminAccess("count mail statuses")) {
            return 0;
        }

        try {
            return this.mailStatusStore.count(normalizeFilterMap(filterMap));
        } catch (MailStoreException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
            return 0;
        }
    }

    /**
     * Delete all messages from a batch (both the statuses in the database and the serialized messages on the file
     * system).
     *
     * @param batchId the id of the batch for which to delete all messages
     */
    public void delete(String batchId)
    {
        // Note: We don't need to check permissions since the call to load() will do that anyway, and it only returns
        // the mail statuses of the current wiki.

        Map<String, Object> filterMap = Collections.<String, Object>singletonMap("batchId", batchId);
        List<MailStatus> statuses = load(filterMap, 0, 0, null, false);
        if (statuses != null) {
            for (MailStatus status : statuses) {
                deleteMail(batchId, status.getMessageId());
            }
        }
    }

    /**
     * Delete all messages having a status in the database and their serialized messages on the file system.
     *
     * @since 9.4RC1
     */
    public void deleteAll()
    {
        // Note: We don't need to check permissions since the call to load() will do that anyway, and it only returns
        // the mail statuses of the current wiki.

        List<MailStatus> statuses = load(Collections.emptyMap(), 0, 0, null, false);
        if (statuses != null) {
            for (MailStatus status : statuses) {
                deleteMail(status.getBatchId(), status.getMessageId());
            }
        }
    }

    /**
     * Delete a message (both the status in the database and the serialized messages on the file system).
     *
     * @param batchId the id of the batch for the message to delete
     * @param uniqueMessageId the unique id of the message to delete
     */
    public void delete(String batchId, String uniqueMessageId)
    {
        try {
            if (canAccessMail(batchId, uniqueMessageId, "delete mails")) {
                deleteMail(batchId, uniqueMessageId);
            }
        } catch (MailStoreException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
        }
    }

    /**
     * @return the configuration for the Mail Storage
     */
    public MailStorageConfiguration getConfiguration()
    {
        return this.storageConfiguration;
    }

    private void deleteMail(String batchId, String uniqueMessageId)
    {
        try {
            // Step 1: Delete mail status from store
            this.mailStatusStore.delete(uniqueMessageId, Collections.<String, Object>emptyMap());
            // Step 2: Delete any matching serialized mail
            this.mailContentStore.delete(batchId, uniqueMessageId);
        } catch (MailStoreException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
        }
    }

    /**
     * Check that the author of the script calling this script service is an administrator of the current wiki. The
     * current user is not checked: a page calling this script service is responsible for checking who can use it. The
     * rights are checked on the wiki and not on the current document, so that being an administrator of a space is not
     * enough. The author cannot be trusted when the permissions have been dropped.
     *
     * @param action the action being performed, used in the error message
     * @return {@code true} if the script author is allowed, {@code false} otherwise, in which case the error is saved
     *     for {@link #getLastError()}
     */
    private boolean checkAdminAccess(String action)
    {
        XWikiContext xwikiContext = this.xwikiContextProvider.get();
        if (!xwikiContext.hasDroppedPermissions() && this.authorizationManager.hasAccess(Right.ADMIN,
            xwikiContext.getAuthorReference(), new WikiReference(xwikiContext.getWikiId())))
        {
            return true;
        }

        // Save the exception for reporting through the script services's getLastError() API
        setError(new MailStoreException(String.format(NOT_ADMIN_ERROR, action)));
        return false;
    }

    /**
     * Check that the given mail can be acted on: the author of the script needs to be an administrator of the current
     * wiki, and the mail needs to have been sent from the current wiki in the given batch unless it's the main wiki,
     * since the mail statuses of all the wikis are stored in the main wiki. The batch is checked too because the
     * content of the mail is located from the batch id passed by the caller and not from the loaded status.
     *
     * @param batchId the id of the batch of the mail
     * @param uniqueMessageId the unique id of the mail
     * @param action the action being performed, used in the error message
     * @return {@code true} if the action is allowed, {@code false} otherwise, in which case the error is saved for
     *     {@link #getLastError()}
     * @throws MailStoreException when failing to load the status of the mail
     */
    private boolean canAccessMail(String batchId, String uniqueMessageId, String action) throws MailStoreException
    {
        if (!checkAdminAccess(action)) {
            return false;
        }

        if (!this.xwikiContextProvider.get().isMainWiki()) {
            MailStatus status = this.mailStatusStore.load(uniqueMessageId);
            if (status == null || !isFromCurrentWiki(status) || !Objects.equals(batchId, status.getBatchId())) {
                setError(new MailStoreException(
                    String.format(NOT_FROM_CURRENT_WIKI_BATCH_ERROR, uniqueMessageId, batchId)));
                return false;
            }
        }

        return true;
    }

    private boolean isFromCurrentWiki(MailStatus status)
    {
        XWikiContext xwikiContext = this.xwikiContextProvider.get();
        return xwikiContext.isMainWiki() || xwikiContext.getWikiId().equals(status.getWiki());
    }

    private Map<String, Object> normalizeFilterMap(Map<String, Object> filterMap)
    {
        // Force the wiki to be the current wiki to prevent any subwiki to see the list of mail statuses from other
        // wikis
        Map<String, Object> normalizedMap = new HashMap<>(filterMap);
        XWikiContext xwikiContext = this.xwikiContextProvider.get();
        if (!xwikiContext.isMainWiki()) {
            normalizedMap.put("wiki", xwikiContext.getWikiId());
        }
        return normalizedMap;
    }

    @Override
    protected String getErrorKey()
    {
        return ERROR_KEY;
    }
}
