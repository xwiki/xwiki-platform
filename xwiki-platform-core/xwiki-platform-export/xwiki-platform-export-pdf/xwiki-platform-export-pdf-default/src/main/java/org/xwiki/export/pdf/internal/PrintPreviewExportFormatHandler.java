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
package org.xwiki.export.pdf.internal;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Request;
import org.xwiki.export.ExportFormatHandler;

import com.xpn.xwiki.XWikiContext;

/**
 * Handles the PDF export print preview, i.e. the export action requests with {@code format=html-print}. The print
 * preview is loaded, with a GET request, by the web browser that generates the PDF (either the user's web browser, in
 * an iframe, or a headless web browser on the server side). It doesn't perform any expensive export operation: it
 * only displays the document rendering results computed by the PDF export job, using the template specified on the
 * request.
 *
 * @version $Id$
 * @since 18.4.7
 * @since 18.9.0RC1
 */
@Component
@Named("html-print")
@Singleton
public class PrintPreviewExportFormatHandler implements ExportFormatHandler
{
    @Override
    public boolean validate(Request request)
    {
        // The print preview can be loaded with any request method, since it doesn't perform any expensive operation.
        return true;
    }

    @Override
    public String export(XWikiContext context)
    {
        // The print preview URL specifies the template to render (xpage) and the sheet to apply, so this is only the
        // fallback template.
        return "get";
    }
}
