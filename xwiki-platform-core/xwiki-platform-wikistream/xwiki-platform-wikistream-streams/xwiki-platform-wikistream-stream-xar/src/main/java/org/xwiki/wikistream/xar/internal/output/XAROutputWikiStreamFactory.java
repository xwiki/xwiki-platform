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
package org.xwiki.wikistream.xar.internal.output;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.wikistream.internal.output.AbstractBeanOutputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;
import org.xwiki.wikistream.xar.internal.XARFilter;
import org.xwiki.wikistream.xar.internal.XARWikiStreamUtils;
import org.xwiki.wikistream.xar.output.XAROutputProperties;

/**
 * Generate XAR package from WikiStream events.
 * 
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Named(XARWikiStreamUtils.ROLEHINT)
@Singleton
public class XAROutputWikiStreamFactory extends AbstractBeanOutputWikiStreamFactory<XAROutputProperties, XARFilter>
{
    public XAROutputWikiStreamFactory()
    {
        super(WikiStreamType.XWIKI_XAR_11);

        setName("XAR output stream");
        setDescription("Write XAR package from wiki events.");
    }
}
