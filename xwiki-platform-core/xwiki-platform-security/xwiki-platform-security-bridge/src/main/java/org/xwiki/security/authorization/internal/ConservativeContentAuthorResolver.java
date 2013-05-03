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
package org.xwiki.security.authorization.internal;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.component.annotation.Component;
import org.xwiki.security.internal.XWikiConstants;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Bridge component for resolving content author on behalf of the AuthorizationContextFactory.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("conservative")
@Singleton
public class ConservativeContentAuthorResolver extends DefaultContentAuthorResolver
{

    @Override
    public DocumentReference resolveContentAuthor(final DocumentModelBridge document)
    {
        final DocumentReference contentAuthor = super.resolveContentAuthor(document);

        if (contentAuthor == null) {
            return null;
        }

        final XWikiDocument xwikiDocument = (XWikiDocument) document;

        if (!isMarkedForProgrammingRights(xwikiDocument)) {
            return null;
        }

        return xwikiDocument.getContentAuthorReference();
    }

    /**
     * @param xwikiDocument The document.
     * @return {@literal true} only if the document is marked for programming rights.
     */
    private static boolean isMarkedForProgrammingRights(XWikiDocument xwikiDocument)
    {
        SpaceReference wikiSpace = new SpaceReference(XWikiConstants.XWIKI_SPACE,
            xwikiDocument.getDocumentReference().getWikiReference());
        DocumentReference requiredRightClass = new DocumentReference(XWikiConstants.REQUIRED_RIGHT_CLASSNAME,
            wikiSpace);

        Iterable<BaseObject> objs = xwikiDocument.getXObjects(requiredRightClass);

        if (objs != null) {
            for (BaseObject obj : objs) {
                String level = obj.getStringValue(XWikiConstants.REQUIRED_RIGHT_LEVEL_FIELD_NAME);
                if (XWikiConstants.REQUIRED_RIGHT_PROGRAMMING.equals(level)) {
                    return true;
                }
            }
        }

        return false;
    }
}
