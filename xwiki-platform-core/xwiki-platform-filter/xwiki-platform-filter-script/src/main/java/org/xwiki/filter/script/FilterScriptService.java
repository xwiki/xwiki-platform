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
package org.xwiki.filter.script;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.filter.FilterStreamFactory;
import org.xwiki.filter.descriptor.FilterStreamDescriptor;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.internal.job.FilterStreamConverterJob;
import org.xwiki.filter.job.FilterStreamConverterJobRequest;
import org.xwiki.filter.output.DefaultOutputStreamOutputTarget;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.output.OutputStreamOutputTarget;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

/**
 * Expose various FilterStream related APIs to scripts.
 * 
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Named(FilterScriptService.ROLEHINT)
@Singleton
@Unstable
public class FilterScriptService extends AbstractFilterScriptService
{
    public static final String ROLEHINT = "filter";

    @Inject
    private ScriptServiceManager scriptServiceManager;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    @Named(FilterStreamConverterJob.JOBTYPE)
    private Provider<Job> jobProvider;

    public ScriptService get(String id)
    {
        return this.scriptServiceManager.get(ROLEHINT + '.' + id);
    }

    /**
     * @since 6.2M1
     */
    public Job startConvert(FilterStreamType inputType, Map<String, Object> inputProperties, FilterStreamType outputType,
        Map<String, Object> outputProperties)
    {
        return convert(inputType, inputProperties, outputType, outputProperties, true);
    }

    /**
     * @since 6.2M1
     */
    public Job convert(FilterStreamType inputType, Map<String, Object> inputProperties, FilterStreamType outputType,
        Map<String, Object> outputProperties)
    {
        return convert(inputType, inputProperties, outputType, outputProperties, false);
    }

    private Job convert(FilterStreamType inputType, Map<String, Object> inputProperties, FilterStreamType outputType,
        Map<String, Object> outputProperties, boolean async)
    {
        resetError();

        Job job = null;

        try {
            this.authorization.checkAccess(Right.PROGRAM);

            FilterStreamConverterJobRequest request =
                new FilterStreamConverterJobRequest(inputType, inputProperties, outputType, outputProperties);

            if (async) {
                job = this.jobExecutor.execute(FilterStreamConverterJob.JOBTYPE, request);
            } else {
                // Not using the job executor to make sure to be executed the current thread
                job = this.jobProvider.get();
                job.initialize(request);
                job.run();
            }
        } catch (Exception e) {
            setError(e);
        }

        return job;
    }

    /**
     * @since 6.2M1
     */
    public Job getCurrentJob()
    {
        resetError();

        Job job = null;

        try {
            this.authorization.checkAccess(Right.PROGRAM);

            return this.jobExecutor.getCurrentJob(FilterStreamConverterJob.ROOT_GROUP);
        } catch (Exception e) {
            setError(e);
        }

        return job;
    }

    /**
     * @since 6.2M1
     */
    private Collection<FilterStreamType> getAvailableStreams(Type factoryType)
    {
        resetError();

        try {
            List<ComponentDescriptor<FilterStreamFactory>> descriptors =
                this.componentManagerProvider.get().<FilterStreamFactory> getComponentDescriptorList(factoryType);

            List<FilterStreamType> types = new ArrayList<FilterStreamType>(descriptors.size());
            for (ComponentDescriptor<FilterStreamFactory> descriptor : descriptors) {
                types.add(FilterStreamType.unserialize(descriptor.getRoleHint()));
            }

            Collections.sort(types);

            return types;
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @since 6.2M1
     */
    public Collection<FilterStreamType> getAvailableInputStreams()
    {
        return getAvailableStreams(InputFilterStreamFactory.class);
    }

    /**
     * @since 6.2M1
     */
    public Collection<FilterStreamType> getAvailableOutputStreams()
    {
        return getAvailableStreams(OutputFilterStreamFactory.class);
    }

    /**
     * @since 6.2M1
     */
    private FilterStreamDescriptor getFilterStreamDescriptor(Type factoryType, FilterStreamType inputType)
    {
        resetError();

        try {
            return this.componentManagerProvider.get()
                .<FilterStreamFactory> getInstance(factoryType, inputType.serialize()).getDescriptor();
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @since 6.2M1
     */
    public FilterStreamDescriptor getInputFilterStreamDescriptor(FilterStreamType inputType)
    {
        return getFilterStreamDescriptor(InputFilterStreamFactory.class, inputType);
    }

    /**
     * @since 6.2M1
     */
    public FilterStreamDescriptor getOutputFilterStreamDescriptor(FilterStreamType inputType)
    {
        return getFilterStreamDescriptor(OutputFilterStreamFactory.class, inputType);
    }

    /**
     * @since 6.2M1
     */
    private <F extends FilterStreamFactory> F getInputFilterStreamFactory(Type factoryType, FilterStreamType inputType)
    {
        resetError();

        try {
            this.authorization.checkAccess(Right.PROGRAM);

            return this.componentManagerProvider.get().getInstance(factoryType, inputType.serialize());
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @since 6.2M1
     */
    public InputFilterStreamFactory getInputFilterStreamFactory(FilterStreamType inputType)
    {
        return getInputFilterStreamFactory(InputFilterStreamFactory.class, inputType);
    }

    /**
     * @since 6.2M1
     */
    public OutputFilterStreamFactory getOutputFilterStreamFactory(FilterStreamType outputType)
    {
        return getInputFilterStreamFactory(OutputFilterStreamFactory.class, outputType);
    }

    /**
     * @since 6.2M1
     */
    public OutputStreamOutputTarget createOutputStreamOutputTarget(OutputStream stream, boolean autoclose)
    {
        return new DefaultOutputStreamOutputTarget(stream, autoclose);
    }
}
