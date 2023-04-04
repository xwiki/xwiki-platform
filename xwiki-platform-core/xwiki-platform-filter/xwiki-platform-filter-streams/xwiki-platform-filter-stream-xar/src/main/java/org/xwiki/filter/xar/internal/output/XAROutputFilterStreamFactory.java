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
package org.xwiki.filter.xar.internal.output;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.output.AbstractBeanOutputFilterStreamFactory;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.filter.xar.internal.XARFilter;
import org.xwiki.filter.xar.internal.XARFilterUtils;
import org.xwiki.filter.xar.output.XAROutputProperties;

import static org.xwiki.filter.xar.internal.XARFilterUtils.*;
import static org.xwiki.filter.xar.internal.XARFilterUtils.ROLEHINT_16;

/**
 * Generate XAR package from FilterStream events.
 * 
 * @version $Id$
 * @since 6.2M1
 */
@Component(hints = {
    ROLEHINT_16,
    ROLEHINT_15,
    ROLEHINT_14,
    ROLEHINT_13,
    ROLEHINT_12,
    ROLEHINT_11
})
@Singleton
public class XAROutputFilterStreamFactory extends AbstractBeanOutputFilterStreamFactory<XAROutputProperties, XARFilter>
{
    public XAROutputFilterStreamFactory()
    {
        super(FilterStreamType.XWIKI_XAR_CURRENT);

        setName("XAR output stream");
        setDescription("Write XAR package from wiki events.");
    }
}
