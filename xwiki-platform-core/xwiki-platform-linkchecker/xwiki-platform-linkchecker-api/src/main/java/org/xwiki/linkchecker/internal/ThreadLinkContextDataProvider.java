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
package org.xwiki.linkchecker.internal;

import java.util.Collections;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.transformation.linkchecker.LinkContextDataProvider;

/**
 * Adds the Current Thread name to the Link context data since it can be useful to diagnose broken links. In XWiki
 * the thread name contains the request URL and thus it makes it easy to reproduce the generated content containing
 * the broken link.
 *
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("thread")
@Singleton
public class ThreadLinkContextDataProvider implements LinkContextDataProvider
{
    @Override
    public Map<String, Object> getContextData(String linkURL, String contentReference)
    {
        return Collections.<String, Object>singletonMap("thread", Thread.currentThread().getName());
    }
}
