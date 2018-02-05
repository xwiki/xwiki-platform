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

import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.xwiki.container.Container;
import org.xwiki.container.Response;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;
import org.xwiki.tika.internal.TikaUtils;

/**
 * Helper to implement {@link org.xwiki.resource.ResourceReferenceHandler} components that return content to the
 * Container's output stream.
 *
 * @version $Id$
 * @since 7.4M2
 */
public abstract class AbstractContentResourceReferenceHandler extends AbstractResourceReferenceHandler<ResourceType>
{
    @Inject
    private Container container;

    protected void serveResource(String resourceName, InputStream resourceStream)
        throws ResourceReferenceHandlerException
    {
        // Make sure the resource stream supports mark & reset which is needed in order be able to detect the
        // content type without affecting the stream (Tika may need to read a few bytes from the start of the
        // stream, in which case it will mark & reset the stream).
        //
        // Note that even though the stream returned by TrueVFS returns true for markSupported() in practice it
        // doesn't! Thus we need to wrap the stream to make it support mark and reset.
        InputStream markResetSupportingStream = new BufferedInputStream(resourceStream);

        try {
            Response response = this.container.getResponse();
            response.setContentType(TikaUtils.detect(markResetSupportingStream, resourceName));
            IOUtils.copy(markResetSupportingStream, response.getOutputStream());
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException(String.format("Failed to read resource [%s]", resourceName), e);
        } finally {
            IOUtils.closeQuietly(markResetSupportingStream);
        }
    }
}
