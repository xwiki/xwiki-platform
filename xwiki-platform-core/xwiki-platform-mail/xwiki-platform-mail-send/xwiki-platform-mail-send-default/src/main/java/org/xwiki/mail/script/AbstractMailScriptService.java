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

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.internal.SessionFactory;
import org.xwiki.script.service.ScriptService;

/**
 * @version $Id $
 * @since 6.4M3
 */
public abstract class AbstractMailScriptService implements ScriptService
{
    @Inject
    protected MailSender mailSender;

    @Inject
    @Named("context")
    protected Provider<ComponentManager> componentManagerProvider;

    /**
     * Provides access to the current context.
     */
    @Inject
    protected Execution execution;

    @Inject
    protected SessionFactory sessionFactory;

    @Inject
    protected MailSenderConfiguration senderConfiguration;

    /**
     * Send the mail asynchronously.
     *
     * @param messages the list of messages that was tried to be sent
     * @param listener the {@link org.xwiki.mail.MailListener} component
     * @param checkPermissions if true then we check authorization to send mail.
     * @return the result and status of the send batch
     */
    public ScriptMailResult sendAsynchronously(Iterable<? extends MimeMessage> messages, MailListener listener,
        boolean checkPermissions)
    {
        if (checkPermissions)
        {
            try {
                checkPermissions();
            } catch (MessagingException e) {
                // Save the exception for reporting through the script services's getLastError() API
                setError(e);
                // Don't send the mail!
                return null;
            }
        }

        // NOTE: we don't throw any error since the message is sent asynchronously. All errors can be found using
        // the passed listener.
        return new ScriptMailResult(this.mailSender.sendAsynchronously(messages, this.sessionFactory.create(
            Collections.<String, String>emptyMap()), listener), listener.getMailStatusResult());
    }

    /**
     * Check authorization to send mail.
     *
     * @throws MessagingException when not authorized to send mail
     */
    private void checkPermissions() throws MessagingException
    {
        // Load the configured permission checker
        ScriptServicePermissionChecker checker;
        String hint = this.senderConfiguration.getScriptServicePermissionCheckerHint();
        try {
            checker = this.componentManagerProvider.get().getInstance(ScriptServicePermissionChecker.class, hint);
        } catch (ComponentLookupException e) {
            // Failed to load the user-configured hint, in order not to have a security hole, consider that we're not
            // authorized to send emails!
            throw new MessagingException(String.format("Failed to locate Permission Checker [%s]. "
                + "The mail has not been sent.", hint), e);
        }

        try {
            checker.check();
        } catch (MessagingException e) {
            throw new MessagingException(String.format("Not authorized by the Permission Checker [%s] to send mail! "
                + "No mail has been sent.", hint), e);
        }
    }

    /**
     * Get the error generated while performing the previously called action. An error can happen for example when:
     * <ul>
     *   <li>creating the message to send</li>
     *   <li>if there isn't enough permissions to send mails (for example if the page containing the sending script
     *       doesn't have Programming Rights)</li>
     *   <li>if the MailListener corresponding to the passed hint doesn't exist</li>
     * </ul>
     *
     * @return the exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(getErrorKey());
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    protected void setError(Exception e)
    {
        this.execution.getContext().setProperty(getErrorKey(), e);
    }

    /**
     * @return The key under which the last encountered error is stored in the current execution context.
     */
    protected abstract String getErrorKey();
}
