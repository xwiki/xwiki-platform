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
package org.xwiki.resource.temporary.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.resource.ResourceLoader;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;

/**
 * Load the content of a TemporaryResourceReference.
 *
 * @version $Id$
 * @since 14.7RC1
 */
@Component
@Singleton
public class TemporaryResourceLoader implements ResourceLoader<TemporaryResourceReference>
{
    @Inject
    private Logger logger;

    @Inject
    private TemporaryResourceStore temporaryResourceStore;

    @Override
    public InputStream load(TemporaryResourceReference reference)
    {
        InputStream result = null;

        try {
            File file = this.temporaryResourceStore.getTemporaryFile(reference);
            result = new FileInputStream(file);
        } catch (Exception e) {
            // Failed to get the document's content, consider the resource doesn't exist but log a debug error
            // in case it's not normal and we need to debug it.
            this.logger.debug("Failed to get the temporary resource's content for [{}]", reference, e);
        }

        return result;
    }
}
