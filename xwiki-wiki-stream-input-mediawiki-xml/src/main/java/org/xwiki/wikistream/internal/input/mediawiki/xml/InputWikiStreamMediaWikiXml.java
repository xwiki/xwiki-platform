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
package org.xwiki.wikistream.internal.input.mediawiki.xml;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.wikistream.AbstractInputWikiStream;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.descriptor.WikiStreamDescriptor;
import org.xwiki.wikistream.input.mediawiki.xml.MediaWikiXmlParameters;
import org.xwiki.wikistream.type.WikiStreamType;

/**
 * @version $Id: 5c213c4c836ba7a506c7fae073a3c2eee28e20be $
 */
@Component("mediawiki-xml")
@Singleton
public class InputWikiStreamMediaWikiXml extends AbstractInputWikiStream<MediaWikiXmlParameters>
{

    public InputWikiStreamMediaWikiXml(String name, String description, WikiStreamDescriptor descriptor)
    {
        super(name, description, descriptor);
        // TODO Auto-generated constructor stub
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.WikiStream#getType()
     */
    @Override
    public WikiStreamType getType()
    {
        return WikiStreamType.MEDIAWIKI_XML;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.InputWikiStream#parse(java.lang.Object, org.xwiki.rendering.listener.Listener)
     */
    @Override
    public void parse(MediaWikiXmlParameters parametersBean, Listener wikiEventListener) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

}
