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
package org.xwiki.wikistream.instance.input;

import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Stack;

import javax.inject.Inject;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.filter.FilterDescriptorManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.internal.ParametersTree;

public abstract class AbstractInstanceInputEventGenerator<F> implements InstanceInputEventGenerator, Initializable
{
    @Inject
    private FilterDescriptorManager filterDescriptorManager;

    private Class<F> filterType;

    protected Map<String, Object> properties;

    protected Object filter;

    protected F proxyFilter;

    protected String currentWiki;

    protected Stack<String> currentSpaces = new Stack<String>();

    protected EntityReference currentReference;

    private ParametersTree currentParameters;

    @Override
    public void initialize() throws InitializationException
    {
        // Get the type of the internal filter
        ParameterizedType genericType =
            (ParameterizedType) ReflectionUtils.resolveType(AbstractEntityEventGenerator.class, getClass());
        this.filterType = ReflectionUtils.getTypeClass(genericType.getActualTypeArguments()[2]);
    }

    public void setProperties(Map<String, Object> properties)
    {
        this.properties = properties;
    }

    @Override
    public void setFilter(Object filter)
    {
        this.filter = filter;
        this.proxyFilter = this.filterDescriptorManager.createFilterProxy(this.filterType, filter);
    }

    @Override
    public void beginFarm(FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);
    }

    @Override
    public void endFarm(FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentWiki = name;
        this.currentReference = new EntityReference(this.currentWiki, EntityType.WIKI);
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentWiki = null;
        this.currentReference = this.currentReference.getParent();
        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentSpaces.push(name);
        this.currentReference = new EntityReference(name, EntityType.SPACE, this.currentReference);
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);
    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentSpaces.pop();
        this.currentReference = this.currentReference.getParent();
        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginUnknwon(String id, FilterEventParameters parameters) throws FilterException
    {
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);
    }

    @Override
    public void endUnknwon(String id, FilterEventParameters parameters) throws FilterException
    {
        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void onUnknwon(String id, FilterEventParameters parameters) throws FilterException
    {
    }
}
