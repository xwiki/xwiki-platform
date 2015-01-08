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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.mail.script.AbstractMailScriptService;
import org.xwiki.mail.script.ScriptMailResult;
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
public class MailStorageScriptService extends AbstractMailScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    static final String ERROR_KEY = "scriptservice.mailstorage.error";

    @Inject
    @Named("filesystem")
    private MailContentStore mailContentStore;

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
            ScriptMailResult  scriptMailResult = sendAsynchronously(Arrays.asList(message), listener, false);

            // Wait for all messages from this batch to have been sent before returning
            scriptMailResult.waitTillSent(Long.MAX_VALUE);

            return scriptMailResult;
        } catch (MailStoreException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
            return null;
        }
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
