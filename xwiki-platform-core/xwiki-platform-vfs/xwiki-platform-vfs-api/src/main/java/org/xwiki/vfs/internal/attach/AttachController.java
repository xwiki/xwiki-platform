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

import org.xwiki.component.manager.ComponentManager;

import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.Entry.Access;
import net.java.truecommons.cio.Entry.Type;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAbstractController;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.FsNodeName;
import net.java.truevfs.kernel.spec.FsNodePath;
import net.java.truevfs.kernel.spec.FsReadOnlyFileSystemException;
import net.java.truevfs.kernel.spec.FsSyncOption;

import static net.java.truecommons.cio.Entry.Access.READ;
import static net.java.truecommons.cio.Entry.Type.FILE;

/**
 * TrueVFS Controller for the Attach driver.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class AttachController extends FsAbstractController
{
    private static final BitField<Access> READ_ONLY = BitField.of(READ);

    private final AttachDriver driver;

    private ComponentManager componentManager;

    AttachController(AttachDriver driver, FsModel model, ComponentManager componentManager)
    {
        super(model);
        this.driver = driver;
        this.componentManager = componentManager;
    }

    private AttachNode newEntry(FsNodeName name)
    {
        return new AttachNode(this, name);
    }

    final FsNodePath resolve(FsNodeName name)
    {
        return getMountPoint().resolve(name);
    }

    @Override
    public FsController getParent()
    {
        return null;
    }

    @Override
    public AttachNode node(BitField<FsAccessOption> options, FsNodeName name) throws IOException
    {
        AttachNode entry = newEntry(name);
        return entry.isType(FILE) ? entry : null;
    }

    @Override
    public void checkAccess(BitField<FsAccessOption> options, FsNodeName name, BitField<Access> types)
        throws IOException
    {
        // We only support READ at the moment
        if (!types.isEmpty() && !READ_ONLY.equals(types)) {
            throw new FsReadOnlyFileSystemException(getMountPoint());
        }
    }

    @Override
    public void setReadOnly(BitField<FsAccessOption> options, FsNodeName name) throws IOException
    {
        // All the Nodes (attachments) are already readonly, no need to set anything!
    }

    @Override
    public boolean setTime(BitField<FsAccessOption> options, FsNodeName name, BitField<Access> types, long value)
        throws IOException
    {
        throw new FsReadOnlyFileSystemException(getMountPoint());
    }

    @Override
    public InputSocket<?> input(BitField<FsAccessOption> options, FsNodeName name)
    {
        return newEntry(name).input(options);
    }

    @Override
    public OutputSocket<?> output(BitField<FsAccessOption> options, FsNodeName name, Entry template)
    {
        return newEntry(name).output(options, template);
    }

    @Override
    public void make(final BitField<FsAccessOption> options, final FsNodeName name, Type type, Entry template)
        throws IOException
    {
        throw new FsReadOnlyFileSystemException(getMountPoint());
    }

    @Override
    public void unlink(BitField<FsAccessOption> options, FsNodeName name) throws IOException
    {
        throw new FsReadOnlyFileSystemException(getMountPoint());
    }

    @Override
    public void sync(BitField<FsSyncOption> options)
    {
        // Empty
    }

    ComponentManager getComponentManager()
    {
        return this.componentManager;
    }
}
