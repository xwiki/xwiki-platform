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
package org.xwiki.security.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;

/**
 * Temporary implementation of the (@link XWikiBridge} interface to access xwiki information.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultXWikiBridge implements XWikiBridge
{
    /** Document reference resolver for user and group. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /** Cached main wiki reference. */
    private WikiReference mainWikiReference;

    @Override
    public WikiReference getMainWikiReference()
    {
        if (mainWikiReference == null) {
            mainWikiReference = new WikiReference(xcontextProvider.get().getMainXWiki());
        }
        return mainWikiReference;
    }

    @Override
    public boolean isWikiReadOnly()
    {
        return xcontextProvider.get().getWiki().isReadOnly();
    }

    @Override
    public boolean needsAuthentication(Right right)
    {
        XWikiContext context = xcontextProvider.get();
        String prefName = "authenticate_" + right.getName();

        String value = context.getWiki().getXWikiPreference(prefName, "", context);
        Boolean result = checkNeedsAuthValue(value);
        if (result != null) {
            return result;
        }

        value = context.getWiki().getSpacePreference(prefName, "", context).toLowerCase();
        result = checkNeedsAuthValue(value);
        if (result != null) {
            return result;
        }

        return false;
    }

    private Boolean checkNeedsAuthValue(String value)
    {
        if (value != null && !value.equals("")) {
            if (value.toLowerCase().equals("yes")) {
                return true;
            }
            try {
                if (Integer.parseInt(value) > 0) {
                    return true;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public EntityReference toCompatibleEntityReference(EntityReference reference)
    {
        if (reference == null) {
            return reference;
        }

        // Make sure the reference is complete
        EntityReference compatibleReference = this.currentResolver.resolve(reference, reference.getType());

        // Convert to PAGE reference to DOCUMENT reference since the security system design does not work well with PAGE
        // one (which have different kinds of right at the same level)
        if (compatibleReference.getType() == EntityType.PAGE
            || compatibleReference.getType().isAllowedAncestor(EntityType.PAGE)) {
            XWikiContext xcontext = this.xcontextProvider.get();
            compatibleReference = xcontext.getWiki().getDocumentReference(reference, xcontext);
        }

        return compatibleReference;
    }
}
