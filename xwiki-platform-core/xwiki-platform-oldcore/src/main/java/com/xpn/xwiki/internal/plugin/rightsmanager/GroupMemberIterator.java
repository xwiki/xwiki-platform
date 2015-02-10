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
package com.xpn.xwiki.internal.plugin.rightsmanager;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.rightsmanager.RightsManager;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Iterates over all the users and groups found in the passed reference (which can be a reference to a group or to
 * a user). Handles nested groups.
 *
 * @version $Id$
 * @since 6.4.2, 7.0M2
 */
public class GroupMemberIterator implements Iterator<DocumentReference>
{
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    private XWikiContext xwikicontext;

    private DocumentReference userOrGroupReference;

    private Stack<Iterator<DocumentReference>> membersIteratorStack = new Stack<>();

    private DocumentReference lookaheadReference;

    /**
     * @param userOrGroupReference the reference to iterate over
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *        {@link DocumentReference}
     * @param execution the component used to access the {@link XWikiContext} we use to call oldcore APIs
     */
    public GroupMemberIterator(DocumentReference userOrGroupReference,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver, Execution execution)
    {
        this.userOrGroupReference = userOrGroupReference;
        initialize(explicitDocumentReferenceResolver, getXWikiContext(execution));
    }

    /**
     * @param userOrGroupReference the reference to iterate over
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *        {@link DocumentReference}
     * @param xwikiContext the {@link XWikiContext} we use to call oldcore APIs
     */
    public GroupMemberIterator(DocumentReference userOrGroupReference,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver, XWikiContext xwikiContext)
    {
        this.userOrGroupReference = userOrGroupReference;
        initialize(explicitDocumentReferenceResolver, xwikiContext);
    }

    private void initialize(DocumentReferenceResolver<String> explicitDocumentReferenceResolver,
        XWikiContext xwikiContext)
    {
        this.explicitDocumentReferenceResolver = explicitDocumentReferenceResolver;
        this.xwikicontext = xwikiContext;

        if (userOrGroupReference != null) {
            this.membersIteratorStack.push(Collections.singletonList(userOrGroupReference).iterator());
        }
    }

    @Override
    public boolean hasNext()
    {
        boolean hasNext = false;
        if (!this.membersIteratorStack.isEmpty()) {
            if (this.lookaheadReference == null) {
                DocumentReference currentReference = getNext();
                if (currentReference != null) {
                    this.lookaheadReference = currentReference;
                    hasNext = true;
                }
            }
        }
        return hasNext;
    }

    @Override
    public DocumentReference next()
    {
        DocumentReference currentReference = this.lookaheadReference;
        if (currentReference != null) {
            this.lookaheadReference = null;
        } else {
            currentReference = getNext();
            if (currentReference == null) {
                throw new NoSuchElementException(String.format(
                    "No more users to extract from the passed [%s] reference", this.userOrGroupReference));
            }
        }
        return currentReference;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("remove");
    }

    private DocumentReference getNext()
    {
        DocumentReference currentReference;

        // If there are no more references in the stack then we've already returned everything!
        if (this.membersIteratorStack.isEmpty()) {
            return null;
        }
        Iterator<DocumentReference> currentIterator = this.membersIteratorStack.peek();

        currentReference = currentIterator.next();

        // If it's not a virtual user (guest or superadmin user), then load the document
        if (!currentReference.getLastSpaceReference().getName().equals(RightsManager.DEFAULT_USERORGROUP_SPACE)
            || (!currentReference.getName().equalsIgnoreCase(XWikiRightService.SUPERADMIN_USER)
            && !currentReference.getName().equals(XWikiRightService.GUEST_USER)))
        {
            XWikiDocument document = getFailsafeDocument(currentReference);
            if (!document.isNew()) {
                Pair<DocumentReference, Iterator<DocumentReference>> result = handleUserOrGroupReference(
                    currentReference, currentIterator, document);
                currentReference = result.getLeft();
                currentIterator = result.getRight();
            } else {
                // The document doesn't exist and thus it cannot point to a real user or group, skip it!
                cleanStackIfNeeded(currentIterator);
                currentReference = getNext();
            }
        }

        cleanStackIfNeeded(currentIterator);

        return currentReference;
    }

    private Pair<DocumentReference, Iterator<DocumentReference>> handleUserOrGroupReference(
        DocumentReference currentReference, Iterator<DocumentReference> currentIterator, XWikiDocument document)
    {
        DocumentReference reference = currentReference;
        Iterator<DocumentReference> iterator = currentIterator;

        // Is the reference pointing to a user?
        DocumentReference userClassReference = new DocumentReference(
            document.getDocumentReference().getWikiReference().getName(),
            RightsManager.DEFAULT_USERORGROUP_SPACE, "XWikiUsers");
        boolean isUserReference = document.getXObject(userClassReference) != null;

        // Is the reference pointing to a group?
        // Note that a reference can point to a user reference and to a group reference at the same time!
        DocumentReference groupClassReference = new DocumentReference(
            document.getDocumentReference().getWikiReference().getName(),
            RightsManager.DEFAULT_USERORGROUP_SPACE, "XWikiGroups");
        List<BaseObject> members = document.getXObjects(groupClassReference);
        boolean isGroupReference = members != null && !members.isEmpty();

        // If we have a group reference and a user reference then stack the group members but return the user reference
        // If we have only a group reference then stack the group members and compute the next reference
        // If we have only a user reference then we'll just return it
        // If we have neither a group reference nor a user reference, skip it and get the next reference
        if (isGroupReference) {
            // Extract the references and push them on the stack as an iterator
            this.membersIteratorStack.push(convertToDocumentReferences(members, reference).iterator());
            if (!isUserReference) {
                reference = getNext();
            }
        } else if (!isUserReference) {
            cleanStackIfNeeded(iterator);
            reference = getNext();
        }

        return new ImmutablePair<>(reference, iterator);
    }

    private void cleanStackIfNeeded(Iterator<DocumentReference> currentIterator)
    {
        // If there is no more reference to handle in the current iterator, pop the stack!
        while (!this.membersIteratorStack.isEmpty() && !this.membersIteratorStack.peek().hasNext()) {
            this.membersIteratorStack.pop();
        }
    }

    private XWikiDocument getFailsafeDocument(DocumentReference reference)
    {
        try {
            return this.xwikicontext.getWiki().getDocument(reference, this.xwikicontext);
        } catch (XWikiException e) {
            throw new RuntimeException(String.format("Failed to get document for User or Group [%s] when extracting "
                + "all users for reference [%s]", reference, this.userOrGroupReference));
        }
    }

    private Collection<DocumentReference> convertToDocumentReferences(List<BaseObject> memberObjects,
        DocumentReference currentReference)
    {
        // Handle duplicates by using a Set (last one wins).
        Collection<DocumentReference> members = new LinkedHashSet<>();
        for (BaseObject memberObject : memberObjects) {
            String member = memberObject.getStringValue("member");
            members.add(this.explicitDocumentReferenceResolver.resolve(member, currentReference));
        }
        return members;
    }

    private XWikiContext getXWikiContext(Execution execution)
    {
        XWikiContext xwikiContext = null;
        ExecutionContext executionContext = execution.getContext();
        if (executionContext != null) {
            xwikiContext = (XWikiContext) executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        }
        if (executionContext == null || xwikiContext == null) {
            throw new RuntimeException(String.format("Aborting member extraction from [%s] since no XWiki Context "
                + "was found", this.userOrGroupReference));
        }
        return xwikiContext;
    }
}
