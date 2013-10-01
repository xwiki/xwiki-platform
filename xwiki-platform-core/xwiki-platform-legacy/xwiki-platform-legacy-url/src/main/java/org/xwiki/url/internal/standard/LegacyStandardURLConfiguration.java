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
package org.xwiki.url.internal.standard;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.web.XWikiConfigurationService;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Replaces {@link DefaultStandardURLConfiguration} with fallbacks to configuration properties defined in the
 * {@code xwiki.cfg} configuration file.
 *
 * @version $Id$
 * @since 5.1M1
 */
@Component
@Singleton
public class LegacyStandardURLConfiguration extends DefaultStandardURLConfiguration
{
    @Inject
    private Execution execution;

    @Override
    public boolean isPathBasedMultiWiki()
    {
        return super.isPathBasedMultiWiki("1".equals(getLegacyValue("xwiki.virtual.usepath", "1")));
    }

    @Override
    public String getWikiPathPrefix()
    {
        return super.getWikiPathPrefix(getLegacyValue("xwiki.virtual.usepath.servletpath", "wiki"));
    }

    @Override
    public String getEntityPathPrefix()
    {
        String prefix = super.getWikiPathPrefix(getLegacyValue("xwiki.defaultservletpath", "bin"));

        // Remove potential trailing "/" since the documentation in xwiki.cfg says it should contain a trailing "/" but
        // getEntityPathPrefix should return the prefix without "/"...
        prefix = StringUtils.removeEnd(prefix, "/");

        return prefix;
    }

    @Override
    public WikiNotFoundBehavior getWikiNotFoundBehavior()
    {
        WikiNotFoundBehavior legacyBehavior = WikiNotFoundBehavior.DISPLAY_ERROR;

        String legacyValue = getLegacyValue("xwiki.virtual.failOnWikiDoesNotExist", "0");

        if (!"1".equals(legacyValue)) {
            legacyBehavior = WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI;
        }

        return super.getWikiNotFoundBehavior(legacyBehavior);
    }

    private String getLegacyValue(String oldKey, String oldDefault)
    {
        String result = null;

        // Hack!
        // Get the ServletContext from the HTTP Session in case we're in a Servlet environment
        ExecutionContext econtext = this.execution.getContext();
        if (econtext != null) {
            XWikiContext xcontext = (XWikiContext) econtext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
            if (xcontext != null) {
                XWikiRequest request = xcontext.getRequest();
                if (request != null) {
                    result = XWikiConfigurationService.getProperty(oldKey, oldDefault,
                        request.getSession().getServletContext());
                }
            }
        }

        return result;
    }
}
