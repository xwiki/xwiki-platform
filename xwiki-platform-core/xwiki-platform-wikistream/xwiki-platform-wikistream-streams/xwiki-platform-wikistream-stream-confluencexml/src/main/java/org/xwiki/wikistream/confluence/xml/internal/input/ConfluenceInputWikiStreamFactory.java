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
package org.xwiki.wikistream.confluence.xml.internal.input;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.wikistream.confluence.input.ConfluenceInputProperties;
import org.xwiki.wikistream.confluence.xml.internal.ConfluenceFilter;
import org.xwiki.wikistream.internal.input.AbstractBeanInputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;

/**
 * A generic xml output wikistream implementation. This class can be used as a test bench to validate various
 * XMLInputStream wiki parsers.
 * 
 * @version $Id$
 * @since 5.3M2
 */
@Component
@Named(ConfluenceInputWikiStreamFactory.ROLEHINT)
@Singleton
public class ConfluenceInputWikiStreamFactory extends
    AbstractBeanInputWikiStreamFactory<ConfluenceInputProperties, ConfluenceFilter>
{
    /**
     * The role hint of that component.
     */
    public static final String ROLEHINT = "confluence+xml";

    /**
     * The default constructor.
     */
    public ConfluenceInputWikiStreamFactory()
    {
        super(WikiStreamType.CONFLUENCE_XML);

        setName("Confluence XML input stream");
        setDescription("Generates wiki events from Confluence XML package.");
    }
}
