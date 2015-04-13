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
package com.xpn.xwiki.internal.render;

import java.lang.reflect.ParameterizedType;
import java.util.Set;

import javax.inject.Provider;

import org.xwiki.component.annotation.Role;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;

/**
 * Hook to let xwiki/1.0 module override a few internal methods to inject some specific behavior that only make sense
 * with the old rendering engine.
 * 
 * @version $Id$
 * @since 7.1M1
 * @deprecated only here for retro-compatibility purposes and should never be used for any new feature
 */
@Role
@Deprecated
public interface OldRendering
{
    /**
     * Type instance for Provider<OldRendering>.
     */
    ParameterizedType TYPE_PROVIDER = new DefaultParameterizedType(null, Provider.class, OldRendering.class);

    void flushCache();

    void renameLinks(XWikiDocument backlinkDocument, DocumentReference oldReference, DocumentReference newReference,
        XWikiContext context) throws XWikiException;

    void resetRenderingEngine(XWikiContext context) throws XWikiException;

    String renderText(String text, XWikiDocument doc, XWikiContext xcontext);

    String renderTemplate(String template, String skin, XWikiContext xcontext);

    String renderTemplate(String template, XWikiContext xcontext);

    String parseContent(String content, XWikiContext xcontext);

    Set<XWikiLink> extractLinks(XWikiDocument doc, XWikiContext context) throws XWikiException;
}
