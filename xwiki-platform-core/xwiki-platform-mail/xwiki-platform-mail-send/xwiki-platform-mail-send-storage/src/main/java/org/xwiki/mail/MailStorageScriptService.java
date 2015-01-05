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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.stability.Unstable;

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
public class MailStorageScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    static final String ERROR_KEY = "scriptservice.mailstorage.error";

    @Inject
    @Named("filesystem")
    private MailContentStore mailContentStore;

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    @Inject
    private Provider<Session> sessionProvider;

    /**
     * Load the serialized MimeMessage synchronously.
     *
     * @param batchId the name of the directory that contains serialized MimeMessage
     * @param mailId the name of the serialized MimeMessage
     * @return the MimeMessage instance deserialized from the store
     */
    public MimeMessage loadMessage(String batchId, String mailId)
    {
        try {
            MimeMessage message = this.mailContentStore.load(this.sessionProvider.get(), batchId, mailId);
        } catch (MailStoreException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
        }
        return null;
    }

    /**
     * Get the error generated while performing the previously called action. An error can happen for example when:
     *
     * @return the exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(ERROR_KEY, e);
    }
}
