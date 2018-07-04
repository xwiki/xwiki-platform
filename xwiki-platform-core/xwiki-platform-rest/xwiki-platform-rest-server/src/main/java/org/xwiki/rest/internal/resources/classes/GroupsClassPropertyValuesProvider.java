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
package org.xwiki.rest.internal.resources.classes;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryBuilder;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserManager;

import com.google.common.base.Objects;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.GroupsClass;

/**
 * Provides values for the "List of Groups" type of properties.
 *
 * @version $Id$
 * @since 9.8
 */
@Component
@Named("Groups")
@Singleton
public class GroupsClassPropertyValuesProvider extends AbstractUsersAndGroupsClassPropertyValuesProvider<GroupsClass>
{
    @Inject
    private WikiUserManager wikiUserManager;

    @Inject
    private QueryBuilder<GroupsClass> allowedValuesQueryBuilder;

    @Override
    protected Class<GroupsClass> getPropertyType()
    {
        return GroupsClass.class;
    }

    @Override
    protected QueryBuilder<GroupsClass> getAllowedValuesQueryBuilder()
    {
        return this.allowedValuesQueryBuilder;
    }

    @Override
    protected PropertyValues getAllowedValues(GroupsClass propertyDefinition, int limit, String filter) throws Exception
    {
        String wikiId = propertyDefinition.getOwnerDocument().getDocumentReference().getWikiReference().getName();
        if (!Objects.equal(wikiId, this.wikiDescriptorManager.getMainWikiId())
            && this.wikiUserManager.getUserScope(wikiId) != UserScope.LOCAL_ONLY) {
            return getLocalAndGlobalAllowedValues(propertyDefinition, limit, filter);
        } else {
            return getLocalAllowedValues(propertyDefinition, limit, filter);
        }
    }

    @Override
    protected Map<String, Object> getIcon(DocumentReference groupReference)
    {
        Map<String, Object> icon = new HashMap<>();
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            XWikiDocument groupProfileDocument = xcontext.getWiki().getDocument(groupReference, xcontext);
            XWikiAttachment avatarAttachment = getFirstImageAttachment(groupProfileDocument, xcontext);
            if (avatarAttachment != null) {
                icon.put(META_DATA_ICON, xcontext.getWiki().getURL(avatarAttachment.getReference(), "download",
                    "width=30&height=30&keepAspectRatio=true", null, xcontext));
            }
        } catch (XWikiException e) {
            this.logger.warn(
                "Failed to read the avatar of group [{}]. Root cause is [{}]. Using the default avatar instead.",
                groupReference.getName(), ExceptionUtils.getRootCauseMessage(e));
        }
        if (!icon.containsKey(META_DATA_ICON)) {
            icon.put(META_DATA_ICON, xcontext.getWiki().getSkinFile("icons/xwiki/noavatargroup.png", true, xcontext));
        }

        return icon;
    }

    private XWikiAttachment getFirstImageAttachment(XWikiDocument document, XWikiContext xcontext)
    {
        for (XWikiAttachment attachment : document.getAttachmentList()) {
            if (attachment.isImage(xcontext)) {
                return attachment;
            }
        }
        return null;
    }

    @Override
    protected String getLabel(DocumentReference groupReference, Object currentLabel)
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            return xcontext.getWiki().getDocument(groupReference, xcontext).getRenderedTitle(Syntax.PLAIN_1_0,
                xcontext);
        } catch (XWikiException e) {
            this.logger.warn("Failed to get the title of group [{}]. Root cause is [{}].",
                this.entityReferenceSerializer.serialize(groupReference), ExceptionUtils.getRootCauseMessage(e));
            return super.getLabel(groupReference, currentLabel);
        }
    }
}
