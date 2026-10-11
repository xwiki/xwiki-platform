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
package com.xpn.xwiki.internal.export;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.pdf.impl.FileSystemURLFactory;

/**
 * URL factory used while exporting a wiki page as an office document.
 * <p>
 * Note: We extend {@link FileSystemURLFactory} for convenience. This is just a temporary solution. The entire export
 * code needs to be redesigned.
 *
 * @version $Id$
 * @since 3.1M1
 */
public class OfficeExporterURLFactory extends FileSystemURLFactory
{
    /**
     * OfficeExporterURLFactory constructor.
     */
    public OfficeExporterURLFactory()
    {
    }

    /**
     * @param checkAccess true if the access to linked resources should be verified against the context user
     * @since 18.6.0RC1
     * @since 18.4.3
     * @since 17.10.10
     */
    public OfficeExporterURLFactory(boolean checkAccess)
    {
        super(checkAccess);
    }

    @Override
    public String getURL(URL url, XWikiContext context)
    {
        if (url != null && "file".equals(url.getProtocol())) {
            @SuppressWarnings("unchecked")
            Map<String, File> fileMapping = (Map<String, File>) context.get("pdfexport-file-mapping");
            try {
                File file = new File(url.toURI());
                if (fileMapping.values().contains(file)) {
                    // Embedded files are placed in the same folder as the HTML input file during office conversion.
                    return file.getName();
                }
            } catch (URISyntaxException e) {
                // Shouldn't happen. Ignore.
            }
        }
        return super.getURL(url, context);
    }
}
