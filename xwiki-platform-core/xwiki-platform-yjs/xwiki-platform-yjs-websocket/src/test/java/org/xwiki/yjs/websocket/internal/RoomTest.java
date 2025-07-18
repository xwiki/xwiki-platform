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
package org.xwiki.yjs.websocket.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import jakarta.websocket.Session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.google.protobuf.CodedOutputStream;

import static jakarta.websocket.RemoteEndpoint.Basic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.test.LogLevel.WARN;

/**
 * Test of {@link Room}.
 *
 * @version $Id$
 */
@ComponentTest
class RoomTest
{
    @InjectMockComponents
    private Room room;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(WARN);

    @Mock
    private Session session1;

    @Mock
    private Session session2;

    @Mock
    private Basic basic;

    @BeforeEach
    void setUp()
    {
        when(this.session1.getId()).thenReturn("1");
        when(this.session2.getId()).thenReturn("2");
        when(this.session2.getBasicRemote()).thenReturn(this.basic);
    }

    @Test
    void handleMessageRegisterClientId() throws Exception
    {
        assertEquals(Map.of(), this.room.getSessionIds());
        this.room.handleMessage(this.session1, new ByteArrayInputStream(buildRegistrationMessage(123).toByteArray()));
        assertEquals(Map.of(this.session1, 123L), this.room.getSessionIds());
    }

    @Test
    void handleMessageToBroadcast() throws Exception
    {

        this.room.handleMessage(this.session1, new ByteArrayInputStream(buildRegistrationMessage(1).toByteArray()));
        this.room.handleMessage(this.session2, new ByteArrayInputStream(buildRegistrationMessage(2).toByteArray()));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        codedOutputStream.writeUInt64NoTag(1);
        codedOutputStream.writeUInt64NoTag(123);
        codedOutputStream.flush();
        byte[] byteArray = output.toByteArray();
        this.room.handleMessage(this.session1, new ByteArrayInputStream(byteArray));
        verify(this.basic).sendBinary(ByteBuffer.wrap(byteArray));
    }

    @Test
    void handleMessageMalformedMessage() throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        codedOutputStream.flush();
        this.room.handleMessage(this.session1, new ByteArrayInputStream(output.toByteArray()));
        assertEquals("Failed to read a message. "
            + "Cause: [InvalidProtocolBufferException: While parsing a protocol message, the input ended unexpectedly "
            + "in the middle of a field.  This could mean either that the input has been truncated or that an embedded "
            + "message misreported its own length.]", this.logCapture.getMessage(0));
    }

    @Test
    void disconnect() throws Exception
    {
        this.room.handleMessage(this.session1, new ByteArrayInputStream(buildRegistrationMessage(1).toByteArray()));
        this.room.handleMessage(this.session2, new ByteArrayInputStream(buildRegistrationMessage(2).toByteArray()));
        Runnable runnable = mock(Runnable.class);
        this.room.init(runnable);
        assertEquals(Map.of(this.session1, 1L, this.session2, 2L), this.room.getSessionIds());
        this.room.disconnect(this.session1);
        verify(runnable, never()).run();
        verify(this.basic).sendBinary(ByteBuffer.wrap(new byte[] { 4, 1 }));
        assertEquals(Map.of(this.session2, 2L), this.room.getSessionIds());
        this.room.disconnect(this.session2);
        verify(runnable).run();
    }

    private static ByteArrayOutputStream buildRegistrationMessage(int clientId) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        codedOutputStream.writeUInt64NoTag(2);
        codedOutputStream.writeUInt64NoTag(clientId);
        codedOutputStream.flush();
        return output;
    }
}