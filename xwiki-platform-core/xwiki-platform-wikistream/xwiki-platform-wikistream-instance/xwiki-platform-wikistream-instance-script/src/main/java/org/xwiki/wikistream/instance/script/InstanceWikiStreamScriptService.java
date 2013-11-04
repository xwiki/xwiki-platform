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
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.instance.internal.input.InstanceInputProperties;
import org.xwiki.wikistream.instance.internal.input.InstanceInputWikiStreamFactory;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;
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
public class InstanceWikiStreamScriptService implements ScriptService
{
    public static final String ROLEHINT = WikiStreamScriptService.ROLEHINT + ".instance";

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named(InstanceInputWikiStreamFactory.ROLEHINT)
    private InputWikiStreamFactory inputInstanceFactory;

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

    public void export(WikiStreamType outputStream, InstanceInputProperties inputProperties,
        Map<String, Object> outputProperties) throws WikiStreamException, ComponentLookupException
    {
        // TODO: introduce advanced right checking system
        XWikiContext xcontext = this.xcontextProvider.get();
        if (xcontext.getWiki().getRightService().hasProgrammingRights(xcontext)) {
            // Create input wiki stream
            InputWikiStream inputWikiStream = this.inputInstanceFactory.createInputWikiStream(inputProperties);

            // Create output wiki stream
            OutputWikiStreamFactory outputWikiStreamFactory =
                this.componentManagerProvider.get()
                    .getInstance(OutputWikiStreamFactory.class, outputStream.serialize());

            OutputWikiStream outputWikiStream = outputWikiStreamFactory.creaOutputWikiStream(outputProperties);

            // Export
            inputWikiStream.read(outputWikiStream.getFilter());
        }
    }
}
