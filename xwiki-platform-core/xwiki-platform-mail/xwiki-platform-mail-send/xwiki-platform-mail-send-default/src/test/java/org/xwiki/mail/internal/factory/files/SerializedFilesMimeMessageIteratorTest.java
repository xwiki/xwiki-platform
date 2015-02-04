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
package org.xwiki.mail.internal.factory.files;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.test.LogRule;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.files.SerializedFilesMimeMessageIterator}.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class SerializedFilesMimeMessageIteratorTest
{
    @Rule
    public LogRule logRule = new LogRule()
    {
        {
            record(LogLevel.ERROR);
            recordLoggingForType(SerializedFilesMimeMessageIterator.class);
        }
    };

    // Passed at the Maven level in the pom.xml file.
    private static final String TEMPORARY_DIRECTORY =
        System.getProperty("temporaryDirectory", System.getProperty("java.io.tmpdir"));

    private String batchId;

    private File batchDirectory;

    @Before
    public void setUp() throws Exception
    {
        this.batchId = UUID.randomUUID().toString();

        File tempDir = new File(TEMPORARY_DIRECTORY);
        this.batchDirectory = new File(new File(tempDir, "mailstore"), this.batchId);
        this.batchDirectory.mkdirs();
    }

    @After
    public void tearDown() throws Exception
    {
        // Delete created messages and directories
        FileUtils.deleteDirectory(
            new File(TEMPORARY_DIRECTORY, this.batchId));
    }

    /**
     * Error that can happen if the file has been locally deleted between the time the time the user executes a
     * resend and the time the Mail Sender Thread reaches that file for processing (i.e. deserializing it).
     */
    @Test
    public void createMessageWhenFileNoLongerExists() throws Exception
    {
        Environment environment = mock(Environment.class);
        when(environment.getPermanentDirectory()).thenReturn(new File(TEMPORARY_DIRECTORY));

        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getInstance(eq(Environment.class))).thenReturn(environment);

        // Create a serialized file before the iterator is initialized
        UUID mailID = UUID.randomUUID();
        createSerializedMessage(mailID);

        SerializedFilesMimeMessageIterator iterator = new SerializedFilesMimeMessageIterator(this.batchId,
            Collections.<String, Object>emptyMap(), componentManager);

        // Remove the file before next() is called to generate the error
        File messageFile = new File(this.batchDirectory, mailID.toString());
        messageFile.delete();

        MimeMessage message = iterator.next();

        // Verify that:
        // 1) the returned message is null since there was an error
        // 2) that the log contains the error
        assertNull(message);
        assertEquals("Failed to create Mime Message", this.logRule.getMessage(0));
    }

    @Test
    public void createMessage() throws Exception
    {
        UUID mailID1 = UUID.randomUUID();
        UUID mailID2 = UUID.randomUUID();
        UUID mailID3 = UUID.randomUUID();

        createSerializedMessage(mailID1);
        createSerializedMessage(mailID2);
        createSerializedMessage(mailID3);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parameters", Collections.EMPTY_MAP);

        Environment environment = mock(Environment.class);
        when(environment.getPermanentDirectory()).thenReturn(new File(TEMPORARY_DIRECTORY));

        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getInstance(eq(Environment.class))).thenReturn(environment);

        SerializedFilesMimeMessageIterator iterator =
            new SerializedFilesMimeMessageIterator(this.batchId, parameters, componentManager);

        ArrayList<String> listID = new ArrayList<>();
        listID.add(mailID1.toString());
        listID.add(mailID2.toString());
        listID.add(mailID3.toString());

        assertTrue(iterator.hasNext());
        MimeMessage message1 = iterator.next();
        assertTrue(listID.contains(message1.getHeader("X-MailID", null)));
        listID.remove(message1.getHeader("X-MailID", null));

        assertTrue(iterator.hasNext());
        MimeMessage message2 = iterator.next();
        assertTrue(listID.contains(message2.getHeader("X-MailID", null)));
        listID.remove(message2.getHeader("X-MailID", null));

        assertTrue(iterator.hasNext());
        MimeMessage message3 = iterator.next();
        assertTrue(listID.contains(message3.getHeader("X-MailID", null)));
        listID.remove(message3.getHeader("X-MailID", null));

        assertFalse(iterator.hasNext());
    }

    private void createSerializedMessage(UUID mailID) throws IOException
    {
        File messageFile = new File(this.batchDirectory, mailID.toString());
        messageFile.createNewFile();
        String newLine = System.getProperty("line.separator");

        FileWriter fileWriter = new FileWriter(messageFile, true);
        // Unique string is <hashcode>.<id>.<currentTime>.JavaMail.<suffix>
        fileWriter.append("Message-ID: <1128820400.0.1419205781342.JavaMail.contact@xwiki.org>" + newLine);
        fileWriter.append("MIME-Version: 1.0" + newLine);
        fileWriter.append("Content-Type: text/plain; charset=us-ascii" + newLine);
        fileWriter.append("Content-Transfer-Encoding: 7bit" + newLine);
        fileWriter.append("X-MailID: " + mailID.toString() + newLine);
        fileWriter.append("X-BatchID: " + this.batchId + newLine + newLine);
        fileWriter.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        fileWriter.close();
    }
}