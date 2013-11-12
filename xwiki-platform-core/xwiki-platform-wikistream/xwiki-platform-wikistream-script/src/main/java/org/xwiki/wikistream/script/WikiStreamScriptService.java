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
package org.xwiki.wikistream.script;

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
import org.xwiki.job.Job;
import org.xwiki.job.JobManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.WikiStreamFactory;
import org.xwiki.wikistream.descriptor.WikiStreamDescriptor;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.internal.job.WikiStreamConverterJob;
import org.xwiki.wikistream.job.WikiStreamConverterJobRequest;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;

import com.xpn.xwiki.XWikiContext;

/**
 * Expose various WikiStream related APIs to scripts.
 * 
 * @version $Id$
 * @since 5.2RC1
 */
@Component
@Named(WikiStreamScriptService.ROLEHINT)
@Singleton
@Unstable
public class WikiStreamScriptService extends AbstractWikiStreamScriptService
{
    public static final String ROLEHINT = "wikistream";

    @Inject
    private ScriptServiceManager scriptServiceManager;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("wikistream")
    private JobManager jobManager;

    public ScriptService get(String id)
    {
        return this.scriptServiceManager.get(ROLEHINT + '.' + id);
    }

    // TODO: introduce advanced right checking system instead
    private void checkProgrammingRights() throws AuthorizationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        if (!xcontext.getWiki().getRightService().hasProgrammingRights(xcontext)) {
            throw new AuthorizationException("WikiStream conversion require programming right");
        }
    }

    /**
     * @since 5.3M2
     */
    public Job startConvert(WikiStreamType inputType, Map<String, Object> inputProperties, WikiStreamType outputType,
        Map<String, Object> outputProperties)
    {
        resetError();

        Job job = null;

        try {
            checkProgrammingRights();

            WikiStreamConverterJobRequest request =
                new WikiStreamConverterJobRequest(inputType, inputProperties, outputType, outputProperties);

            job = this.jobManager.addJob(WikiStreamConverterJob.JOBTYPE, request);
        } catch (Exception e) {
            setError(e);
        }

        return job;
    }

    /**
     * @since 5.3M2
     */
    public Job getCurrentJob()
    {
        resetError();

        Job job = null;

        try {
            checkProgrammingRights();

            return this.jobManager.getCurrentJob();
        } catch (Exception e) {
            setError(e);
        }

        return job;
    }

    /**
     * @since 5.3M2
     */
    private Collection<WikiStreamType> getAvailableModules(Type factoryType)
    {
        resetError();

        try {
            List<ComponentDescriptor<WikiStreamFactory>> descriptors =
                this.componentManagerProvider.get().<WikiStreamFactory> getComponentDescriptorList(factoryType);

            List<WikiStreamType> types = new ArrayList<WikiStreamType>(descriptors.size());
            for (ComponentDescriptor<WikiStreamFactory> descriptor : descriptors) {
                types.add(WikiStreamType.unserialize(descriptor.getRoleHint()));
            }

            Collections.sort(types);

            return types;
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @since 5.3M2
     */
    public Collection<WikiStreamType> getAvailableInputModules()
    {
        return getAvailableModules(InputWikiStreamFactory.class);
    }

    /**
     * @since 5.3M2
     */
    public Collection<WikiStreamType> getAvailableOutputModules()
    {
        return getAvailableModules(OutputWikiStreamFactory.class);
    }

    /**
     * @since 5.3M2
     */
    private WikiStreamDescriptor getWikiStreamDescriptor(Type factoryType, WikiStreamType inputType)
    {
        resetError();

        try {
            return this.componentManagerProvider.get()
                .<WikiStreamFactory> getInstance(factoryType, inputType.serialize()).getDescriptor();
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @since 5.3M2
     */
    public WikiStreamDescriptor getInputWikiStreamDescriptor(WikiStreamType inputType)
    {
        return getWikiStreamDescriptor(InputWikiStreamFactory.class, inputType);
    }

    /**
     * @since 5.3M2
     */
    public WikiStreamDescriptor getOutputWikiStreamDescriptor(WikiStreamType inputType)
    {
        return getWikiStreamDescriptor(OutputWikiStreamFactory.class, inputType);
    }

    /**
     * @since 5.3M2
     */
    private <F extends WikiStreamFactory> F getInputWikiStreamFactory(Type factoryType, WikiStreamType inputType)
    {
        resetError();

        try {
            checkProgrammingRights();

            return this.componentManagerProvider.get().getInstance(factoryType, inputType.serialize());
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @since 5.3M2
     */
    public InputWikiStreamFactory getInputWikiStreamFactory(WikiStreamType inputType)
    {
        return getInputWikiStreamFactory(InputWikiStreamFactory.class, inputType);
    }

    /**
     * @since 5.3M2
     */
    public OutputWikiStreamFactory getOutputWikiStreamFactory(WikiStreamType outputType)
    {
        return getInputWikiStreamFactory(OutputWikiStreamFactory.class, outputType);
    }
}
