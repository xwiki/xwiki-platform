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
package org.xwiki.vfs.internal.attach;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;

import javax.annotation.concurrent.NotThreadSafe;

import net.java.truecommons.cio.AbstractInputSocket;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.IoBuffer;
import net.java.truecommons.cio.IoSockets;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.io.ReadOnlyChannel;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;

/**
 * TrueVFS input socket for the Attach Driver.
 *
 * @version $Id$
 * @since 7.4M2
 */
@NotThreadSafe
public class AttachInputSocket extends AbstractInputSocket<AttachNode>
{
    private final AttachNode entry;

    AttachInputSocket(BitField<FsAccessOption> options, AttachNode entry)
    {
        this.entry = entry;
    }

    @Override
    public AttachNode target()
    {
        return entry;
    }

    @Override
    public InputStream stream(final OutputSocket<? extends Entry> peer) throws IOException
    {
        return entry.newInputStream();
    }

    @Override
    public SeekableByteChannel channel(final OutputSocket<? extends Entry> peer) throws IOException
    {
        final IoBuffer buffer = entry.getPool().allocate();
        try {
            IoSockets.copy(entry.input(), buffer.output());
        } catch (final Exception ex) {
            try {
                buffer.release();
            } catch (final Exception ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
        final class BufferReadOnlyChannel extends ReadOnlyChannel
        {
            private boolean closed;

            BufferReadOnlyChannel() throws IOException
            {
                super(buffer.input().channel(peer));
            }

            @Override
            public void close() throws IOException
            {
                if (!closed) {
                    channel.close();
                    closed = true;
                    buffer.release();
                }
            }
        }
        return new BufferReadOnlyChannel();
    }
}
