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
import java.io.OutputStream;

import javax.annotation.concurrent.NotThreadSafe;

import net.java.truecommons.cio.AbstractOutputSocket;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;

/**
 * TrueVFS output socket for the Attach Driver.
 *
 * @version $Id$
 * @since 7.4M2
 */
@NotThreadSafe
public class AttachOutputSocket extends AbstractOutputSocket<AttachNode>
{
    private final AttachNode entry;

    AttachOutputSocket(BitField<FsAccessOption> options, AttachNode entry, Entry template)
    {
        this.entry = entry;
    }

    @Override
    public AttachNode target()
    {
        return entry;
    }

    @Override
    public OutputStream stream(final InputSocket<? extends Entry> peer) throws IOException
    {
        return entry.newOutputStream();
    }
}
