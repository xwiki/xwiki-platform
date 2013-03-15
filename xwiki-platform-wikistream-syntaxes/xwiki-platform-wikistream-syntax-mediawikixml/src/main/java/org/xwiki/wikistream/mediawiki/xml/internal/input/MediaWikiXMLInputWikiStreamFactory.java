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
package org.xwiki.wikistream.mediawiki.xml.internal.input;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xml.sax.ContentHandler;
import org.xwiki.component.annotation.Component;
import org.xwiki.wikistream.mediawiki.xml.input.MediaWikiXMLInputParameters;
import org.xwiki.wikistream.type.WikiStreamType;
import org.xwiki.wikistream.xml.internal.input.AbstractXMLBeanInputWikiStreamFactory;

@Component
@Singleton
@Named("mediawiki+xml")
public class MediaWikiXMLInputWikiStreamFactory extends
    AbstractXMLBeanInputWikiStreamFactory<MediaWikiXMLInputParameters>
{
    public MediaWikiXMLInputWikiStreamFactory()
    {
        super(WikiStreamType.MEDIAWIKI_XML);

        setName("MediaWiki XML input");
        setDescription("Generates wiki events from MediaWiki XML inputstream.");
    }

    @Override
    protected ContentHandler createContentHandler(Object listener, MediaWikiXMLInputParameters parameters)
    {
        return new MediaWikiXMLContentHandlerParser(listener, parameters);
    }
}
