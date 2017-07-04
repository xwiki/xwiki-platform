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
package org.xwiki.webjars.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.filesystem.FilesystemExportContext;
import org.xwiki.url.internal.RelativeExtendedURL;

/**
 * Converts a {@link WebJarsResourceReference} into a {@link ExtendedURL} (with the Context Path added) that points to a
 * absolute URL pointing to a file on the local filesystem. This can be used when exporting to HTML for example.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named("filesystem")
@Singleton
public class FilesystemResourceReferenceSerializer
    implements ResourceReferenceSerializer<WebJarsResourceReference, ExtendedURL>
{
    /**
     * Prefix for locating resource files (JavaScript, CSS) in the classloader.
     */
    private static final String WEBJARS_RESOURCE_PREFIX = "META-INF/resources/webjars";

    private static final String PARENT = "..";

    private static final String WEBJAR_PATH = "webjars";

    @Inject
    private Provider<FilesystemExportContext> exportContextProvider;

    @Override
    public ExtendedURL serialize(WebJarsResourceReference reference)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        // Copy the resource from the webjar to the filesystem
        FilesystemExportContext exportContext = this.exportContextProvider.get();
        try {
            FilesystemResourceReferenceCopier copier = new FilesystemResourceReferenceCopier();
            copier.copyResourceFromJAR(WEBJARS_RESOURCE_PREFIX, reference.getResourceName(), WEBJAR_PATH,
                exportContext);

            // If the resource asked is a CSS file, then parse it to look for relative URLs and also save them on the
            // filesystem.
            if (reference.getResourceName().toLowerCase().endsWith("css")) {
                copier.processCSS(WEBJARS_RESOURCE_PREFIX, reference.getResourceName(), WEBJAR_PATH, exportContext);
            }
        } catch (Exception e) {
            throw new SerializeResourceReferenceException(
                String.format("Failed to extract and copy WebJAR resource [%s]", reference.getResourceName()), e);
        }

        List<String> pathSegments = new ArrayList<>();

        // If the webjar URL is computed inside a CSS file then we need to be relative to that CSS's path, i.e only
        // take into account the CSS Parent levels. However if the webjar URL is not inside a CSS we need to take into
        // account the doc parent level.

        // Adjust path depending on where the current doc is stored
        if (exportContext.getCSSParentLevel() == 0) {
            for (int i = 0; i < exportContext.getDocParentLevel(); i++) {
                pathSegments.add(PARENT);
            }
        } else {
            // Adjust path for links inside CSS files (since they need to be relative to the CSS file they're in).
            for (int i = 0; i < exportContext.getCSSParentLevel(); i++) {
                pathSegments.add(PARENT);
            }
        }

        pathSegments.add(WEBJAR_PATH);
        for (String resourceSegment : StringUtils.split(reference.getResourceName(), '/')) {
            pathSegments.add(resourceSegment);
        }

        return new RelativeExtendedURL(pathSegments);
    }
}
