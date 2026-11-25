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
package org.xwiki.livedata.internal.livetable;

import java.util.Map;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.internal.LiveDataSourceContentTrust;

/**
 * Trusts the content of the {@code liveTable} source only when its entries are computed by the standard
 * {@code XWiki.LiveTableResults} page.
 * <p>
 * The {@code resultPage} and {@code template} source parameters make the source read its entries, HTML included, from
 * a page or a template chosen by the Live Data author, so the content is not trusted as soon as one of them is set,
 * whatever its value.
 *
 * @version $Id$
 * @since 19.0.0RC1
 * @since 18.10.0
 * @since 18.4.9
 * @since 17.10.16
 * @since 16.10.20
 */
@Component
@Named("liveTable")
@Singleton
public class LiveTableLiveDataSourceContentTrust implements LiveDataSourceContentTrust
{
    @Override
    public boolean isContentTrusted(LiveDataQuery.Source source)
    {
        Map<String, Object> parameters = source.getParameters();
        return !parameters.containsKey(LiveTableRequestHandler.RESULT_PAGE)
            && !parameters.containsKey(LiveTableRequestHandler.TEMPLATE);
    }
}
