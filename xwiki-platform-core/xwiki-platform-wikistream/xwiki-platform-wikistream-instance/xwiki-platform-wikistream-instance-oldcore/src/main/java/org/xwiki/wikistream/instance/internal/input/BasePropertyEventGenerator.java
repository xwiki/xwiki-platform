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
package org.xwiki.wikistream.instance.internal.input;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.internal.BasePropertyFilter;
import org.xwiki.wikistream.instance.internal.BasePropertyProperties;

import com.xpn.xwiki.objects.BaseProperty;

/**
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
public class BasePropertyEventGenerator extends
    AbstractBeanEntityEventGenerator<BaseProperty, BasePropertyFilter, BasePropertyProperties>
{
    @Override
    public void write(BaseProperty xclassProperty, Object filter, BasePropertyFilter propertyFilter,
        BasePropertyProperties properties) throws WikiStreamException
    {
        // * WikiObjectProperty

        propertyFilter.onWikiObjectProperty(xclassProperty.getName(), xclassProperty.toText(),
            FilterEventParameters.EMPTY);
    }
}
