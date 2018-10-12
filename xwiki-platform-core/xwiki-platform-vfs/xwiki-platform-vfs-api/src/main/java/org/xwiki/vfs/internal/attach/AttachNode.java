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
import java.io.OutputStream;
import java.net.URI;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.xpn.xwiki.doc.XWikiAttachment;

import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truecommons.cio.IoEntry;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAbstractNode;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsNodeName;
import net.java.truevfs.kernel.spec.FsReadOnlyFileSystemException;
import net.java.truevfs.kernel.spec.sl.IoBufferPoolLocator;

import static net.java.truecommons.cio.Entry.Access.READ;
import static net.java.truecommons.cio.Entry.Access.WRITE;
import static net.java.truecommons.cio.Entry.Size.DATA;
import static net.java.truecommons.cio.Entry.Type.FILE;
import static net.java.truevfs.kernel.spec.FsAccessOptions.NONE;

/**
 * Represents a TrueVFS Node inside an archive located in an attachment in a wiki page.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Immutable
public class AttachNode extends FsAbstractNode implements IoEntry<AttachNode>
{
    private final URI uri;

    private final AttachController controller;

    private final String name;

    private XWikiModelNode xwikiModelNode;

    AttachNode(final AttachController controller, final FsNodeName name)
    {
        assert null != controller;
        this.controller = controller;
        this.name = name.toString();
        this.uri = controller.resolve(name).getUri();
        this.xwikiModelNode = new XWikiModelNode(controller, name);
    }

    IoBufferPool getPool()
    {
        return IoBufferPoolLocator.SINGLETON.get();
    }

    protected InputStream newInputStream() throws IOException
    {
        // Return the attachment as an input stream
        try {
            return this.xwikiModelNode.getAttachment().getContentInputStream(this.xwikiModelNode.getXWikiContext());
        } catch (Exception e) {
            throw new IOException(String.format(
                "Failed to get attachment content for attachment [%s] in URI [%s]", this.name, this.uri), e);
        }
    }

    protected OutputStream newOutputStream() throws IOException
    {
        throw new FsReadOnlyFileSystemException(this.controller.getMountPoint());
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public BitField<Type> getTypes()
    {
        // All Attach Driver Nodes are of type File, except if the Node doesn't exist, in which case we should return
        // NO_TYPES
        return this.xwikiModelNode.hasAttachment() ? FILE_TYPE : NO_TYPES;
    }

    @Override
    public boolean isType(final Type type)
    {
        return type == FILE && getTypes().is(FILE);
    }

    @Override
    public long getSize(final Size type)
    {
        if (DATA != type) {
            return UNKNOWN;
        }
        // Get the size of the attachment in bytes
        try {
            XWikiAttachment attachment = this.xwikiModelNode.getAttachment();
            return attachment.getContentLongSize(this.xwikiModelNode.getXWikiContext());
        } catch (Exception e) {
            return UNKNOWN;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public long getTime(Access type)
    {
        if (WRITE != type) {
            return UNKNOWN;
        }
        // Get the last modified time for the attachment
        try {
            XWikiAttachment attachment = this.xwikiModelNode.getAttachment();
            return attachment.getDate().getTime();
        } catch (IOException e) {
            return UNKNOWN;
        }
    }

    @Override
    public Boolean isPermitted(final Access type, final Entity entity)
    {
        if (READ != type) {
            return null;
        }
        return true;
    }

    @Override
    public Set<String> getMembers()
    {
        return null;
    }

    @Override
    public final InputSocket<AttachNode> input()
    {
        return input(NONE);
    }

    /**
     * @param options the options for accessing the file system node
     * @return An input socket for reading this entry
     */
    protected InputSocket<AttachNode> input(BitField<FsAccessOption> options)
    {
        return new AttachInputSocket(options, this);
    }

    @Override
    public final OutputSocket<AttachNode> output()
    {
        return output(NONE, null);
    }

    protected OutputSocket<AttachNode> output(BitField<FsAccessOption> options, Entry template)
    {
        return new AttachOutputSocket(options, this, template);
    }
}
