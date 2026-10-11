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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.job.JobRequestContext;

/**
 * Expose various FilterStream related APIs to scripts.
 * 
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Named(FilterScriptService.ROLEHINT)
@Singleton
public class FilterScriptService extends AbstractFilterScriptService
{
    /**
     * The role hint of this script service.
     */
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
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named(FilterStreamConverterJob.JOBTYPE)
    private Provider<Job> jobProvider;

    private Job lastJob;

    /**
     * @param id the identifier of the sub script service to return
     * @return the sub script service registered under the passed identifier
     */
    public ScriptService get(String id)
    {
        return this.scriptServiceManager.get(ROLEHINT + '.' + id);
    }

    /**
     * Start converting from one stream type to another asynchronously.
     *
     * @param inputType the type of the input stream to read the data from
     * @param inputProperties the properties controlling the input stream
     * @param outputType the type of the output stream to write the data to
     * @param outputProperties the properties controlling the output stream
     * @return the job performing the conversion
     * @since 6.2M1
     */
    public Job startConvert(FilterStreamType inputType, Map<String, Object> inputProperties,
        FilterStreamType outputType, Map<String, Object> outputProperties)
    {
        return startConvert(inputType, inputProperties, outputType, true, outputProperties);
    }

    /**
     * Start converting from one stream type to another asynchronously.
     *
     * @param inputType the type of the input stream to read the data from
     * @param inputProperties the properties controlling the input stream
     * @param outputType the type of the output stream to write the data to
     * @param folded {@code true} if the events should be folded
     * @param outputProperties the properties controlling the output stream
     * @return the job performing the conversion
     * @since 8.2RC1
     */
    public Job startConvert(FilterStreamType inputType, Map<String, Object> inputProperties,
        FilterStreamType outputType, boolean folded, Map<String, Object> outputProperties)
    {
        return convert(inputType, inputProperties, outputType, folded, outputProperties, true);
    }

    /**
     * Convert from one stream type to another synchronously.
     *
     * @param inputType the type of the input stream to read the data from
     * @param inputProperties the properties controlling the input stream
     * @param outputType the type of the output stream to write the data to
     * @param outputProperties the properties controlling the output stream
     * @return the job that performed the conversion
     * @since 6.2M1
     */
    public Job convert(FilterStreamType inputType, Map<String, Object> inputProperties, FilterStreamType outputType,
        Map<String, Object> outputProperties)
    {
        return convert(inputType, inputProperties, outputType, true, outputProperties);
    }

    /**
     * Convert from one stream type to another synchronously.
     *
     * @param inputType the type of the input stream to read the data from
     * @param inputProperties the properties controlling the input stream
     * @param outputType the type of the output stream to write the data to
     * @param folded {@code true} if the events should be folded
     * @param outputProperties the properties controlling the output stream
     * @return the job that performed the conversion
     * @since 8.2RC1
     */
    public Job convert(FilterStreamType inputType, Map<String, Object> inputProperties, FilterStreamType outputType,
        boolean folded, Map<String, Object> outputProperties)
    {
        return convert(inputType, inputProperties, outputType, folded, outputProperties, false);
    }

    private Job convert(FilterStreamType inputType, Map<String, Object> inputProperties, FilterStreamType outputType,
        boolean folded, Map<String, Object> outputProperties, boolean async)
    {
        resetError();

        this.lastJob = null;

        try {
            this.authorization.checkAccess(Right.PROGRAM);

            FilterStreamConverterJobRequest request =
                new FilterStreamConverterJobRequest(inputType, inputProperties, outputType, folded, outputProperties);

            if (async) {
                // Give a few context related values to the job
                JobRequestContext.set(request, this.xcontextProvider.get());

                this.lastJob = this.jobExecutor.execute(FilterStreamConverterJob.JOBTYPE, request);
            } else {
                // Not using the job executor to make sure to be executed in the current thread
                this.lastJob = this.jobProvider.get();
                this.lastJob.initialize(request);
                this.lastJob.run();
            }
        } catch (Exception e) {
            setError(e);
        }

        return this.lastJob;
    }

    /**
     * @return the last job started through this script service
     * @since 6.2M1
     */
    public Job getCurrentJob()
    {
        return this.lastJob;
    }

    /**
     * @since 6.2M1
     */
    private Collection<FilterStreamType> getAvailableStreams(Type factoryType)
    {
        resetError();

        try {
            List<ComponentDescriptor<FilterStreamFactory>> descriptors =
                this.componentManagerProvider.get().<FilterStreamFactory>getComponentDescriptorList(factoryType);

            List<FilterStreamType> types = new ArrayList<>(descriptors.size());
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
     * @return the types of all the available input streams
     * @since 6.2M1
     */
    public Collection<FilterStreamType> getAvailableInputStreams()
    {
        return getAvailableStreams(InputFilterStreamFactory.class);
    }

    /**
     * @return the types of all the available output streams
     * @since 6.2M1
     */
    public Collection<FilterStreamType> getAvailableOutputStreams()
    {
        return getAvailableStreams(OutputFilterStreamFactory.class);
    }

    /**
     * @since 6.2M1
     */
    private FilterStreamDescriptor getFilterStreamDescriptor(Type factoryType, FilterStreamType type)
    {
        resetError();

        try {
            return this.componentManagerProvider.get()
                .<FilterStreamFactory>getInstance(factoryType, type.serialize()).getDescriptor();
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @param inputType the type of the input stream to describe
     * @return the descriptor of the input stream of the passed type
     * @since 6.2M1
     */
    public FilterStreamDescriptor getInputFilterStreamDescriptor(FilterStreamType inputType)
    {
        return getFilterStreamDescriptor(InputFilterStreamFactory.class, inputType);
    }

    /**
     * @param outputType the type of the output stream to describe
     * @return the descriptor of the output stream of the passed type
     * @since 6.2M1
     */
    public FilterStreamDescriptor getOutputFilterStreamDescriptor(FilterStreamType outputType)
    {
        return getFilterStreamDescriptor(OutputFilterStreamFactory.class, outputType);
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
     * @param inputType the type of the input stream factory to return
     * @return the input stream factory of the passed type
     * @since 6.2M1
     */
    public InputFilterStreamFactory getInputFilterStreamFactory(FilterStreamType inputType)
    {
        return getInputFilterStreamFactory(InputFilterStreamFactory.class, inputType);
    }

    /**
     * @param outputType the type of the output stream factory to return
     * @return the output stream factory of the passed type
     * @since 6.2M1
     */
    public OutputFilterStreamFactory getOutputFilterStreamFactory(FilterStreamType outputType)
    {
        return getInputFilterStreamFactory(OutputFilterStreamFactory.class, outputType);
    }

    /**
     * @param stream the stream to wrap into an output target
     * @param autoclose {@code true} if the stream should be closed automatically once the conversion is done
     * @return the output target wrapping the passed stream
     * @since 6.2M1
     */
    public OutputStreamOutputTarget createOutputStreamOutputTarget(OutputStream stream, boolean autoclose)
    {
        return new DefaultOutputStreamOutputTarget(stream, autoclose);
    }
}
