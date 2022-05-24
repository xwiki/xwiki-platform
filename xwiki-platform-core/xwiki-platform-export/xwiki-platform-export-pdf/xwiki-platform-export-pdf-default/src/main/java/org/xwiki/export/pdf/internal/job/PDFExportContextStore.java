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
package org.xwiki.export.pdf.internal.job;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.concurrent.ContextStore;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

/**
 * Used to save and restore the PDF export cookies, which are needed in order to be able to authenticate the user when
 * communicating with the headless web browser.
 * 
 * @version $Id$
 * @since 14.4.1
 * @since 14.5RC1
 */
@Component
@Singleton
@Named("export/pdf")
public class PDFExportContextStore implements ContextStore
{
    private static final String ENTRY_COOKIES = "export.pdf.cookies";

    @Inject
    private Provider<XWikiContext> writeProvider;

    @Inject
    @Named("readonly")
    private Provider<XWikiContext> readProvider;

    @Override
    public Collection<String> getSupportedEntries()
    {
        return Arrays.asList(ENTRY_COOKIES);
    }

    @Override
    public void restore(Map<String, Serializable> contextStore)
    {
        XWikiContext xcontext = this.writeProvider.get();
        if (contextStore.containsKey(ENTRY_COOKIES)) {
            XWikiRequest request = xcontext.getRequest();
            if (request != null) {
                Cookie[] cookies = (Cookie[]) contextStore.get(ENTRY_COOKIES);
                xcontext.setRequest(new XWikiServletRequestStub(request)
                {
                    @Override
                    public Cookie[] getCookies()
                    {
                        return cookies;
                    }

                    @Override
                    public Cookie getCookie(String cookieName)
                    {
                        return Stream.of(cookies).filter(cookie -> Objects.equals(cookieName, cookie.getName()))
                            .findFirst().orElse(null);
                    }
                });
            }
        }
    }

    @Override
    public void save(Map<String, Serializable> contextStore, Collection<String> entries)
    {
        XWikiContext xcontext = this.readProvider.get();
        if (xcontext != null) {
            XWikiRequest request = xcontext.getRequest();
            if (request != null && entries.contains(ENTRY_COOKIES)) {
                contextStore.put(ENTRY_COOKIES, request.getCookies());
            }
        }
    }
}
