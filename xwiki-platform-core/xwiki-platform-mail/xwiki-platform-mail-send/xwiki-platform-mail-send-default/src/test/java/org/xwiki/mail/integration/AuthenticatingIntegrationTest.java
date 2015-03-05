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
package org.xwiki.mail.integration;

import java.security.Security;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import javax.inject.Provider;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.internal.EnvironmentConfiguration;
import org.xwiki.environment.internal.StandardEnvironment;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.mail.XWikiAuthenticator;
import org.xwiki.mail.internal.factory.attachment.AttachmentMimeBodyPartFactory;
import org.xwiki.mail.internal.FileSystemMailContentStore;
import org.xwiki.mail.internal.thread.PrepareMailQueueManager;
import org.xwiki.mail.internal.DefaultMailSender;
import org.xwiki.mail.internal.thread.PrepareMailRunnable;
import org.xwiki.mail.internal.thread.SendMailQueueManager;
import org.xwiki.mail.internal.thread.SendMailRunnable;
import org.xwiki.mail.internal.configuration.DefaultMailSenderConfiguration;
import org.xwiki.mail.internal.factory.text.TextMimeBodyPartFactory;
import org.xwiki.mail.internal.MemoryMailListener;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.DummySSLSocketFactory;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Integration tests to prove that mail sending is working fully end to end with the Java API when using an
 * authenticating SMTP server that requires SSL.
 *
 * @version $Id$
 * @since 6.4M1
 */
@ComponentList({
    TextMimeBodyPartFactory.class,
    AttachmentMimeBodyPartFactory.class,
    StandardEnvironment.class,
    DefaultMailSender.class,
    MemoryMailListener.class,
    SendMailRunnable.class,
    PrepareMailRunnable.class,
    PrepareMailQueueManager.class,
    SendMailQueueManager.class,
    FileSystemMailContentStore.class
})
public class AuthenticatingIntegrationTest
{
    // Required by GreenMail.
    static {
        Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());
    }

    @Rule
    public GreenMailRule mail = new GreenMailRule(ServerSetupTest.SMTPS);

    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private MailSenderConfiguration configuration;

    private MimeBodyPartFactory<String> defaultBodyPartFactory;

    private MailSender sender;

    @BeforeComponent
    public void registerConfiguration() throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.starttls.enable", "true");

        // Required by GreenMail. When using XWiki with Gmail for example this is not required.
        properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        this.configuration = new TestMailSenderConfiguration(
            this.mail.getSmtps().getPort(), "peter", "password", properties);
        this.componentManager.registerComponent(MailSenderConfiguration.class, this.configuration);

        // Set the current wiki in the Context
        ModelContext modelContext = this.componentManager.registerMockComponent(ModelContext.class);
        when(modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("wiki"));

        Provider<XWikiContext> xwikiContextProvider = this.componentManager.registerMockComponent(
            XWikiContext.TYPE_PROVIDER);
        when(xwikiContextProvider.get()).thenReturn(Mockito.mock(XWikiContext.class));

        this.componentManager.registerMockComponent(ExecutionContextManager.class);
        this.componentManager.registerMockComponent(Execution.class);

        EnvironmentConfiguration environmentConfiguration =
            this.componentManager.registerMockComponent(EnvironmentConfiguration.class);
        when(environmentConfiguration.getPermanentDirectoryPath()).thenReturn(System.getProperty("java.io.tmpdir"));
    }

    @Before
    public void initialize() throws Exception
    {
        // Create a user in the SMTP server.
        this.mail.setUser("peter@doe.com", "peter", "password");

        this.defaultBodyPartFactory = this.componentManager.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class));
        this.sender = this.componentManager.getInstance(MailSender.class);
    }

    @After
    public void cleanUp() throws Exception
    {
        // Make sure we stop the Mail Sender thread after each test (since it's started automatically when looking
        // up the MailSender component.
        ((DefaultMailSender) this.sender).stopMailThreads();
    }

    @Test
    public void sendTextMail() throws Exception
    {
        // Step 1: Create a JavaMail Session
        Properties properties = this.configuration.getAllProperties();
        assertEquals("true", properties.getProperty(DefaultMailSenderConfiguration.JAVAMAIL_SMTP_AUTH));
        Session session = Session.getInstance(properties, new XWikiAuthenticator(this.configuration));

        // Step 2: Create the Message to send
        MimeMessage message = new MimeMessage(session);
        message.setSubject("subject");
        message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress("john@doe.com"));

        // Step 3: Add the Message Body
        Multipart multipart = new MimeMultipart("mixed");
        // Add text in the body
        multipart.addBodyPart(this.defaultBodyPartFactory.create("some text here",
            Collections.<String, Object>singletonMap("mimetype", "text/plain")));
        message.setContent(multipart);

        // Step 4: Send the mail
        this.sender.sendAsynchronously(Arrays.asList(message), session, null);

        // Verify that the mail has been received (wait maximum 10 seconds).
        this.mail.waitForIncomingEmail(10000L, 1);
        MimeMessage[] messages = this.mail.getReceivedMessages();

        assertEquals(1, messages.length);
        assertEquals("subject", messages[0].getHeader("Subject", null));
        assertEquals("john@doe.com", messages[0].getHeader("To", null));

        assertEquals(1, ((MimeMultipart) messages[0].getContent()).getCount());

        BodyPart textBodyPart = ((MimeMultipart) messages[0].getContent()).getBodyPart(0);
        assertEquals("text/plain", textBodyPart.getHeader("Content-Type")[0]);
        assertEquals("some text here", textBodyPart.getContent());
    }
}
