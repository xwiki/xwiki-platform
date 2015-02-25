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

import org.apache.commons.lang3.StringUtils;
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
 * Iterates over all the users found in the passed references (which can be references to groups or to users) and
 * return extracted data from the User for each found user (the extracted data is controlled by the passed
 * {@link UserDataExtractor}. Handles nested groups.
 *
 * @param <T> the type of data returned by the iterator when {@link #next()} is called
 * @version $Id$
 * @since 6.4.2, 7.0M2
 */
public class UserIterator<T> implements Iterator<T>
{
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    private XWikiContext xwikiContext;

    private List<DocumentReference> userAndGroupReferences;

    private List<DocumentReference> excludedUserAndGroupReferences;

    private Stack<Iterator<DocumentReference>> userAndGroupIteratorStack = new Stack<>();

    private T lookaheadValue;

    private UserDataExtractor<T> userDataExtractor;

    /**
     * Recommended if this iterator is called from code using components.
     *
     * @param userAndGroupReferences the list of references (users or groups) to iterate over
     * @param excludedUserAndGroupReferences the list of references (users or groups) to exclude. Can be null.
     * @param userDataExtractor the extractor to use the return the value extracted from the user profile
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *        {@link DocumentReference}
     * @param execution the component used to access the {@link XWikiContext} we use to call oldcore APIs
     */
    public UserIterator(List<DocumentReference> userAndGroupReferences,
        List<DocumentReference> excludedUserAndGroupReferences, UserDataExtractor<T> userDataExtractor,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver, Execution execution)
    {
        initialize(userAndGroupReferences, excludedUserAndGroupReferences, userDataExtractor,
            explicitDocumentReferenceResolver);
        this.xwikiContext = getXWikiContext(execution);
    }

    /**
     * Recommended if this iterator is called from code using components.
     *
     * @param userOrGroupReference the reference (user or group) to iterate over
     * @param userDataExtractor the extractor to use the return the value extracted from the user profile
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *        {@link DocumentReference}
     * @param execution the component used to access the {@link XWikiContext} we use to call oldcore APIs
     */
    public UserIterator(DocumentReference userOrGroupReference, UserDataExtractor<T> userDataExtractor,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver, Execution execution)
    {
        initialize(userOrGroupReference, userDataExtractor, explicitDocumentReferenceResolver);
        this.xwikiContext = getXWikiContext(execution);
    }

    /**
     * Recommended if this iterator is called from old code not using components.
     *
     * @param userAndGroupReferences the list of references (users or groups) to iterate over
     * @param excludedUserAndGroupReferences the list of references (users or groups) to exclude. Can be null.
     * @param userDataExtractor the extractor to use the return the value extracted from the user profile
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *        {@link DocumentReference}
     * @param xwikiContext the {@link XWikiContext} we use to call oldcore APIs
     */
    public UserIterator(List<DocumentReference> userAndGroupReferences,
        List<DocumentReference> excludedUserAndGroupReferences, UserDataExtractor<T> userDataExtractor,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver, XWikiContext xwikiContext)
    {
        initialize(userAndGroupReferences, excludedUserAndGroupReferences, userDataExtractor,
            explicitDocumentReferenceResolver);
        this.xwikiContext = xwikiContext;
    }

    /**
     * Recommended if this iterator is called from old code not using components.
     *
     * @param userOrGroupReference the reference (user or group) to iterate over
     * @param userDataExtractor the extractor to use the return the value extracted from the user profile
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *        {@link DocumentReference}
     * @param xwikiContext the {@link XWikiContext} we use to call oldcore APIs
     */
    public UserIterator(DocumentReference userOrGroupReference, UserDataExtractor<T> userDataExtractor,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver, XWikiContext xwikiContext)
    {
        initialize(userOrGroupReference, userDataExtractor, explicitDocumentReferenceResolver);
        this.xwikiContext = xwikiContext;
    }

    private void initialize(DocumentReference userOrGroupReference, UserDataExtractor<T> userDataExtractor,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver)
    {
        List<DocumentReference> references = userOrGroupReference == null
            ? Collections.<DocumentReference>emptyList() : Collections.singletonList(userOrGroupReference);
        initialize(references, null, userDataExtractor, explicitDocumentReferenceResolver);
    }

    private void initialize(List<DocumentReference> userAndGroupReferences,
        List<DocumentReference> excludedUserAndGroupReferences, UserDataExtractor<T> userDataExtractor,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver)
    {
        this.userAndGroupReferences = userAndGroupReferences;
        this.excludedUserAndGroupReferences = excludedUserAndGroupReferences;
        if (excludedUserAndGroupReferences == null) {
            this.excludedUserAndGroupReferences = Collections.emptyList();
        }
        this.explicitDocumentReferenceResolver = explicitDocumentReferenceResolver;
        this.userDataExtractor = userDataExtractor;
        if (userAndGroupReferences != null && !userAndGroupReferences.isEmpty()) {
            this.userAndGroupIteratorStack.push(userAndGroupReferences.iterator());
        }
    }

    @Override
    public boolean hasNext()
    {
        boolean hasNext = false;
        if (!this.userAndGroupIteratorStack.isEmpty()) {
            if (this.lookaheadValue == null) {
                T currentValue = getNext();
                if (currentValue != null) {
                    this.lookaheadValue = currentValue;
                    hasNext = true;
                }
            }
        }
        return hasNext;
    }

    @Override
    public T next()
    {
        T currentValue = this.lookaheadValue;
        if (currentValue != null) {
            this.lookaheadValue = null;
        } else {
            currentValue = getNext();
            if (currentValue == null) {
                throw new NoSuchElementException(String.format(
                    "No more users to extract from the passed references [%s]", serializeUserAndGroupReferences()));
            }
        }
        return currentValue;
    }

    private String serializeUserAndGroupReferences()
    {
        StringBuilder buffer = new StringBuilder();
        Iterator<DocumentReference> iterator = this.userAndGroupReferences.iterator();
        while (iterator.hasNext()) {
            DocumentReference reference = iterator.next();
            buffer.append('[').append(reference).append(']');
            if (iterator.hasNext()) {
                buffer.append(',').append(' ');
            }
        }
        return buffer.toString();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("remove");
    }

    private T getNext()
    {
        T currentValue;
        DocumentReference currentReference;

        // If there are no more references in the stack then we've already returned everything!
        if (this.userAndGroupIteratorStack.isEmpty()) {
            return null;
        }

        Iterator<DocumentReference> currentIterator = this.userAndGroupIteratorStack.peek();

        currentReference = currentIterator.next();

        // If the reference is in the excluded list, skip it!
        if (this.excludedUserAndGroupReferences.contains(currentReference)) {
            cleanStackIfNeeded(currentIterator);
            return getNext();
        }

        // If it's not a virtual user (guest or superadmin user), then load the document
        if (isSuperAdmin(currentReference)) {
            currentValue = this.userDataExtractor.extractFromSuperadmin(currentReference);
        } else if (isGuest(currentReference)) {
            currentValue = this.userDataExtractor.extractFromGuest(currentReference);
        } else {
            XWikiDocument document = getFailsafeDocument(currentReference);
            if (!document.isNew()) {
                Pair<T, Iterator<DocumentReference>> result = handleUserOrGroupReference(
                    currentReference, currentIterator, document);
                currentValue = result.getLeft();
                currentIterator = result.getRight();
            } else {
                // The document doesn't exist and thus it cannot point to a real user or group, skip it!
                cleanStackIfNeeded(currentIterator);
                currentValue = getNext();
            }
        }

        cleanStackIfNeeded(currentIterator);

        return currentValue;
    }

    private boolean isSuperAdmin(DocumentReference reference)
    {
        return reference.getLastSpaceReference().getName().equals(RightsManager.DEFAULT_USERORGROUP_SPACE)
            && reference.getName().equalsIgnoreCase(XWikiRightService.SUPERADMIN_USER);
    }

    private boolean isGuest(DocumentReference reference)
    {
        return reference.getLastSpaceReference().getName().equals(RightsManager.DEFAULT_USERORGROUP_SPACE)
            && reference.getName().equals(XWikiRightService.GUEST_USER);
    }

    private Pair<T, Iterator<DocumentReference>> handleUserOrGroupReference(
        DocumentReference currentReference, Iterator<DocumentReference> currentIterator, XWikiDocument document)
    {
        T value;

        // Is the reference pointing to a user?
        DocumentReference userClassReference = new DocumentReference(
            document.getDocumentReference().getWikiReference().getName(),
            RightsManager.DEFAULT_USERORGROUP_SPACE, "XWikiUsers");
        BaseObject userObject = document.getXObject(userClassReference);
        boolean isUserReference = userObject != null;

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
            Collection<DocumentReference> groupMemberReferences =
                convertToDocumentReferences(members, currentReference);
            if (!groupMemberReferences.isEmpty()) {
                this.userAndGroupIteratorStack.push(groupMemberReferences.iterator());
            } else {
                cleanStackIfNeeded(currentIterator);
            }
            if (!isUserReference) {
                value = getNext();
            } else {
                value = getValue(currentIterator, currentReference, document, userObject);
            }
        } else if (!isUserReference) {
            cleanStackIfNeeded(currentIterator);
            value = getNext();
        } else {
            value = getValue(currentIterator, currentReference, document, userObject);
        }

        return new ImmutablePair<>(value, currentIterator);
    }

    private T getValue(Iterator<DocumentReference> iterator, DocumentReference reference, XWikiDocument document,
        BaseObject userObject)
    {
        T value = this.userDataExtractor.extract(reference, document, userObject);
        if (value == null) {
            cleanStackIfNeeded(iterator);
            value = getNext();
        }
        return value;
    }

    private void cleanStackIfNeeded(Iterator<DocumentReference> currentIterator)
    {
        // If there is no more reference to handle in the current iterator, pop the stack!
        while (!this.userAndGroupIteratorStack.isEmpty() && !this.userAndGroupIteratorStack.peek().hasNext()) {
            this.userAndGroupIteratorStack.pop();
        }
    }

    private XWikiDocument getFailsafeDocument(DocumentReference reference)
    {
        try {
            return getXwikiContext().getWiki().getDocument(reference, getXwikiContext());
        } catch (XWikiException e) {
            throw new RuntimeException(String.format("Failed to get document for User or Group [%s] when extracting "
                + "all users for the references [%s]", reference, serializeUserAndGroupReferences()), e);
        }
    }

    private Collection<DocumentReference> convertToDocumentReferences(List<BaseObject> memberObjects,
        DocumentReference currentReference)
    {
        // Handle duplicates by using a Set (last one wins).
        Collection<DocumentReference> members = new LinkedHashSet<>();
        for (BaseObject memberObject : memberObjects) {
            String member = memberObject.getStringValue("member");
            // If the member is empty, discard it!
            if (!StringUtils.isBlank(member)) {
                DocumentReference resolvedReference =
                    this.explicitDocumentReferenceResolver.resolve(member, currentReference);
                if (!resolvedReference.equals(currentReference)) {
                    members.add(resolvedReference);
                }
            }
        }
        return members;
    }

    protected XWikiContext getXwikiContext()
    {
        return this.xwikiContext;
    }

    private XWikiContext getXWikiContext(Execution execution)
    {
        XWikiContext context = null;
        ExecutionContext executionContext = execution.getContext();
        if (executionContext != null) {
            context = (XWikiContext) executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        }
        if (executionContext == null || context == null) {
            throw new RuntimeException(String.format("Aborting member extraction from passed references [%s] since "
                + "no XWiki Context was found", serializeUserAndGroupReferences()));
        }
        return context;
    }
}
