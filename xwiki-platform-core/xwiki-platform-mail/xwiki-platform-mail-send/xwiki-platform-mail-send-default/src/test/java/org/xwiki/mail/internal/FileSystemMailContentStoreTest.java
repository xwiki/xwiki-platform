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
package org.xwiki.mail.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailStoreException;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FileSystemMailContentStore}.
 *
 * @version $Id$
 * @since 6.4M3
 */
@ComponentTest
public class FileSystemMailContentStoreTest
{
    // Passed at the Maven level in the pom.xml file.
    private static final String TEMPORARY_DIRECTORY =
        System.getProperty("temporaryDirectory", System.getProperty("java.io.tmpdir"));

    @InjectMockComponents
    private FileSystemMailContentStore store;

    @BeforeEach
    public void deleteMailStore() throws Exception
    {
        // Delete content of the mails store directory
        FileUtils.deleteDirectory(
            new File(TEMPORARY_DIRECTORY, this.store.ROOT_DIRECTORY));
    }

    @BeforeComponent
    public void registerMockComponents(MockitoComponentManager componentManager) throws Exception
    {
        Environment environment = componentManager.registerMockComponent(Environment.class);
        when(environment.getPermanentDirectory()).thenReturn(new File(TEMPORARY_DIRECTORY));
    }

    @Test
    public void saveMessage() throws Exception
    {
        String batchId = UUID.randomUUID().toString();

        ExtendedMimeMessage message = new ExtendedMimeMessage();
        message.setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit");

        this.store.save(batchId, message);
        String messageId = message.getMessageID();

        File tempDir = new File(TEMPORARY_DIRECTORY);
        File batchDirectory =
            new File(new File(tempDir, this.store.ROOT_DIRECTORY), URLEncoder.encode(batchId, "UTF-8"));
        File messageFile = new File(batchDirectory, URLEncoder.encode(message.getUniqueMessageId(), "UTF-8"));
        InputStream in = new FileInputStream(messageFile);
        String messageContent = IOUtils.toString(in);

        assertTrue(messageContent.contains("Message-ID: " + messageId));
        assertTrue(messageContent.contains("Lorem ipsum dolor sit amet, consectetur adipiscing elit"));
    }

    @Test
    public void saveMessageWithCustomMessageId() throws Exception
    {
        String batchId = UUID.randomUUID().toString();
        String mimeMessageId = "<1128820400.0.1419205781342.JavaMail.contact@xwiki.org>";

        ExtendedMimeMessage message = new ExtendedMimeMessage();
        message.setMessageId(mimeMessageId);
        message.setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit");

        this.store.save(batchId, message);

        File tempDir = new File(TEMPORARY_DIRECTORY);
        File batchDirectory =
            new File(new File(tempDir, this.store.ROOT_DIRECTORY), URLEncoder.encode(batchId, "UTF-8"));
        File messageFile = new File(batchDirectory, URLEncoder.encode(message.getUniqueMessageId(), "UTF-8"));
        InputStream in = new FileInputStream(messageFile);
        String messageContent = IOUtils.toString(in, "UTF-8");

        assertTrue(messageContent.contains("Message-ID: " + mimeMessageId));
        assertTrue(messageContent.contains("Lorem ipsum dolor sit amet, consectetur adipiscing elit"));
    }

    @Test
    public void saveMessageWhenInstableCustomMessageID() throws Exception
    {
        String batchId = UUID.randomUUID().toString();
        String mimeMessageId = "<1128820400.0.1419205781342.JavaMail.contact@xwiki.org>";

        ExtendedMimeMessage message = new ExtendedMimeMessage();
        message.setHeader("Message-ID", mimeMessageId);
        message.setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit");

        this.store.save(batchId, message);

        File tempDir = new File(TEMPORARY_DIRECTORY);
        File batchDirectory =
            new File(new File(tempDir, this.store.ROOT_DIRECTORY), URLEncoder.encode(batchId, "UTF-8"));
        File messageFile = new File(batchDirectory, URLEncoder.encode(message.getUniqueMessageId(), "UTF-8"));
        InputStream in = new FileInputStream(messageFile);
        String messageContent = IOUtils.toString(in, "UTF-8");

        assertTrue(messageContent.contains("Message-ID: " + message.getMessageID()));
        assertTrue(messageContent.contains("Lorem ipsum dolor sit amet, consectetur adipiscing elit"));
    }

    @Test
    public void saveMessageThrowsMailStoreExceptionWhenError(ComponentManager componentManager) throws Exception
    {
        Environment environment = componentManager.getInstance(Environment.class);
        when(environment.getPermanentDirectory()).thenReturn(new File(TEMPORARY_DIRECTORY));

        String batchId = UUID.randomUUID().toString();
        String messageId = "ar1vm0Wca42E/dDn3dsH8ogs3/s=";

        ExtendedMimeMessage message = mock(ExtendedMimeMessage.class);
        when(message.getUniqueMessageId()).thenReturn(messageId);

        when(message.getContent()).thenReturn("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        doThrow(new IOException()).when(message).writeTo(any(OutputStream.class));

        Throwable exception = assertThrows(MailStoreException.class, () -> {
            this.store.save(batchId, message);
        });
        assertTrue(exception.getMessage().startsWith(
            "Failed to save message (id [" + messageId + "], batch id [" + batchId + "]) into file"));
    }

    @Test
    public void loadMessage() throws Exception
    {
        String batchId = UUID.randomUUID().toString();
        String messageId = "ar1vm0Wca42E/dDn3dsH8ogs3/s=";
        String mimeMessageId = "<1128820400.0.1419205781342.JavaMail.contact@xwiki.org>";

        File tempDir = new File(TEMPORARY_DIRECTORY);
        File batchDirectory =
            new File(new File(tempDir, this.store.ROOT_DIRECTORY), URLEncoder.encode(batchId,"UTF-8"));
        batchDirectory.mkdirs();
        File messageFile = new File(batchDirectory, URLEncoder.encode(messageId,"UTF-8"));
        messageFile.createNewFile();

        String newLine = System.getProperty("line.separator");

        try (FileWriter fileWriter = new FileWriter(messageFile, true)) {
            // Unique string is <hashcode>.<id>.<currentTime>.JavaMail.<suffix>
            fileWriter.append("Message-ID: " + mimeMessageId + newLine);
            fileWriter.append("MIME-Version: 1.0" + newLine);
            fileWriter.append("Content-Type: text/plain; charset=us-ascii" + newLine);
            fileWriter.append("Content-Transfer-Encoding: 7bit" + newLine + newLine);
            fileWriter.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        }

        Session session = Session.getInstance(new Properties());
        MimeMessage message = this.store.load(session, batchId, messageId);

        assertEquals(mimeMessageId, message.getMessageID());
        assertEquals("Lorem ipsum dolor sit amet, consectetur adipiscing elit", message.getContent());
    }

    @Test
    public void loadMessageThrowsMailStoreExceptionWhenError()
    {
        String batchId = UUID.randomUUID().toString();
        String messageId = "ar1vm0Wca42E/dDn3dsH8ogs3/s=";
        Session session = Session.getInstance(new Properties());

        Throwable exception = assertThrows(MailStoreException.class, () -> {
           this.store.load(session, batchId, messageId);
        });
        assertTrue(exception.getMessage().startsWith(
            "Failed to load message (id [" + messageId + "], batch id [" + batchId + "]) from file"));
    }

    @Test
    public void deleteMessage(ComponentManager componentManager) throws Exception
    {
        Environment environment = componentManager.getInstance(Environment.class);
        when(environment.getPermanentDirectory()).thenReturn(new File(TEMPORARY_DIRECTORY));

        String batchId = UUID.randomUUID().toString();
        String messageId = "ar1vm0Wca42E/dDn3dsH8ogs3/s=";

        File tempDir = new File(TEMPORARY_DIRECTORY);
        File batchDirectory =
            new File(new File(tempDir, this.store.ROOT_DIRECTORY), URLEncoder.encode(batchId, "UTF-8"));
        batchDirectory.mkdirs();
        File messageFile = new File(batchDirectory, URLEncoder.encode(messageId, "UTF-8"));
        messageFile.createNewFile();

        this.store.delete(batchId, messageId);

        assertTrue(!messageFile.exists());
    }
}
