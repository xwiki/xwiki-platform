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
package org.xwiki.filter.instance.script;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.filter.descriptor.FilterStreamDescriptor;
import org.xwiki.filter.instance.input.InstanceInputProperties;
import org.xwiki.filter.script.AbstractFilterScriptService;
import org.xwiki.filter.script.FilterScriptService;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.job.Job;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.script.service.ScriptService;

/**
 * Expose various FilterStream {@code instance} input/output streams related APIs to scripts.
 * 
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Named(InstanceFilterScriptService.ROLEHINT)
@Singleton
public class InstanceFilterScriptService extends AbstractFilterScriptService
{
    public static final String ROLEHINT = FilterScriptService.ROLEHINT + ".instance";

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named("filter")
    private ScriptService filterScriptService;

    public EntityReferenceSet newEntityReferenceSet()
    {
        return new EntityReferenceSet();
    }

    public InstanceInputProperties newInstanceInputProperties()
    {
        return new InstanceInputProperties();
    }

    /**
     * @since 6.2M1
     */
    public FilterStreamDescriptor getInputFilterStreamDescriptor()
    {
        return ((FilterScriptService) this.filterScriptService)
            .getInputFilterStreamDescriptor(FilterStreamType.XWIKI_INSTANCE);
    }

    /**
     * @since 6.2M1
     */
    public FilterStreamDescriptor getOutputFilterStreamDescriptor()
    {
        return ((FilterScriptService) this.filterScriptService)
            .getOutputFilterStreamDescriptor(FilterStreamType.XWIKI_INSTANCE);
    }

    /**
     * @since 6.2M1
     */
    public Job startImport(FilterStreamType inputType, Map<String, Object> inputProperties,
        InstanceInputProperties instanceProperties)
    {
        return ((FilterScriptService) this.filterScriptService).startConvert(inputType, inputProperties,
            FilterStreamType.XWIKI_INSTANCE, instanceProperties);
    }

    /**
     * @since 6.2M1
     */
    public Job startExport(FilterStreamType outputType, Map<String, Object> outputProperties,
        InstanceInputProperties instanceProperties)
    {
        return ((FilterScriptService) this.filterScriptService).startConvert(FilterStreamType.XWIKI_INSTANCE,
            instanceProperties, outputType, outputProperties);
    }
}
