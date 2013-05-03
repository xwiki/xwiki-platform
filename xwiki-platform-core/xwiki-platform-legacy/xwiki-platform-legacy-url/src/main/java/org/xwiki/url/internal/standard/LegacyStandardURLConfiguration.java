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

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

import com.xpn.xwiki.XWikiContext;

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

    private String getLegacyValue(String oldKey, String oldDefault)
    {
        String result = null;
        ExecutionContext econtext = this.execution.getContext();
        if (econtext != null) {
            XWikiContext xcontext = (XWikiContext) econtext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
            if (xcontext != null) {
                result = xcontext.getWiki().Param(oldKey, oldDefault);
            }
        }
        return result;
    }
}
