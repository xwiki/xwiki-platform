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
package org.xwiki.eventstream.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.WikiBaseObjectComponentBuilder;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This implementation of {@link WikiBaseObjectComponentBuilder} allows to dynamically register an
 * {@link DefaultUntypedRecordableEventDescriptor} against the Component Manager.
 *
 * @version $Id$
 * @since 9.6RC1
 */
@Component
@Named(UntypedRecordableEventDescriptorComponentBuilder.BOUNDED_XOBJECT_CLASS)
@Singleton
public class UntypedRecordableEventDescriptorComponentBuilder implements WikiBaseObjectComponentBuilder
{
    /**
     * The class name of the XObject that should be bounded to this builder.
     */
    public static final String BOUNDED_XOBJECT_CLASS = "XWiki.EventStream.Code.EventClass";

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Override
    public EntityReference getClassReference()
    {
        return new LocalDocumentReference(Arrays.asList("XWiki", "EventStream", "Code"), "EventClass");
    }

    @Override
    public List<WikiComponent> buildComponents(BaseObject baseObject) throws WikiComponentException
    {
        try {
            XWikiDocument parentDocument = baseObject.getOwnerDocument();
            this.checkRights(parentDocument.getDocumentReference(), parentDocument.getAuthorReference());

            return Arrays.asList(
                    new DefaultUntypedRecordableEventDescriptor(
                            baseObject.getReference(), parentDocument.getAuthorReference(), baseObject,
                            contextualLocalizationManager));
        } catch (Exception e) {
            throw new WikiComponentException(String.format(
                    "Unable to build the UntypedRecordableEvent wiki component "
                            + "for [%s].", baseObject), e);
        }
    }

    /**
     * Ensure that the given author has the administrative rights in the current context.
     *
     * @param documentReference the working entity
     * @param authorReference the author that should have its rights checked
     * @throws EventStreamException if the author rights are not sufficient
     */
    private void checkRights(DocumentReference documentReference, DocumentReference authorReference)
            throws EventStreamException
    {
        if (!this.authorizationManager.hasAccess(Right.ADMIN, authorReference, documentReference.getWikiReference()))
        {
            throw new EventStreamException("Registering Untyped Events requires wiki administration rights.");
        }
    }
}
