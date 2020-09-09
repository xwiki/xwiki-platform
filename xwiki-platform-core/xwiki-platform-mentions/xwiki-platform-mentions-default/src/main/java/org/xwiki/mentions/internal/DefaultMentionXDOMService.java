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
package org.xwiki.mentions.internal;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Default implementation of {@link MentionXDOMService}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Singleton
public class DefaultMentionXDOMService implements MentionXDOMService
{
    private static final String MENTION_MACRO_NAME = "mention";

    private static final String REFERENCE_PARAM_NAME = "reference";

    private static final String ANCHORID_PARAM_NAME = "anchor";

    @Inject
    private Logger logger;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    private static boolean matchMentionMacro(Block block)
    {
        return block instanceof MacroBlock && Objects.equals(((MacroBlock) block).getId(), MENTION_MACRO_NAME);
    }

    @Override
    public List<MacroBlock> listMentionMacros(XDOM xdom)
    {
        return xdom.getBlocks(DefaultMentionXDOMService::matchMentionMacro, Block.Axes.DESCENDANT);
    }

    @Override
    public Map<DocumentReference, List<String>> countByIdentifier(List<MacroBlock> mentions,
        WikiReference wikiReference)
    {
        Map<DocumentReference, List<String>> ret = new HashMap<>();
        for (MacroBlock block : mentions) {
            String macroReference = block.getParameter(REFERENCE_PARAM_NAME);
            DocumentReference reference = this.documentReferenceResolver.resolve(macroReference, wikiReference);
            String anchor = block.getParameter(ANCHORID_PARAM_NAME);
            ret.merge(reference, new ArrayList<>(Collections.singletonList(anchor)), (l1, l2) -> {
                l1.addAll(l2);
                return l1;
            });
        }
        return ret;
    }

    @Override
    public Optional<XDOM> parse(String payload, Syntax syntax)
    {
        Optional<XDOM> oxdom;
        try {
            Parser instance = this.componentManager.get().getInstance(Parser.class, syntax.toIdString());
            XDOM xdom = instance.parse(new StringReader(payload));
            oxdom = Optional.of(xdom);
        } catch (ParseException e) {
            this.logger
                .warn("Failed to parse the payload [{}]. Cause [{}].", payload, ExceptionUtils.getRootCauseMessage(e));
            oxdom = Optional.empty();
        } catch (ComponentLookupException e) {
            this.logger
                .warn("Failed to get the parser instance [{}]. Cause [{}].", syntax,
                    ExceptionUtils.getRootCauseMessage(e));
            oxdom = Optional.empty();
        }
        return oxdom;
    }
}
