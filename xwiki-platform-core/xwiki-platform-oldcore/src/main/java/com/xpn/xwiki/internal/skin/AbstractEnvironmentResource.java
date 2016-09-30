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
package com.xpn.xwiki.internal.skin;

import java.io.InputStream;

import org.xwiki.environment.Environment;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.skin.ResourceRepository;

/**
 * @version $Id$
 * @since 6.4M1
 */
public abstract class AbstractEnvironmentResource extends AbstractResource<InputSource>
{
    protected Environment environment;

    public AbstractEnvironmentResource(String path, String resourceName, ResourceRepository repository,
        Environment environment)
    {
        super(path, path, resourceName, repository);

        this.environment = environment;
    }

    @Override
    public InputSource getInputSource()
    {
        InputStream inputStream = this.environment.getResourceAsStream(getPath());
        if (inputStream != null) {
            return new DefaultInputStreamInputSource(inputStream, true);
        }

        return null;
    }
}
