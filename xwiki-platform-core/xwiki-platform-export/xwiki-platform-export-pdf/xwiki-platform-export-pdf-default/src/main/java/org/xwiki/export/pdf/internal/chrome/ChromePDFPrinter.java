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
package org.xwiki.export.pdf.internal.chrome;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.xwiki.component.annotation.Component;
import org.xwiki.export.pdf.browser.AbstractBrowserPDFPrinter;
import org.xwiki.export.pdf.browser.BrowserManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Prints the content of a given URL using a (headless) Chrome web browser (that may be running inside a Docker
 * container).
 * 
 * @version $Id$
 * @since 14.7RC1
 */
@Component
@Named("chrome")
@Singleton
public class ChromePDFPrinter extends AbstractBrowserPDFPrinter
{
    @Inject
    @Named("chrome")
    private Provider<BrowserManager> chromeManagerProvider;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    protected BrowserManager getBrowserManager()
    {
        return this.chromeManagerProvider.get();
    }

    @Override
    protected HttpServletRequest getRequest()
    {
        return this.xcontextProvider.get().getRequest();
    }
}
