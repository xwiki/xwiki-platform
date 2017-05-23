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
package org.xwiki.notifications.page;

import java.util.concurrent.Callable;

import javax.inject.Provider;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 * @since 9.5RC1
 */
public class VelocityTemplateEvaluator implements Callable<String>
{
    private String content;

    private Provider<XWikiContext> contextProvider;

    /**
     * Constructs a {@link VelocityTemplateEvaluator}.
     *
     * @param contextProvider a reference to the XWikiContext provider component
     * @param content the content of the template that will be evaluated
     */
    public VelocityTemplateEvaluator(Provider<XWikiContext> contextProvider, String content)
    {
        this.contextProvider = contextProvider;
        this.content = content;
    }

    @Override
    public String call() throws Exception
    {
        return contextProvider.get().getWiki().evaluateVelocity(content, "page-notification");
    }
}
