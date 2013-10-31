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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;

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
public class WikiStreamScriptService implements ScriptService
{
    public static final String ROLEHINT = "wikistream";

    @Inject
    private ScriptServiceManager scriptServiceManager;

    @Inject
    private ComponentManager componentManager;

    public ScriptService get(String id)
    {
        return this.scriptServiceManager.get(ROLEHINT + '.' + id);
    }

    public void convert(WikiStreamType inputType, Map<String, Object> inputProperties, WikiStreamType outputType,
        Map<String, Object> outputProperties) throws WikiStreamException, ComponentLookupException
    {
        createInputWikiStream(inputType, inputProperties).read(createOutputWikiStream(outputType, outputProperties));
    }

    public InputWikiStream createInputWikiStream(WikiStreamType inputType, Map<String, Object> inputProperties)
        throws ComponentLookupException, WikiStreamException
    {
        InputWikiStreamFactory factory =
            this.componentManager.getInstance(InputWikiStreamFactory.class, inputType.serialize());

        return factory.createInputWikiStream(inputProperties);
    }

    public OutputWikiStream createOutputWikiStream(WikiStreamType outputType, Map<String, Object> outputProperties)
        throws ComponentLookupException, WikiStreamException
    {
        OutputWikiStreamFactory factory =
            this.componentManager.getInstance(OutputWikiStreamFactory.class, outputType.serialize());

        return factory.createOutputWikiStream(outputProperties);
    }
}
