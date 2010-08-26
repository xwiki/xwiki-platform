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
package org.xwiki.rendering.internal.macro.script;

import java.util.Collections;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.ScriptEvaluationStartsEvent;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;


/**
 * Listens to {@link org.xwiki.observation.event.ScriptEvaluationStartsEvent} and cancels the evaluation if the script
 * is nested.
 * 
 * @version $Id$
 * @since 2.5M1
 */
@Component("nestedscriptmacrovalidator")
public class NestedScriptMacroValidatorListener implements EventListener
{
    /** FIXME Nested script macro validator. */
    @Requirement("nested")
    private ScriptMacroValidator validator;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    public String getName()
    {
        return "nestedscriptmacrovalidator";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    public List<Event> getEvents()
    {
        return Collections.singletonList((Event) new ScriptEvaluationStartsEvent());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object, java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ScriptEvaluationStartsEvent) {
            MacroTransformationContext context = (MacroTransformationContext) source;
            String content = context.getCurrentMacroBlock().getContent();
            try {
                validator.validate((ScriptMacroParameters) data, content, context);
            } catch (MacroExecutionException exception) {
                ((CancelableEvent) event).cancel(exception.getMessage());
            }
        }
    }
}

