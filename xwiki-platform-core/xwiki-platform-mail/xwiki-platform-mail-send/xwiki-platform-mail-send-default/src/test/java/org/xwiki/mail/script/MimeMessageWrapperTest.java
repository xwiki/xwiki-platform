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

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.internal.ExtendedMimeMessage;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.mail.script.MimeMessageWrapper}.
 *
 * @version $Id$
 * @since 6.4M2
 */
public class MimeMessageWrapperTest
{
    @Test
    public void sendWhenNotAllowed() throws Exception
    {
        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        message.setText("test");
        message.setSubject("subject");
        ExtendedMimeMessage extendedMessage = new ExtendedMimeMessage(message);

        MailSender sender = mock(MailSender.class);

        Execution execution = mock(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);

        MailSenderConfiguration configuration = mock(MailSenderConfiguration.class);
        when(configuration.getScriptServicePermissionCheckerHint()).thenReturn("test");

        ScriptServicePermissionChecker checker = mock(ScriptServicePermissionChecker.class);
        doThrow(new MessagingException("notauthorized")).when(checker).check(any(MimeMessage.class));

        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getInstance(ScriptServicePermissionChecker.class, "test")).thenReturn(checker);

        MimeMessageWrapper wrapper =
            new MimeMessageWrapper(extendedMessage, session, sender, execution, configuration, componentManager);

        wrapper.send();

        // Check errors (we should have one, stored in the EC!)
        Exception expectedException = (Exception) executionContext.getProperty(MailSenderScriptService.ERROR_KEY);
        assertEquals("Not authorized to send mail with subject [subject], using Permission Checker [test]. The mail "
            + "has not been sent.", expectedException.getMessage());
    }

    @Test
    public void sendWhenCheckerNotFound() throws Exception
    {
        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        message.setText("test");
        message.setSubject("subject");
        ExtendedMimeMessage extendedMessage = new ExtendedMimeMessage(message);

        MailSender sender = mock(MailSender.class);

        Execution execution = mock(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);

        MailSenderConfiguration configuration = mock(MailSenderConfiguration.class);
        when(configuration.getScriptServicePermissionCheckerHint()).thenReturn("test");

        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getInstance(ScriptServicePermissionChecker.class, "test")).thenThrow(
            new ComponentLookupException("not found"));

        MimeMessageWrapper wrapper =
            new MimeMessageWrapper(extendedMessage, session, sender, execution, configuration, componentManager);

        wrapper.send();

        // Check errors (we should have one, stored in the EC!)
        Exception expectedException = (Exception) executionContext.getProperty(MailSenderScriptService.ERROR_KEY);
        assertEquals("Failed to locate Permission Checker [test]. The mail has not been sent.",
            expectedException.getMessage());
    }
}
