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

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.component.util.ReflectionUtils;
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
public abstract class AbstractBeanOutputWikiStreamFactory<P, F> extends AbstractBeanWikiStreamFactory<P> implements
    BeanOutputWikiStreamFactory<P>
{
    @Inject
    private ComponentManager componentManager;

    private List<Class< ? >> filerInterfaces;

    public AbstractBeanOutputWikiStreamFactory(WikiStreamType type)
    {
        super(type);
    }

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        // Get bean properties type
        ParameterizedType genericType =
            (ParameterizedType) ReflectionUtils.resolveType(AbstractBeanOutputWikiStreamFactory.class, getClass());
        this.filerInterfaces =
            Arrays.<Class< ? >> asList(ReflectionUtils.getTypeClass(genericType.getActualTypeArguments()[1]));
    }

    @Override
    public Collection<Class< ? >> getFilterInterfaces() throws WikiStreamException
    {
        return this.filerInterfaces;
    }

    @Override
    public OutputWikiStream createOutputWikiStream(Map<String, Object> properties) throws WikiStreamException
    {
        return createOutputWikiStream(createPropertiesBean(properties));
    }

    @Override
    public OutputWikiStream createOutputWikiStream(P properties) throws WikiStreamException
    {
        BeanOutputWikiStream<P> inputWikiStream;
        try {
            inputWikiStream =
                this.componentManager.getInstance(new DefaultParameterizedType(null, BeanOutputWikiStream.class,
                    getPropertiesBeanClass()), getType().serialize());
        } catch (ComponentLookupException e) {
            throw new WikiStreamException(String.format("Failed to get instance of [%s] for type [%s]",
                BeanInputWikiStream.class, getType()), e);
        }

        inputWikiStream.setProperties(properties);

        return inputWikiStream;
    }
}
