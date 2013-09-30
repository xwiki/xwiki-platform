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
package org.xwiki.wikistream.internal.output;

import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.internal.AbstractBeanWikiStreamFactory;
import org.xwiki.wikistream.internal.input.BeanInputWikiStream;
import org.xwiki.wikistream.output.BeanOutputWikiStreamFactory;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.type.WikiStreamType;

/**
 * @param <P>
 * @version $Id$
 * @since 5.2M2
 */
public abstract class AbstractBeanOutputWikiStreamFactory<P> extends AbstractBeanWikiStreamFactory<P> implements
    BeanOutputWikiStreamFactory<P>
{
    @Inject
    private ComponentManager componentManager;

    public AbstractBeanOutputWikiStreamFactory(WikiStreamType type)
    {
        super(type);
    }

    @Override
    public OutputWikiStream creaOutputWikiStream(Map<String, Object> properties) throws WikiStreamException
    {
        return creaOutputWikiStream(createPropertiesBean(properties));
    }

    @Override
    public OutputWikiStream creaOutputWikiStream(P properties) throws WikiStreamException
    {
        BeanOutputWikiStream<P> inputWikiStream;
        try {
            inputWikiStream =
                this.componentManager.getInstance(new DefaultParameterizedType(null, BeanOutputWikiStream.class,
                    properties.getClass()), getType().serialize());
        } catch (ComponentLookupException e) {
            throw new WikiStreamException(String.format("Failed to get instance of [%s] for type [%s]",
                BeanInputWikiStream.class, getType()), e);
        }

        inputWikiStream.setProperties(properties);

        return inputWikiStream;
    }
}
