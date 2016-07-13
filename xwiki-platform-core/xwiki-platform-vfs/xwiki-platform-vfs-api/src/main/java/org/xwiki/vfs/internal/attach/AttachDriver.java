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

import org.xwiki.component.manager.ComponentManager;

import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsModel;

/**
 * TrueVFS Driver for archives attached to wiki pages as attachments.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class AttachDriver extends FsDriver
{
    private ComponentManager componentManager;

    /**
     * @param componentManager the Component Manager used to retrieve all other components since a TrueVFS driver is
     *        not a Component and thus cannot have dependency-injection. Thus we need to pass the Component Manager.
     */
    public AttachDriver(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    @Override
    public FsController newController(FsManager manager, FsModel model, FsController parent)
    {
        return new AttachController(this, model, this.componentManager);
    }
}
