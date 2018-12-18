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
import java.net.URLEncoder;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private static final String TEMPORARY_DIRECTORY = "target/"
        + SerializedFilesMimeMessageIteratorTest.class.getSimpleName();

    @Rule
    public LogRule logRule = new LogRule()
    {
        {
            record(LogLevel.ERROR);
            recordLoggingForType(SerializedFilesMimeMessageIterator.class);
        }
    };

    private String batchId;

    private File batchDirectory;

    @Before
    public void setUp() throws Exception
    {
        this.batchId = UUID.randomUUID().toString();

        File tempDir = new File(TEMPORARY_DIRECTORY);
        this.batchDirectory = new File(new File(tempDir, "mailstore"), URLEncoder.encode(this.batchId, "UTF-8"));
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
        String mailID = "<1128820400.0.1419205781342.JavaMail.contact@xwiki.org>";
        createSerializedMessage(mailID);

        SerializedFilesMimeMessageIterator iterator = new SerializedFilesMimeMessageIterator(this.batchId,
            Collections.<String, Object>emptyMap(), componentManager);

        // Remove the file before next() is called to generate the error
        File messageFile = new File(this.batchDirectory, URLEncoder.encode(mailID, "UTF-8"));
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
        String mailID1 = "<1128820400.0.1419205781342.JavaMail.contact@xwiki.org>";
        String mailID2 = "<1128820400.1.1419205781342.JavaMail.contact@xwiki.org>";
        String mailID3 = "<1128820400.2.1419205781342.JavaMail.contact@xwiki.org>";

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
        listID.add(mailID1);
        listID.add(mailID2);
        listID.add(mailID3);

        assertTrue(iterator.hasNext());
        MimeMessage message1 = iterator.next();
        assertTrue(listID.contains(message1.getMessageID()));
        listID.remove(message1.getMessageID());

        assertTrue(iterator.hasNext());
        MimeMessage message2 = iterator.next();
        assertTrue(listID.contains(message2.getMessageID()));
        listID.remove(message1.getMessageID());

        assertTrue(iterator.hasNext());
        MimeMessage message3 = iterator.next();
        assertTrue(listID.contains(message2.getMessageID()));
        listID.remove(message3.getMessageID());

        assertFalse(iterator.hasNext());
    }

    private void createSerializedMessage(String messageId) throws IOException
    {
        File messageFile = new File(this.batchDirectory, URLEncoder.encode(messageId, "UTF-8"));
        messageFile.createNewFile();
        String newLine = System.getProperty("line.separator");

        FileWriter fileWriter = new FileWriter(messageFile, true);
        // Unique string is <hashcode>.<id>.<currentTime>.JavaMail.<suffix>
        fileWriter.append("Message-ID: " + messageId + newLine);
        fileWriter.append("MIME-Version: 1.0" + newLine);
        fileWriter.append("Content-Type: text/plain; charset=us-ascii" + newLine);
        fileWriter.append("Content-Transfer-Encoding: 7bit" + newLine);
        fileWriter.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        fileWriter.close();
    }
}
