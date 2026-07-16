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
    /**
     * The role hint of this script service.
     */
    public static final String ROLEHINT = FilterScriptService.ROLEHINT + ".instance";

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named("filter")
    private ScriptService filterScriptService;

    /**
     * @return a new empty set of entity references, to be used to filter the entities to import or export
     */
    public EntityReferenceSet newEntityReferenceSet()
    {
        return new EntityReferenceSet();
    }

    /**
     * @return a new empty set of properties controlling how the instance input stream behaves
     */
    public InstanceInputProperties newInstanceInputProperties()
    {
        return new InstanceInputProperties();
    }

    /**
     * @return the descriptor of the instance input stream
     * @since 6.2M1
     */
    public FilterStreamDescriptor getInputFilterStreamDescriptor()
    {
        return ((FilterScriptService) this.filterScriptService)
            .getInputFilterStreamDescriptor(FilterStreamType.XWIKI_INSTANCE);
    }

    /**
     * @return the descriptor of the instance output stream
     * @since 6.2M1
     */
    public FilterStreamDescriptor getOutputFilterStreamDescriptor()
    {
        return ((FilterScriptService) this.filterScriptService)
            .getOutputFilterStreamDescriptor(FilterStreamType.XWIKI_INSTANCE);
    }

    /**
     * Start importing into the current instance.
     *
     * @param inputType the type of the input stream to read the data from
     * @param inputProperties the properties controlling the input stream
     * @param instanceProperties the properties controlling how the data is written into the instance
     * @return the job importing the data
     * @since 6.2M1
     */
    public Job startImport(FilterStreamType inputType, Map<String, Object> inputProperties,
        InstanceInputProperties instanceProperties)
    {
        return ((FilterScriptService) this.filterScriptService).startConvert(inputType, inputProperties,
            FilterStreamType.XWIKI_INSTANCE, instanceProperties);
    }

    /**
     * Start exporting the current instance.
     *
     * @param outputType the type of the output stream to write the data to
     * @param outputProperties the properties controlling the output stream
     * @param instanceProperties the properties controlling how the data is read from the instance
     * @return the job exporting the data
     * @since 6.2M1
     */
    public Job startExport(FilterStreamType outputType, Map<String, Object> outputProperties,
        InstanceInputProperties instanceProperties)
    {
        return ((FilterScriptService) this.filterScriptService).startConvert(FilterStreamType.XWIKI_INSTANCE,
            instanceProperties, outputType, outputProperties);
    }
}
