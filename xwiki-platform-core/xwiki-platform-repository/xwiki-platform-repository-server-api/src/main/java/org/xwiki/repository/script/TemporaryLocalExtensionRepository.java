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
package org.xwiki.repository.script;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.extension.repository.internal.local.DefaultLocalExtensionRepository;
import org.xwiki.extension.repository.internal.local.LocalExtensionStorage;

/**
 * @version $Id$
 */
@Component
@Named("tmp")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class TemporaryLocalExtensionRepository extends DefaultLocalExtensionRepository
{
    @Inject
    private Environment environment;

    @Inject
    private ComponentManager componentManager;

    private Path path;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.path = Files.createTempDirectory(this.environment.getTemporaryDirectory().toPath(), "local");
            this.storage = new LocalExtensionStorage(this, path.toFile(), this.componentManager);
        } catch (Exception e) {
            throw new InitializationException("Failed to intialize local extension storage", e);
        }
    }

    /**
     * @return the path
     */
    public Path getPath()
    {
        return this.path;
    }

    /**
     * Dispose.
     * 
     * @throws IOException when failing to dispose
     */
    public void dispose() throws IOException
    {
        Files.delete(this.path);
    }
}
