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
package org.xwiki.mail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.mail.script.AbstractMailScriptService;
import org.xwiki.mail.script.ScriptMailResult;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;

/**
 * Expose Mail Storage API to scripts.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Named("mailstorage")
@Singleton
@Unstable
public class MailStorageScriptService extends AbstractMailScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    static final String ERROR_KEY = "scriptservice.mailstorage.error";

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

    /**
     * Resend the serialized MimeMessage synchronously.
     *
     * @param batchId the name of the directory that contains serialized MimeMessage
     * @param mailId the name of the serialized MimeMessage
     * @return the result and status of the send batch
     */
    public ScriptMailResult resend(String batchId, String mailId)
    {
        MailListener listener;
        try {
            listener = this.componentManagerProvider.get().getInstance(MailListener.class, "database");
        } catch (ComponentLookupException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
            // Don't send the mail!
            return null;
        }

        MimeMessage message;
        try {
            message = loadMessage(batchId, mailId);
            ScriptMailResult scriptMailResult = sendAsynchronously(Arrays.asList(message), listener, false);

            // Wait for all messages from this batch to have been sent before returning
            scriptMailResult.waitTillSent(Long.MAX_VALUE);

            return scriptMailResult;
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
     * @return the loaded {@link org.xwiki.mail.MailStatus} instances or null if not allowed or an error happens
     */
    public List<MailStatus> load(Map<String, Object> filterMap)
    {
        // Only admins are allowed
        if (this.authorizationManager.hasAccess(Right.ADMIN)) {
            try {
                return this.mailStatusStore.load(normalizeFilterMap(filterMap));
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

    private MimeMessage loadMessage(String batchId, String mailId) throws MailStoreException
    {
        MimeMessage message = this.mailContentStore.load(this.sessionProvider.get(), batchId, mailId);
        return message;
    }

    @Override
    protected String getErrorKey()
    {
        return ERROR_KEY;
    }
}
