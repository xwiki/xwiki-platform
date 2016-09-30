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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;

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
 * @since 6.4.2
 * @since 7.0M2
 */
public class UserIterator<T> implements Iterator<T>
{
    /**
     * The local reference of the class containing group member.
     */
    static final LocalDocumentReference GROUP_CLASS_REF =
        new LocalDocumentReference(RightsManager.DEFAULT_USERORGROUP_SPACE, "XWikiGroups");

    /**
     * The local reference of the class containing user profile.
     */
    static final LocalDocumentReference USER_CLASS_REF =
        new LocalDocumentReference(RightsManager.DEFAULT_USERORGROUP_SPACE, "XWikiUsers");


    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    private final Execution execution;

    private final XWikiContext xwikiContext;

    private List<DocumentReference> userAndGroupReferences;

    private List<DocumentReference> excludedUserAndGroupReferences;

    private List<DocumentReference> processedGroups = new ArrayList<>();

    private Deque<Iterator<DocumentReference>> userAndGroupIteratorStack = new ArrayDeque<>();

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
        this.execution = execution;
        this.xwikiContext = null;
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
        this.execution = execution;
        this.xwikiContext = null;
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
        this.execution = null;
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
        this.execution = null;
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
        if (this.lookaheadValue == null) {
            this.lookaheadValue = getNext();
        }
        return this.lookaheadValue != null;
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
        T currentValue = null;

        // Unless there are no more references in the stack we have not tried everything, try getting a user
        // from the first element at the top of the stack.
        while (currentValue == null && !this.userAndGroupIteratorStack.isEmpty()) {
            currentValue = getNextUser(this.userAndGroupIteratorStack.peek());
        }

        return currentValue;
    }


    private T getNextUser(Iterator<DocumentReference> currentIterator)
    {
        T currentValue = null;

        DocumentReference currentReference = currentIterator.next();

        // If the reference is not in the excluded list (else skip it!)
        if (!this.excludedUserAndGroupReferences.contains(currentReference)) {
            // If it's not a virtual user (guest or superadmin user), then load the document
            if (isSuperAdmin(currentReference)) {
                currentValue = this.userDataExtractor.extractFromSuperadmin(currentReference);
            } else if (isGuest(currentReference)) {
                currentValue = this.userDataExtractor.extractFromGuest(currentReference);
            } else {
                XWikiDocument document = getFailsafeDocument(currentReference);
                // If we found the document, try to get a user from the document, and stack group members if any
                if (document != null && !document.isNew()) {
                    currentValue = handleUserOrGroupReference(currentReference, document);
                }
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

    private T handleUserOrGroupReference(DocumentReference currentReference, XWikiDocument document)
    {
        T value = null;

        // Is the reference pointing to a user?
        BaseObject userObject = document.getXObject(USER_CLASS_REF);
        boolean isUserReference = userObject != null;

        // Is the reference pointing to a group?
        // Note that a reference can point to a user reference and to a group reference at the same time!
        List<BaseObject> members = document.getXObjects(GROUP_CLASS_REF);
        boolean isGroupReference = members != null && !members.isEmpty();

        // If we have a group reference then stack the group members
        if (isGroupReference) {
            // Ensure groups are visited only once to prevent potential infinite loops
            if (!processedGroups.contains(currentReference)) {
                processedGroups.add(currentReference);

                // Extract the references and push them on the stack as an iterator
                Collection<DocumentReference> groupMemberReferences =
                    convertToDocumentReferences(members, currentReference);
                if (!groupMemberReferences.isEmpty()) {
                    this.userAndGroupIteratorStack.push(groupMemberReferences.iterator());
                }
            }
        }
        // If we have a user reference then we'll just return it
        if (isUserReference) {
            value = this.userDataExtractor.extract(currentReference, document, userObject);
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
            // If the member object is null, discard this entry!
            if (memberObject == null) {
                continue;
            }

            String member = memberObject.getStringValue("member");

            // If the member reference is empty, discard this entry!
            if (StringUtils.isBlank(member)) {
                continue;
            }

            DocumentReference resolvedReference =
                this.explicitDocumentReferenceResolver.resolve(member, currentReference);
            if (!resolvedReference.equals(currentReference)) {
                members.add(resolvedReference);
            }
        }
        return members;
    }

    protected XWikiContext getXwikiContext()
    {
        if (execution == null) {
            return this.xwikiContext;
        }

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
