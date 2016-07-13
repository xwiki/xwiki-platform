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
package org.xwiki.filter.instance.input;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.filter.FilterDescriptorManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.descriptor.FilterStreamDescriptor;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * @param <F> the type of the filter declaring the events supported by this {@link InstanceInputEventGenerator}
 * @version $Id$
 * @since 6.2M1
 */
public abstract class AbstractInstanceInputEventGenerator<F> implements InstanceInputEventGenerator, Initializable
{
    @Inject
    private FilterDescriptorManager filterDescriptorManager;

    private List<Class<?>> filerInterfaces;

    protected Map<String, Object> properties;

    protected Object filter;

    protected F proxyFilter;

    protected String currentWiki;

    protected Stack<String> currentSpaces = new Stack<String>();

    protected EntityReference currentReference;

    protected FilterStreamDescriptor descriptor;

    @Override
    public void initialize() throws InitializationException
    {
        // Get the type of the internal filter
        ParameterizedType genericType =
            (ParameterizedType) ReflectionUtils.resolveType(AbstractInstanceInputEventGenerator.class, getClass());
        this.filerInterfaces =
            Arrays.<Class<?>>asList(ReflectionUtils.getTypeClass(genericType.getActualTypeArguments()[0]));
    }

    @Override
    public FilterStreamDescriptor getDescriptor()
    {
        return this.descriptor;
    }

    protected void setDescriptor(FilterStreamDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    @Override
    public Collection<Class<?>> getFilterInterfaces() throws FilterException
    {
        return this.filerInterfaces;
    }

    @Override
    public void setWikiFarmParameters(FilterEventParameters parameters) throws FilterException
    {
        // To ovewrite
    }

    @Override
    public void setWikiParameters(String name, FilterEventParameters parameters) throws FilterException
    {
        // To ovewrite
    }

    @Override
    public void setWikiSpaceParameters(String name, FilterEventParameters parameters) throws FilterException
    {
        // To ovewrite
    }

    @Override
    public void setWikiDocumentParameters(String name, FilterEventParameters parameters) throws FilterException
    {
        // To ovewrite
    }

    @Override
    public void setProperties(Map<String, Object> properties)
    {
        this.properties = properties;
    }

    @Override
    public void setFilter(Object filter)
    {
        this.filter = filter;
        this.proxyFilter = this.filterDescriptorManager.createFilterProxy(filter,
            this.filerInterfaces.toArray(ArrayUtils.EMPTY_CLASS_ARRAY));
    }

    @Override
    public void beginWikiFarm(FilterEventParameters parameters) throws FilterException
    {
    }

    @Override
    public void endWikiFarm(FilterEventParameters parameters) throws FilterException
    {
    }

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentWiki = name;
        this.currentReference = new EntityReference(this.currentWiki, EntityType.WIKI);
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentWiki = null;
        this.currentReference = this.currentReference.getParent();
    }

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentSpaces.push(name);
        this.currentReference = new EntityReference(name, EntityType.SPACE, this.currentReference);
    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentSpaces.pop();
        this.currentReference = this.currentReference.getParent();
    }

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentReference = new EntityReference(name, EntityType.DOCUMENT, this.currentReference);
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentReference = this.currentReference.getParent();
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
    }

    @Override
    public void beginWikiDocumentRevision(String version, FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void endWikiDocumentRevision(String version, FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void beginUnknwon(String id, FilterEventParameters parameters) throws FilterException
    {
    }

    @Override
    public void endUnknwon(String id, FilterEventParameters parameters) throws FilterException
    {
    }

    @Override
    public void onUnknwon(String id, FilterEventParameters parameters) throws FilterException
    {
    }
}
