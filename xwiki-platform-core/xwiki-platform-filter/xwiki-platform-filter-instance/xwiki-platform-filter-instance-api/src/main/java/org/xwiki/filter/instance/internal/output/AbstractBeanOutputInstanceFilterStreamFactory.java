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
package org.xwiki.filter.instance.internal.output;

import org.xwiki.component.phase.Initializable;
import org.xwiki.filter.instance.output.OutputInstanceFilterStreamFactory;
import org.xwiki.filter.output.AbstractBeanOutputFilterStreamFactory;
import org.xwiki.filter.type.FilterStreamType;

/**
 * @param <P> the type of the properties bean
 * @param <F> the type of the filter
 * @version $Id$
 * @since 6.2M1
 */
public abstract class AbstractBeanOutputInstanceFilterStreamFactory<P, F> extends
    AbstractBeanOutputFilterStreamFactory<P, F> implements OutputInstanceFilterStreamFactory, Initializable
{
    /**
     * @param id the id of the {@link OutputInstanceFilterStreamFactory}
     */
    public AbstractBeanOutputInstanceFilterStreamFactory(String id)
    {
        super(new FilterStreamType(FilterStreamType.XWIKI_INSTANCE.getType(),
            FilterStreamType.XWIKI_INSTANCE.getDataFormat() + "+" + id));
    }
}
