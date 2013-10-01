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
package org.xwiki.wikistream.wikixml.internal.output;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.xml.serializer.XMLSerializerFactory;
import org.xwiki.wikistream.internal.utils.AllFilter;
import org.xwiki.wikistream.type.WikiStreamType;
import org.xwiki.wikistream.xml.internal.output.AbstractXMLBeanOutputWikiStreamFactory;

/**
 * A generic xml output wikistream implementation. This class can be used as a test bench to validate various
 * XMLInputStream wiki parsers.
 * 
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Named("wiki+xml")
@Singleton
public class WikiXMLOutputWikiStreamFactory extends AbstractXMLBeanOutputWikiStreamFactory<WikiXMLOuputProperties>
{
    @Inject
    private XMLSerializerFactory serializerFactory;

    public WikiXMLOutputWikiStreamFactory()
    {
        super(WikiStreamType.WIKI_XML);

        setName("Generic XML output stream");
        setDescription("Write generic XML from wiki events.");
    }

    @Override
    protected Object createListener(Result result, WikiXMLOuputProperties properties) throws XMLStreamException,
        FactoryConfigurationError
    {
        return this.serializerFactory.createSerializer(AllFilter.class, result, null);
    }
}
