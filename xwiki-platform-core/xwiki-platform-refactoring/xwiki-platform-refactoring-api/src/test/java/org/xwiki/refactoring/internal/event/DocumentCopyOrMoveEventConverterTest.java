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
package org.xwiki.refactoring.internal.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.EventConverterManager;
import org.xwiki.observation.remote.internal.converter.DefaultEventConverterManager;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link DocumentCopyOrMoveEventConverter};
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({DocumentCopyOrMoveEventConverter.class, DefaultEventConverterManager.class})
class DocumentCopyOrMoveEventConverterTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Test
    void convert() throws Exception
    {
        EventConverterManager eventConverterManager = this.componentManager.getInstance(EventConverterManager.class);

        // local -> remote

        LocalEventData localEvent = new LocalEventData();
        localEvent.setEvent(new DocumentRenamedEvent(new DocumentReference("wiki", "space", "source"),
            new DocumentReference("wiki", "space", "target")));
        MoveRequest moveRequest = new MoveRequest();
        moveRequest.setAutoRedirect(false);
        localEvent.setData(moveRequest);

        RemoteEventData remoteEvent = eventConverterManager.createRemoteEventData(localEvent);

        // serialize/unserialize
        ByteArrayOutputStream sos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(sos);
        oos.writeObject(remoteEvent);
        ByteArrayInputStream sis = new ByteArrayInputStream(sos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(sis);
        remoteEvent = (RemoteEventData) ois.readObject();

        // remote -> local

        LocalEventData localEvent2 = eventConverterManager.createLocalEventData(remoteEvent);

        assertTrue(localEvent2.getData() instanceof MoveRequest);
        assertFalse(((MoveRequest) localEvent2.getData()).isAutoRedirect());
    }
}
