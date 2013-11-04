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
package org.xwiki.wikistream.instance.script;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.Job;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.descriptor.WikiStreamDescriptor;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.instance.internal.InstanceUtils;
import org.xwiki.wikistream.instance.internal.input.InstanceInputProperties;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;
import org.xwiki.wikistream.script.AbstractWikiStreamScriptService;
import org.xwiki.wikistream.script.WikiStreamScriptService;
import org.xwiki.wikistream.type.WikiStreamType;

import com.xpn.xwiki.XWikiContext;

/**
 * Expose various WikiStream <tt>instance</tt> input/output streams related APIs to scripts.
 * 
 * @version $Id$
 * @since 5.2RC1
 */
@Component
@Named(InstanceWikiStreamScriptService.ROLEHINT)
@Singleton
@Unstable
public class InstanceWikiStreamScriptService extends AbstractWikiStreamScriptService
{
    public static final String ROLEHINT = WikiStreamScriptService.ROLEHINT + ".instance";

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named(InstanceUtils.ROLEHINT)
    private InputWikiStreamFactory inputInstanceFactory;

    @Inject
    @Named(InstanceUtils.ROLEHINT)
    private OutputWikiStreamFactory outputInstanceFactory;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    public EntityReferenceSet newEntityReferenceSet()
    {
        return new EntityReferenceSet();
    }

    public InstanceInputProperties newInstanceInputProperties()
    {
        return new InstanceInputProperties();
    }

    /**
     * @since 5.3M2
     */
    public WikiStreamDescriptor getInputWikiStreamDescriptor()
    {
        return this.inputInstanceFactory.getDescriptor();
    }

    /**
     * @since 5.3M2
     */
    public WikiStreamDescriptor getOuputWikiStreamDescriptor()
    {
        return this.outputInstanceFactory.getDescriptor();
    }

    /**
     * @since 5.3M2
     */
    public Job startImport(WikiStreamType inputType, Map<String, Object> inputProperties,
        InstanceInputProperties instanceProperties)
    {
        resetError();

        Job job = null;

        try {
            // TODO: introduce advanced right checking system instead
            XWikiContext xcontext = this.xcontextProvider.get();
            if (xcontext.getWiki().getRightService().hasProgrammingRights(xcontext)) {
                throw new AuthorizationException("WikiStream conversion require programming right");
            }

            // Create instance wiki stream
            OutputWikiStream outputWikiStream = this.outputInstanceFactory.createOutputWikiStream(instanceProperties);

            // Create input wiki stream
            InputWikiStreamFactory inputWikiStreamFactory =
                this.componentManagerProvider.get().getInstance(InputWikiStreamFactory.class, inputType.serialize());

            InputWikiStream inputWikiStream = inputWikiStreamFactory.createInputWikiStream(inputProperties);

            // Start import
            inputWikiStream.read(outputWikiStream.getFilter());
        } catch (Exception e) {
            setError(e);
        }

        return job;
    }

    /**
     * @since 5.3M2
     */
    public Job startExport(WikiStreamType outputType, Map<String, Object> outputProperties,
        InstanceInputProperties instanceProperties)
    {
        resetError();

        Job job = null;

        try {
            // TODO: introduce advanced right checking system instead
            XWikiContext xcontext = this.xcontextProvider.get();
            if (xcontext.getWiki().getRightService().hasProgrammingRights(xcontext)) {
                throw new AuthorizationException("WikiStream conversion require programming right");
            }

            // Create instance wiki stream
            InputWikiStream inputWikiStream = this.inputInstanceFactory.createInputWikiStream(instanceProperties);

            // Create input wiki stream
            OutputWikiStreamFactory outputWikiStreamFactory =
                this.componentManagerProvider.get().getInstance(OutputWikiStreamFactory.class, outputType.serialize());

            OutputWikiStream outputWikiStream = outputWikiStreamFactory.createOutputWikiStream(outputProperties);

            // Start export
            inputWikiStream.read(outputWikiStream.getFilter());
        } catch (Exception e) {
            setError(e);
        }

        return job;
    }
}
