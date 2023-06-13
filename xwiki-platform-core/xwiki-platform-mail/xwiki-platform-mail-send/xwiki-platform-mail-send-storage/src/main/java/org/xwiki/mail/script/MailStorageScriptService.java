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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

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
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;

/**
 * Expose Mail Storage API to scripts.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@Component
@Named("mail.storage")
@Singleton
public class MailStorageScriptService extends AbstractMailScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String ERROR_KEY = "scriptservice.mail.storage.error";

    @Inject
    @Named("filesystem")
    private MailContentStore mailContentStore;

    @Inject
    @Named("database")
    private MailStatusStore mailStatusStore;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

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
     * @return the result and status of the send batch; null if an error occurred when getting the message from the
     *         store
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
     * @return the result and status of the send batch; null if an error occurred when getting the message from the
     *         store
     * @since 9.3RC1
     */
    public ScriptMailResult resendAsynchronously(String batchId, String uniqueMessageId)
    {
        try {
            MailStatusResult statusResult = this.mailResender.resendAsynchronously(batchId, uniqueMessageId);
            ScriptMailResult scriptMailResult = new ScriptMailResult(new DefaultMailResult(batchId), statusResult);
            return scriptMailResult;
        } catch (MailStoreException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
            return null;
        }
    }

    /**
     * Resends all mails matching the passed filter map, asynchronously.
     *
     * @param filterMap the map of Mail Status parameters to match (e.g. "state", "wiki", "batchId", etc)
     * @param offset the number of rows to skip (0 means don't skip any row)
     * @param count the number of rows to return. If 0 then all rows are returned
     * @return the mail results for the resent mails and null if an error occurred while loading the mail statuses
     *         from the store
     * @since 9.3RC1
     */
    public List<ScriptMailResult> resendAsynchronously(Map<String, Object> filterMap, int offset, int count)
    {
        return resendGeneric(filterMap, offset, count,
            (filterMap1, offset1, count1) -> mailResender.resendAsynchronously(filterMap1, offset1, count1));
    }

    /**
     * Resends all mails matching the passed filter map, synchronously (one mail after another).
     *
     * @param filterMap the map of Mail Status parameters to match (e.g. "state", "wiki", "batchId", etc)
     * @param offset the number of rows to skip (0 means don't skip any row)
     * @param count the number of rows to return. If 0 then all rows are returned
     * @return the mail results for the resent mails and null if an error occurred while loading the mail statuses
     *         from the store
     * @since 12.10
     */
    public List<ScriptMailResult> resend(Map<String, Object> filterMap, int offset, int count)
    {
        return resendGeneric(filterMap, offset, count,
            (filterMap1, offset1, count1) -> mailResender.resend(filterMap1, offset1, count1));
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
        // Note: We don't need to check permissions since the caller already needs to know the message id
        // to be able to call this method and for it to have any effect.

        try {
            return this.mailStatusStore.load(uniqueMessageId);
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
        // Only admins are allowed
        if (this.authorizationManager.hasAccess(Right.ADMIN)) {
            try {
                return this.mailStatusStore.load(normalizeFilterMap(filterMap), offset, count,
                    sortField, sortAscending);
            } catch (MailStoreException e) {
                // Save the exception for reporting through the script services's getLastError() API
                setError(e);
                return null;
            }
        } else {
            // Save the exception for reporting through the script services's getLastError() API
            setError(new MailStoreException("You need Admin rights to load mail statuses"));
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
        // Only admins are allowed
        if (this.authorizationManager.hasAccess(Right.ADMIN)) {
            try {
                return this.mailStatusStore.count(normalizeFilterMap(filterMap));
            } catch (MailStoreException e) {
                // Save the exception for reporting through the script services's getLastError() API
                setError(e);
                return 0;
            }
        } else {
            // Save the exception for reporting through the script services's getLastError() API
            setError(new MailStoreException("You need Admin rights to count mail statuses"));
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
        // Note: We don't need to check permissions since the call to load() will do that anyway.

        Map<String, Object> filterMap = Collections.<String, Object>singletonMap("batchId", batchId);
        List<MailStatus> statuses = load(filterMap, 0, 0, null, false);
        if (statuses != null) {
            for (MailStatus status : statuses) {
                delete(batchId, status.getMessageId());
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
        // Note: We don't need to check permissions since the call to load() will do that anyway.

        List<MailStatus> statuses = load(Collections.emptyMap(), 0, 0, null, false);
        if (statuses != null) {
            for (MailStatus status : statuses) {
                delete(status.getBatchId(), status.getMessageId());
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
        // Note: We don't need to check permissions since the caller already needs to know the batch id and mail id
        // to be able to call this method and for it to have any effect.

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
     * @return the configuration for the Mail Storage
     */
    public MailStorageConfiguration getConfiguration()
    {
        return this.storageConfiguration;
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

    private MimeMessage loadMessage(Session session, String batchId, String mailId) throws MailStoreException
    {
        MimeMessage message = this.mailContentStore.load(session, batchId, mailId);
        return message;
    }

    private List<ScriptMailResult> resendGeneric(Map<String, Object> filterMap, int offset, int count,
        MultipleMailResender multipleMailResender)
    {
        List<Pair<MailStatus, MailStatusResult>> results;
        try {
            results = multipleMailResender.resendMessages(normalizeFilterMap(filterMap), offset, count);
        } catch (MailStoreException e) {
            // Save the exception for reporting through the script service's getLastError() API
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

    @Override
    protected String getErrorKey()
    {
        return ERROR_KEY;
    }

    @FunctionalInterface
    private interface MultipleMailResender
    {
        /**
         * Resends messages.
         * 
         * @param filterMap the map of Mail Status parameters to match (e.g. "status", "wiki", "batchId", etc)
         * @param offset the number of rows to skip (0 means don't skip any row)
         * @param count the number of rows to return. If 0 then all rows are returned
         * @return the mail results for the resent mails.
         * @throws MailStoreException If an exception occurs.
         */
        List<Pair<MailStatus, MailStatusResult>> resendMessages(Map<String, Object> filterMap, int offset, int count)
            throws MailStoreException;
    }
}
