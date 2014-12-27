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
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.environment.Environment;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultMailStore}.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class DefaultMailStoreTest
{
    // Passed at the Maven level in the pom.xml file.
    private static final String TEMPORARY_DIRECTORY =
        System.getProperty("temporaryDirectory", System.getProperty("java.io.tmpdir"));

    @Rule
    public MockitoComponentMockingRule<DefaultMailStore> mocker =
        new MockitoComponentMockingRule<>(DefaultMailStore.class);

    @Before
    public void deleteMailStore() throws Exception
    {
        // Delete content of the mails store directory
        FileUtils.deleteDirectory(
            new File(TEMPORARY_DIRECTORY, this.mocker.getComponentUnderTest().ROOT_DIRECTORY));
    }

    @Test
    public void saveMessage() throws Exception
    {
        Environment environment = this.mocker.getInstance(Environment.class);
        when(environment.getPermanentDirectory()).thenReturn(new File(TEMPORARY_DIRECTORY));

        String batchID = UUID.randomUUID().toString();
        String mailID = UUID.randomUUID().toString();

        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        message.setHeader("X-BatchID", batchID);
        message.setHeader("X-MailID", mailID);
        message.setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit");

        this.mocker.getComponentUnderTest().save(message);

        File tempDir = new File(TEMPORARY_DIRECTORY);
        File batchDirectory =
            new File(new File(tempDir, this.mocker.getComponentUnderTest().ROOT_DIRECTORY), batchID);
        File messageFile = new File(batchDirectory, mailID);
        InputStream in = new FileInputStream(messageFile);
        String messageContent = IOUtils.toString(in);

        assertTrue(messageContent.contains("X-BatchID: " + batchID));
        assertTrue(messageContent.contains("X-MailID: " + mailID));
        assertTrue(messageContent.contains("Lorem ipsum dolor sit amet, consectetur adipiscing elit"));
    }

    @Test
    public void loadMessage() throws Exception
    {
        Environment environment = this.mocker.getInstance(Environment.class);
        when(environment.getPermanentDirectory()).thenReturn(new File(TEMPORARY_DIRECTORY));

        String batchID = UUID.randomUUID().toString();
        String mailID = UUID.randomUUID().toString();

        File tempDir = new File(TEMPORARY_DIRECTORY);
        File batchDirectory =
            new File(new File(tempDir, this.mocker.getComponentUnderTest().ROOT_DIRECTORY), batchID);
        batchDirectory.mkdirs();
        File messageFile = new File(batchDirectory, mailID);
        messageFile.createNewFile();

        String newLine = System.getProperty("line.separator");

        FileWriter fileWriter = new FileWriter(messageFile, true);
        // Unique string is <hashcode>.<id>.<currentTime>.JavaMail.<suffix>
        fileWriter.append("Message-ID: <1128820400.0.1419205781342.JavaMail.contact@xwiki.org>" + newLine);
        fileWriter.append("MIME-Version: 1.0" + newLine);
        fileWriter.append("Content-Type: text/plain; charset=us-ascii" + newLine);
        fileWriter.append("Content-Transfer-Encoding: 7bit" + newLine);
        fileWriter.append("X-MailID: " + mailID + newLine);
        fileWriter.append("X-BatchID: " + batchID + newLine + newLine);
        fileWriter.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        fileWriter.close();

        Session session = Session.getInstance(new Properties());
        MimeMessage message = this.mocker.getComponentUnderTest().load(session, batchID, mailID);

        assertEquals(batchID, message.getHeader("X-BatchID", null));
        assertEquals(mailID, message.getHeader("X-MailID", null));
        assertEquals("Lorem ipsum dolor sit amet, consectetur adipiscing elit", message.getContent());
    }
}