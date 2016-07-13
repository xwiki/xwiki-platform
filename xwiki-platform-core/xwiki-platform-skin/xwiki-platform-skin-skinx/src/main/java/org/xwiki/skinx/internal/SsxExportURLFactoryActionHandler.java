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
package org.xwiki.skinx.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.url.filesystem.FilesystemExportContext;

import com.xpn.xwiki.web.SsxAction;
import com.xpn.xwiki.web.sx.Extension;
import com.xpn.xwiki.web.sx.SxSource;

/**
 * Handles SSX URL rewriting, by extracting and rendering the SSX content in a file on disk and generating a URL
 * pointing to it.
 *
 * @version $Id$
 * @since 6.2RC1
 */
@Component
@Named("ssx")
@Singleton
public class SsxExportURLFactoryActionHandler extends AbstractSxExportURLFactoryActionHandler
{
    @Override
    protected String getSxPrefix()
    {
        return "ssx";
    }

    @Override
    protected String getFileSuffix()
    {
        return "css";
    }

    @Override public Extension getExtensionType()
    {
        return SsxAction.CSSX;
    }

    @Override
    protected String getContent(SxSource sxSource, FilesystemExportContext exportContext)
    {
        // There can be some calls to URLs inside the SSX content. For example:
        //   background-image: url("$xwiki.getSkinFile('icons/silk/folder_add.png')");
        // In order for these URLs to be resolved correctly they need to be relative to where the CSS file is located
        // on disk. Thus we adjust the CSS path by 3 levels since we're locating the SSX files in "ssx/<space>/<page>".
        exportContext.pushCSSParentLevels(3);

        try {
            return super.getContent(sxSource, exportContext);
        } finally {
            // Put back the CSS level
            exportContext.popCSSParentLevels();
        }
    }
}
