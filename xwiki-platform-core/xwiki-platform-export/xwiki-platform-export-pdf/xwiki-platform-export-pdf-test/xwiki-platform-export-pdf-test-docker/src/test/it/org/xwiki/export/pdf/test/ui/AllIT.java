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
package org.xwiki.export.pdf.test.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI tests for the PDF Export feature.
 *
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@UITest(
    extraJARs = {
        "org.xwiki.platform:xwiki-platform-resource-temporary",
        // Code macro highlighting works only if Jython is a core extension. It's not enough to use language=none in our
        // test because we want to reproduce a bug in Paged.js where white-space between highlighted tokens is lost.
        // TODO: Remove when https://jira.xwiki.org/browse/XWIKI-17972 is fixed
        "org.python:jython-slim",
        // The image plugin that performs the server-side image resize is not registered until the server is restarted
        // so we need to make it a core extension.
        "org.xwiki.platform:xwiki-platform-image-processing-plugin"
    },
    resolveExtraJARs = true,
    // We need the Office server because we want to be able to test how the Office macro is exported to PDF.
    office = true,
    properties = {
        // Starting or stopping the Office server requires PR (for the current user, on the main wiki reference).
        // Enabling debug logs also requires PR.
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern="
            + ".*:(XWiki\\.OfficeImporterAdmin|PDFExportIT\\.EnableDebugLogs)",
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.image.ImagePlugin",
    }
)
@ExtendWith(PDFExportExecutionCondition.class)
class AllIT
{
    @Nested
    @DisplayName("PDF Export Tests")
    class NestedPDFExportIT extends PDFExportIT
    {
    }
}
