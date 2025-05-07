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
package com.xpn.xwiki.internal.filter.output;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiClassFilter;

import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * @version $Id$
 * @since 9.0RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class BaseClassOutputFilterStream extends AbstractElementOutputFilterStream<BaseClass> implements Initializable
{
    @Inject
    private EntityOutputFilterStream<PropertyClass> propertyFilter;

    @Override
    public void initialize() throws InitializationException
    {
        initialize(this.propertyFilter);
    }

    private PropertyClassOutputFilterStream getPropertyClassOutputFilterStream()
    {
        return (PropertyClassOutputFilterStream) this.propertyFilter;
    }

    // Events

    @Override
    public void beginWikiClass(FilterEventParameters parameters) throws FilterException
    {
        if (this.enabled) {
            if (this.entity == null) {
                this.entity = new BaseClass();
            }

            this.entity.setDocumentReference(getDocumentReference(WikiClassFilter.PARAMETER_NAME, parameters, null));
            this.entity.setCustomClass(getString(WikiClassFilter.PARAMETER_CUSTOMCLASS, parameters, null));
            this.entity.setCustomMapping(getString(WikiClassFilter.PARAMETER_CUSTOMMAPPING, parameters, null));
            this.entity.setDefaultViewSheet(getString(WikiClassFilter.PARAMETER_SHEET_DEFAULTVIEW, parameters, null));
            this.entity.setDefaultEditSheet(getString(WikiClassFilter.PARAMETER_SHEET_DEFAULTEDIT, parameters, null));
            this.entity.setDefaultWeb(getString(WikiClassFilter.PARAMETER_DEFAULTSPACE, parameters, null));
            this.entity.setNameField(getString(WikiClassFilter.PARAMETER_NAMEFIELD, parameters, null));
            this.entity.setValidationScript(getString(WikiClassFilter.PARAMETER_VALIDATIONSCRIPT, parameters, null));

            getPropertyClassOutputFilterStream().setCurrentXClass(this.entity);
            getPropertyClassOutputFilterStream().enable();
        }
    }

    @Override
    public void endWikiClassProperty(String name, String type, FilterEventParameters parameters) throws FilterException
    {
        if (this.enabled) {
            if (getPropertyClassOutputFilterStream().getEntity() != null) {
                this.entity.safeput(name, getPropertyClassOutputFilterStream().getEntity());
                getPropertyClassOutputFilterStream().setEntity(null);
            }
        }
    }

    @Override
    public void endWikiClass(FilterEventParameters parameters) throws FilterException
    {
        getPropertyClassOutputFilterStream().setCurrentXClass(null);
        getPropertyClassOutputFilterStream().disable();
    }
}
