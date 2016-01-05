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
package org.xwiki.vfs.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.vfs.internal.attach.AttachDriver;

import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TConfig;

/**
 * Register the XWiki custom "attach" VFS driver.
 *
 * @version $Id$
 * @since 7.4.1
 */
@Component
@Named("vfsAttachDriver")
@Singleton
public class VfsAttachDriverRegistrationListener implements EventListener
{
    /**
     * The name of the listener.
     */
    private static final String NAME = "vfsAttachDriver";

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(new ApplicationReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Register our Attach VFS Driver and inject a Component Manager in it.
        TConfig config = TConfig.current();
        // Note: Make sure we add our own Archive Detector to the existing Detector so that all archive formats
        // supported by TrueVFS are handled properly.
        config.setArchiveDetector(new TArchiveDetector(config.getArchiveDetector(), "attach",
            new AttachDriver(this.componentManagerProvider.get())));
    }
}
