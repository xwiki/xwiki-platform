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
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.descriptor.WikiStreamDescriptor;
import org.xwiki.wikistream.instance.input.InstanceInputProperties;
import org.xwiki.wikistream.script.AbstractWikiStreamScriptService;
import org.xwiki.wikistream.script.WikiStreamScriptService;
import org.xwiki.wikistream.type.WikiStreamType;

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
    @Named("wikistream")
    private ScriptService wikistreamScriptService;

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
        return ((WikiStreamScriptService) this.wikistreamScriptService)
            .getInputWikiStreamDescriptor(WikiStreamType.XWIKI_INSTANCE);
    }

    /**
     * @since 5.3M2
     */
    public WikiStreamDescriptor getOutputWikiStreamDescriptor()
    {
        return ((WikiStreamScriptService) this.wikistreamScriptService)
            .getOutputWikiStreamDescriptor(WikiStreamType.XWIKI_INSTANCE);
    }

    /**
     * @since 5.3M2
     */
    public Job startImport(WikiStreamType inputType, Map<String, Object> inputProperties,
        InstanceInputProperties instanceProperties)
    {
        return ((WikiStreamScriptService) this.wikistreamScriptService).startConvert(inputType, inputProperties,
            WikiStreamType.XWIKI_INSTANCE, instanceProperties);
    }

    /**
     * @since 5.3M2
     */
    public Job startExport(WikiStreamType outputType, Map<String, Object> outputProperties,
        InstanceInputProperties instanceProperties)
    {
        return ((WikiStreamScriptService) this.wikistreamScriptService).startConvert(WikiStreamType.XWIKI_INSTANCE,
            instanceProperties, outputType, outputProperties);
    }
}
