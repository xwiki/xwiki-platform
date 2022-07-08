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
package org.xwiki.rendering.internal.rights;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.RawContentEvent;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Check permissions for RAW HTML content.
 *
 * @version $Id$
 * @since 14.6RC1
 */
@Component
@Named(HTMLRawContentPermissionCheckerListener.NAME)
public class HTMLRawContentPermissionCheckerListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "htmlRawContentPermissionChecker";

    /**
     * Used to verify if the current doc has programming rights.
     */
    @Inject
    private ContextualAuthorizationManager authorizationManager;

    /**
     * Default constructor, initializes the event listener.
     */
    public HTMLRawContentPermissionCheckerListener()
    {
        super(NAME, new RawContentEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (source instanceof MacroTransformationContext && data instanceof Syntax) {
            check((CancelableEvent) event, (MacroTransformationContext) source, (Syntax) data);
        }
    }

    private void check(CancelableEvent event, MacroTransformationContext context, Syntax syntax)
    {
        if (SyntaxType.HTML_FAMILY_TYPES.contains(syntax.getType())
            && (context.getTransformationContext().isRestricted()
            // TODO: change to CLIENT_SCRIPT right once introduced.
            || !this.authorizationManager.hasAccess(Right.PROGRAM)))
        {
            event.cancel();
        }
    }
}
