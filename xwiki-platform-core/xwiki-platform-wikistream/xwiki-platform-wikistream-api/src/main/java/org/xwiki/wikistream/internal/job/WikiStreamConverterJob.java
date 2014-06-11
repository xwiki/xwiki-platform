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
package org.xwiki.wikistream.internal.job;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.job.internal.AbstractJob;
import org.xwiki.job.internal.DefaultJobStatus;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.job.WikiStreamConverterJobRequest;
import org.xwiki.wikistream.job.WikiStreamJobRequest;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;

/**
 * Perform a WikiStream conversion.
 * 
 * @version $Id$
 * @since 5.3M2
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(WikiStreamConverterJob.JOBTYPE)
public class WikiStreamConverterJob extends
    AbstractJob<WikiStreamConverterJobRequest, DefaultJobStatus<WikiStreamConverterJobRequest>> implements GroupedJob
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "wikistream.converter";

    /**
     * The root group of all wiki stream conversion jobs.
     */
    public static final JobGroupPath ROOT_GROUP = new JobGroupPath(Arrays.asList(WikiStreamJobRequest.JOBID_PREFIX,
        "converter"));

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    public JobGroupPath getGroupPath()
    {
        return ROOT_GROUP;
    }

    @Override
    protected void runInternal() throws Exception
    {
        InputWikiStreamFactory inputFactory =
            this.componentManagerProvider.get().getInstance(InputWikiStreamFactory.class,
                getRequest().getInputType().serialize());

        InputWikiStream inputWikiStream = inputFactory.createInputWikiStream(getRequest().getInputProperties());

        OutputWikiStreamFactory outputFactory =
            this.componentManagerProvider.get().getInstance(OutputWikiStreamFactory.class,
                getRequest().getOutputType().serialize());

        OutputWikiStream outputWikiStream = outputFactory.createOutputWikiStream(getRequest().getOutputProperties());

        // Convert

        inputWikiStream.read(outputWikiStream.getFilter());

        inputWikiStream.close();
        outputWikiStream.close();
    }
}
