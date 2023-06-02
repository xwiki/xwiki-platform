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
package org.xwiki.security.authorization.testwikibuilding;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.legacy.ClassImposteriser;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiGroupsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiGroupService;

/**
 * Utility class for mocking the objects as needed to test both the old and the caching right service implementations.
 * 
 * @since 4.2
 * @version $Id$
 */
// TODO: replace with MockitoOldcoreRule
public class LegacyTestWiki extends AbstractTestWiki
{
    private final boolean legacymock;

    private final XWikiContext context;

    private final XWiki xwiki;

    private final Mockery mockery;

    private final DocumentReferenceResolver<String> documentReferenceResolver;

    private final EntityReferenceSerializer<String> entityReferenceSerializer;

    private final ObservationManager observationManager;

    protected WikiDescriptorManager wikiDescriptorManager;

    private final Map<String, TestWiki> wikis = new HashMap<String, TestWiki>();

    /**
     * @param mockery The mockery.
     * @param componentManager The component manager.
     * @param testWikiFilename The file name of the test wiki configuration.
     */
    public LegacyTestWiki(Mockery mockery, ComponentManager componentManager, String testWikiFilename,
        boolean legacymock) throws Exception
    {
        this.legacymock = legacymock;
        this.mockery = mockery;

        this.documentReferenceResolver = componentManager.getInstance(DocumentReferenceResolver.TYPE_STRING);
        this.entityReferenceSerializer = componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);
        this.observationManager = componentManager.getInstance(ObservationManager.class);

        mockery.setImposteriser(ClassImposteriser.INSTANCE);

        context = new XWikiContext();

        xwiki = mockery.mock(XWiki.class);
        this.context.setWiki(xwiki);

        if (!componentManager.hasComponent(WikiDescriptorManager.class)) {
            DefaultComponentDescriptor<WikiDescriptorManager> descriptor = new DefaultComponentDescriptor<>();
            descriptor.setRoleType(WikiDescriptorManager.class);
            this.wikiDescriptorManager = mockery.mock(WikiDescriptorManager.class);
            componentManager.registerComponent(descriptor, this.wikiDescriptorManager);
        }

        final XWikiGroupService groupService = mockery.mock(XWikiGroupService.class);

        loadTestWiki(testWikiFilename);

        if (this.context.getMainXWiki() == null) {
            throw new RuntimeException("None of the declared wikis had attribute mainWiki=\"true\"!");
        }

        this.context.setWikiId(this.context.getMainXWiki());

        mockery.checking(new Expectations()
        {
            {
                // Expectations for XWiki

                allowing(xwiki).getDatabase();
                will(returnValue(context.getMainXWiki()));
                allowing(xwiki).isReadOnly();
                will(new CustomAction("indicate wether the wiki is read only")
                {
                    @Override
                    public Object invoke(Invocation invocation)
                    {
                        return isReadOnly(context.getWikiId());
                    }
                });
                allowing(xwiki).getMaxRecursiveSpaceChecks(with(any(XWikiContext.class)));
                will(returnValue(100));
                allowing(xwiki).getDocument(with(any(DocumentReference.class)), with(any(XWikiContext.class)));
                will(new CustomAction("return a mocked document")
                {
                    @Override
                    public Object invoke(Invocation invocation)
                    {
                        DocumentReference documentReference = (DocumentReference) invocation.getParameter(0);
                        return getDocument(documentReference);
                    }
                });
                allowing(xwiki).getDocument(with(any(EntityReference.class)), with(any(XWikiContext.class)));
                will(new CustomAction("return a mocked document")
                {
                    @Override
                    public Object invoke(Invocation invocation)
                    {
                        EntityReference documentReference = (EntityReference) invocation.getParameter(0);
                        return getDocument(documentReference, (XWikiContext) invocation.getParameter(1));
                    }
                });
                allowing(xwiki).getWikiOwner(with(aNonNull(String.class)), with(any(XWikiContext.class)));
                will(new CustomAction("return the wiki owner")
                {
                    @Override
                    public Object invoke(Invocation invocation)
                    {
                        String wikiName = (String) invocation.getParameter(0);
                        TestWiki wiki = wikis.get(wikiName);
                        return wiki.getOwner();
                    }
                });
                allowing(xwiki).getGroupService(with(any(XWikiContext.class)));
                will(returnValue(groupService));

                // Expectations for XWikiGroupService

                allowing(groupService).getAllGroupsReferencesForMember(with(any(DocumentReference.class)),
                    with(any(Integer.class)), with(any(Integer.class)), with(any(XWikiContext.class)));
                will(new CustomAction("return a collection of member profile document references")
                {
                    @Override
                    public Object invoke(Invocation invocation)
                    {
                        return getAllGroupReferences((DocumentReference) invocation.getParameter(0));
                    }
                });

                // Expectations for WikiDescriptorManager
                allowing(wikiDescriptorManager).getCurrentWikiId();
                will(new CustomAction("get the current wiki id")
                {
                    @Override
                    public Object invoke(Invocation invocation)
                    {
                        return context.getWikiId();
                    }
                });

                // Expectations needed for old implementation

                allowing(xwiki).getXWikiPreference(with(any(String.class)), with(equal("")),
                    with(any(XWikiContext.class)));
                will(returnValue("false"));
                allowing(xwiki).getXWikiPreferenceAsInt(with(any(String.class)), with(0),
                    with(any(XWikiContext.class)));
                will(returnValue(0));
                allowing(xwiki).getSpacePreference(with(any(String.class)), with(equal("")),
                    with(any(XWikiContext.class)));
                will(returnValue("false"));
                allowing(xwiki).getSpacePreferenceAsInt(with(any(String.class)), with(0),
                    with(any(XWikiContext.class)));
                will(returnValue(0));

            }
        });

        if (legacymock) {
            mockery.checking(new Expectations()
            {
                {
                    allowing(xwiki).getDocument(with(any(String.class)), with(any(XWikiContext.class)));
                    will(new CustomAction("return a mocked document")
                    {
                        @Override
                        public Object invoke(Invocation invocation)
                        {
                            String documentName = (String) invocation.getParameter(0);
                            return getDocument(documentName);
                        }
                    });
                    allowing(xwiki).getDocument(with(any(String.class)), with(any(String.class)),
                        with(any(XWikiContext.class)));
                    will(new CustomAction("return a mocked document")
                    {
                        @Override
                        public Object invoke(Invocation invocation)
                        {
                            String spaceName = (String) invocation.getParameter(0);
                            String documentName = (String) invocation.getParameter(1);
                            return getDocument(spaceName, documentName);
                        }
                    });
                }
            });
        }

    }

    public String getMainWikiName()
    {
        return context.getMainXWiki();
    }

    @Override
    public HasWikiContents addWiki(String name, String owner, boolean isMainWiki, boolean isReadOnly, String alt)
    {

        if (isMainWiki) {
            if (context.getMainXWiki() != null) {
                throw new RuntimeException("Only one wiki can be the main wiki!");
            }

            context.setMainXWiki(name);
        }

        return mockWiki(name, owner, isReadOnly, isMainWiki, alt);
    }

    public void setUser(String username)
    {
        context.setUser(username);
    }

    public WikiReference getCurrentWikiReference()
    {
        String currentWikiId = this.context.getWikiId();

        return currentWikiId != null ? new WikiReference(currentWikiId) : null;
    }

    public void setSdoc(String sdocFullname)
    {
        XWikiDocument sdoc;
        if (sdocFullname != null) {
            sdoc = new XWikiDocument(this.documentReferenceResolver.resolve(sdocFullname, getCurrentWikiReference()));
        } else {
            sdoc = null;
        }

        setSdoc(sdoc);
    }

    public void setSdoc(XWikiDocument sdoc)
    {
        this.context.put("sdoc", sdoc);
    }

    public void setDoc(String docFullname)
    {
        XWikiDocument doc;
        if (docFullname != null) {
            doc = new XWikiDocument(this.documentReferenceResolver.resolve(docFullname, getCurrentWikiReference()));
        } else {
            doc = null;
        }

        this.context.setDoc(doc);
    }

    private TestWiki mockWiki(String name, String owner, boolean isReadOnly, boolean isMainWiki, String alt)
    {
        TestWiki wiki = wikis.get(name);

        if (wiki == null) {
            wiki = new TestWiki(name, owner, isReadOnly, isMainWiki, alt);
            wikis.put(name, wiki);
        }

        return wiki;
    }

    private Collection<DocumentReference> getAllGroupReferences(final DocumentReference userReference)
    {
        final TestWiki wiki = wikis.get(context.getWikiId());

        if (wiki == null) {
            return Collections.<DocumentReference>emptySet();
        }

        final Collection<String> groupNames = wiki.getGroupsForUser(userReference);

        final SpaceReference userSpaceReference = new SpaceReference("XWiki", new WikiReference(wiki.getName()));

        return new AbstractCollection<DocumentReference>()
        {

            @Override
            public int size()
            {
                return groupNames.size();
            }

            @Override
            public Iterator<DocumentReference> iterator()
            {
                return new Iterator<DocumentReference>()
                {

                    private final Iterator<String> groupNamesIterator = groupNames.iterator();

                    @Override
                    public boolean hasNext()
                    {
                        return groupNamesIterator.hasNext();
                    }

                    @Override
                    public DocumentReference next()
                    {
                        String groupName = groupNamesIterator.next();

                        return documentReferenceResolver.resolve(groupName, userSpaceReference);
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public XWikiContext getXWikiContext()
    {
        return context;
    }

    public boolean isReadOnly(String wikiName)
    {
        return wikis.get(wikiName).isReadOnly();
    }

    public String getWikiPrettyName(WikiReference wikiReference)
    {
        TestWiki wiki = wikis.get(wikiReference.getName());

        return (wiki != null) ? wiki.getPrettyName() : wikiReference.getName();
    }

    public String getSpacePrettyName(SpaceReference spaceReference)
    {
        TestWiki wiki = wikis.get(spaceReference.getParent().getName());

        if (wiki == null) {
            return null;
        }

        TestSpace space = wiki.getTestSpace(spaceReference.getName());

        return (space != null) ? space.getPrettyName() : spaceReference.getName();
    }

    public String getDocumentPrettyName(DocumentReference documentReference)
    {
        TestWiki wiki = wikis.get(documentReference.getWikiReference().getName());

        if (wiki == null) {
            return null;
        }

        TestDocument doc = wiki.getTestDocument(documentReference.getParent().getName(), documentReference.getName());

        return (doc != null) ? doc.getPrettyName() : documentReference.getName();
    }

    private XWikiDocument getDocument(DocumentReference documentReference)
    {
        TestWiki wiki = wikis.get(documentReference.getWikiReference().getName());
        return wiki.getDocument(documentReference);
    }

    private XWikiDocument getDocument(EntityReference entityReference, XWikiContext context)
    {
        return getDocument(new DocumentReference(entityReference.appendParent(new WikiReference(context.getWikiId()))));
    }

    private XWikiDocument getDocument(String name)
    {
        DocumentReference documentReference = documentReferenceResolver.resolve(name, getCurrentWikiReference());

        TestWiki wiki = wikis.get(documentReference.getWikiReference().getName());

        if (wiki == null) {
            return null;
        }

        return wiki.getDocument(documentReference);
    }

    private XWikiDocument getDocument(String spaceName, String documentName)
    {
        DocumentReference documentReference = new DocumentReference(this.context.getWikiId(), spaceName, documentName);

        TestWiki wiki = wikis.get(documentReference.getWikiReference().getName());

        if (wiki == null) {
            return null;
        }

        return wiki.getDocument(documentReference);
    }

    private class TestWiki extends TestAcl implements HasWikiContents
    {
        private final SpaceReference userSpaceReference;

        private final String name;

        private final String owner;

        private final String alt;

        private final Map<String, TestSpace> spaces = new HashMap<String, TestSpace>();

        private final Set<String> users = new HashSet<String>();

        private final Map<String, Set<String>> groups = new HashMap<String, Set<String>>();

        private final Map<String, Set<String>> groupsForUser = new HashMap<String, Set<String>>();

        private final boolean isReadOnly;

        private final boolean isMainWiki;

        TestWiki(String name, String owner, boolean isReadOnly)
        {
            this(name, owner, isReadOnly, false);
        }

        TestWiki(String name, String owner, boolean isReadOnly, boolean isMainWiki)
        {
            this(name, owner, isReadOnly, isMainWiki, null);
        }

        TestWiki(String name, String owner, boolean isReadOnly, boolean isMainWiki, String alt)
        {
            this.name = name;
            this.owner = owner;
            this.isReadOnly = isReadOnly;
            this.isMainWiki = isMainWiki;
            this.alt = alt;
            this.userSpaceReference = new SpaceReference("XWiki", new WikiReference(name));

            // The XWikiPreferences document always exist (unless explicitly deleted!) since it will be created on
            // application startup or at wiki creation.
            mockDocument("XWiki", "XWikiPreferences", "XWiki.Admin", false);
        }

        @Override
        public HasDocuments addSpace(String name, String alt)
        {
            return mockSpace(name, alt);
        }

        TestDocument mockDocument(String spaceName, String name, String creator, boolean isNew)
        {
            TestSpace space = mockSpace(spaceName);

            return space.mockDocument(name, creator, isNew, alt);
        }

        TestSpace mockSpace(String name)
        {
            return mockSpace(name, null);
        }

        TestSpace mockSpace(String name, String alt)
        {
            TestSpace space = spaces.get(name);

            if (space == null) {
                space = new TestSpace(name, this, alt);
                spaces.put(name, space);
            }

            return space;
        }

        XWikiDocument removeDocument(DocumentReference documentReference)
        {
            return removeDocument(documentReference.getParent().getName(), documentReference.getName());
        }

        XWikiDocument removeDocument(String spaceName, String documentName)
        {
            TestSpace space = mockSpace(spaceName);

            return space.removeDocument(documentName);
        }

        XWikiDocument getDocument(DocumentReference documentReference)
        {
            TestSpace space = mockSpace(documentReference.getParent().getName());

            return space.getDocument(documentReference);
        }

        TestSpace getTestSpace(String spaceName)
        {
            if (spaces.containsKey(spaceName)) {
                TestSpace space = mockSpace(spaceName);

                return space;
            }

            return null;
        }

        TestDocument getTestDocument(String spaceName, String documentName)
        {
            if (spaces.containsKey(spaceName)) {
                TestSpace space = mockSpace(spaceName);

                return space.getTestDocument(documentName);
            }

            return null;
        }

        @Override
        EntityType getType()
        {
            if (isMainWiki) {
                return null; // FARM
            } else {
                return EntityType.WIKI;
            }
        }

        @Override
        String getName()
        {
            return name;
        }

        @Override
        String getPrettyName()
        {
            return (this.alt != null) ? this.alt : this.name;
        }

        String getOwner()
        {
            return owner;
        }

        public boolean isReadOnly()
        {
            return isReadOnly;
        }

        Collection<String> getGroupsForUser(String userName)
        {
            DocumentReference userDoc = documentReferenceResolver.resolve(userName, userSpaceReference);
            return getGroupsForUser(userDoc);
        }

        Collection<String> getGroupsForUser(DocumentReference userDoc)
        {
            Set<String> groups;
            if (userDoc.getWikiReference().getName().equals(getName())) {
                groups = groupsForUser.get(userDoc.getName());
            } else {
                groups = groupsForUser.get(entityReferenceSerializer.serialize(userDoc));
            }

            return groups == null ? Collections.<String>emptySet() : groups;
        }

        void notifyCreatedDocument(XWikiDocument document)
        {
            // Send event
            // Do not notify during parsing !
            if (context.getMainXWiki() == null) {
                observationManager.notify(new DocumentCreatedEvent(document.getDocumentReference()), document, context);
            }
        }

        void notifyDeleteDocument(XWikiDocument document)
        {
            XWikiDocument newDocument = new XWikiDocument(document.getDocumentReference());
            newDocument.setOriginalDocument(document);

            // Send event
            observationManager.notify(new DocumentDeletedEvent(newDocument.getDocumentReference()), newDocument,
                context);
        }

        @Override
        public void addUser(String userName)
        {
            DocumentReference userDoc = documentReferenceResolver.resolve(userName, userSpaceReference);
            users.add(userDoc.getName());
            if (groupsForUser.get(userDoc.getName()) == null) {
                groupsForUser.put(userDoc.getName(), new HashSet<String>());
            }

            TestSpace testSpace = mockSpace(userDoc.getParent().getName());

            UserTestDocument userTestDocument = new UserTestDocument(userDoc.getName(), testSpace, null, false);

            testSpace.documents.put(userDoc.getName(), userTestDocument);

            // Send event
            notifyCreatedDocument(userTestDocument.getDocument());
        }

        void deleteUser(String userName)
        {
            DocumentReference userDoc = documentReferenceResolver.resolve(userName, userSpaceReference);
            if (users.contains(userDoc.getName())) {
                users.remove(userDoc.getName());
                groupsForUser.remove(userDoc.getName());
                for (Map.Entry<String, Set<String>> entry : groups.entrySet()) {
                    entry.getValue().remove(userDoc.getName());
                    ((GroupTestDocument) getTestDocument(userDoc.getParent().getName(), entry.getKey()))
                        .removeUser(userDoc.getName());
                }

                // Make sure user document is removed
                XWikiDocument userDocument = removeDocument(userDoc.getParent().getName(), userDoc.getName());

                // Send event
                notifyDeleteDocument(userDocument);
            }
        }

        @Override
        public GroupTestDocument addGroup(final String groupName)
        {
            DocumentReference groupDoc = documentReferenceResolver.resolve(groupName, userSpaceReference);
            Set<String> groupMembers = groups.get(groupDoc.getName());

            if (groupMembers == null) {
                groupMembers = new HashSet<String>();
                groups.put(groupDoc.getName(), groupMembers);
            }

            TestSpace testSpace = mockSpace(groupDoc.getParent().getName());

            GroupTestDocument groupTestDocument = new GroupTestDocument(groupDoc.getName(), testSpace, null, false);

            testSpace.documents.put(groupDoc.getName(), groupTestDocument);

            // Send event
            notifyCreatedDocument(groupTestDocument.getDocument());

            return groupTestDocument;
        }

        void deleteGroup(String groupName)
        {
            DocumentReference groupDoc = documentReferenceResolver.resolve(groupName, userSpaceReference);
            if (groups.containsKey(groupDoc.getName())) {
                groups.remove(groupDoc.getName());
                for (Set<String> groups : groupsForUser.values()) {
                    groups.remove(groupDoc.getName());
                }

                // Make sure group document is removed
                XWikiDocument groupDocument = removeDocument(groupDoc.getParent().getName(), groupDoc.getName());

                // Send event
                notifyDeleteDocument(groupDocument);
            }
        }

        TestDocument addDocument(DocumentReference documentReference, String creator, boolean isNew)
        {
            TestDocument document = mockDocument(documentReference.getParent().getName(), creator, creator, isNew);

            // Send event
            notifyCreatedDocument(document.getDocument());

            return document;
        }

        void deleteDocument(DocumentReference documentReference)
        {
            XWikiDocument document = removeDocument(documentReference);

            if (document != null) {
                // Send event
                notifyDeleteDocument(document);
            }
        }
    }

    private class TestSpace extends TestAcl implements HasDocuments
    {
        private final String name;

        private final String alt;

        private final TestWiki wiki;

        private final Map<String, TestDocument> documents = new HashMap<String, TestDocument>();

        TestSpace(String name, TestWiki wiki)
        {
            this(name, wiki, null);
        }

        TestSpace(String name, TestWiki wiki, String alt)
        {
            this.name = name;
            this.alt = alt;
            this.wiki = wiki;
        }

        @Override
        public HasAcl addDocument(String name, String creator, String alt)
        {
            return mockDocument(name, creator, false, alt);
        }

        @Override
        public XWikiDocument removeDocument(String name)
        {
            TestDocument document = documents.get(name);

            if (document == null) {
                return null;
            }

            return document.getDocument();
        }

        TestDocument mockDocument(String name, String creator, boolean isNew)
        {
            return mockDocument(name, creator, isNew, null);
        }

        TestDocument mockDocument(String name, String creator, boolean isNew, String alt)
        {
            TestDocument document = getTestDocument(name);

            if (document == null) {
                if (creator == null) {
                    creator = "XWiki.Admin";
                }
                document = new TestDocument(name, this, creator, isNew, alt);
                documents.put(name, document);
            }

            return document;
        }

        XWikiDocument getDocument(DocumentReference documentReference)
        {
            TestDocument document = mockDocument(documentReference.getName(), context.getUser(), true);

            return document.getDocument();
        }

        public TestDocument getTestDocument(String name)
        {
            return documents.get(name);
        }

        @Override
        EntityType getType()
        {
            return EntityType.SPACE;
        }

        @Override
        String getName()
        {
            return name;
        }

        @Override
        String getPrettyName()
        {
            return (this.alt != null) ? this.alt : this.name;
        }

        TestWiki getWiki()
        {
            return wiki;
        }
    }

    private class UserTestDocument extends TestDocument
    {
        private BaseObject userObject;

        UserTestDocument(String name, TestSpace space, String creator, Boolean isNew)
        {
            this(name, space, creator, isNew, null);
        }

        UserTestDocument(String name, TestSpace space, String creator, Boolean isNew, String alt)
        {
            super(name, space, creator, isNew, alt);

            this.userObject = mockUserBaseObject();

            mockery.checking(new Expectations()
            {
                {
                    allowing(mockedDocument).getXObject(with(equal(new LocalDocumentReference("XWiki", "XWikiUsers"))));
                    will(new CustomAction("return the user object")
                    {
                        @Override
                        public Object invoke(Invocation invocation)
                        {
                            return userObject;
                        }
                    });
                }
            });
        }

        private BaseObject mockUserBaseObject()
        {
            ++objectNumber;

            final BaseObject userBaseObject = mockery.mock(BaseObject.class, getName() + objectNumber);

            return userBaseObject;
        }

        @Override
        public Map<DocumentReference, List<BaseObject>> getLegacyXObjects()
        {
            Map<DocumentReference, List<BaseObject>> xobjects = super.getLegacyXObjects();

            xobjects.put(new DocumentReference(this.space.wiki.getName(), "XWiki", "XWikiUsers"),
                Arrays.asList(this.userObject));

            return xobjects;
        }
    }

    private class GroupTestDocument extends TestDocument implements HasUsers
    {
        private Map<String, BaseObject> memberObjects = new LinkedHashMap<String, BaseObject>();

        GroupTestDocument(String name, TestSpace space, String creator, Boolean isNew)
        {
            this(name, space, creator, isNew, null);
        }

        GroupTestDocument(String name, TestSpace space, String creator, Boolean isNew, String alt)
        {
            super(name, space, creator, isNew, alt);

            mockery.checking(new Expectations()
            {
                {
                    allowing(mockedDocument)
                        .getXObjects(with(equal(new LocalDocumentReference("XWiki", "XWikiGroups"))));
                    will(new CustomAction("return a vector of group members")
                    {
                        @Override
                        public Object invoke(Invocation invocation)
                        {
                            return new Vector<BaseObject>(memberObjects.values());
                        }
                    });
                }
            });
        }

        private BaseObject mockGroupBaseObject(String user)
        {
            ++objectNumber;

            final BaseObject userBaseObject = mockery.mock(BaseObject.class, getName() + objectNumber);

            return userBaseObject;
        }

        @Override
        public Map<DocumentReference, List<BaseObject>> getLegacyXObjects()
        {
            Map<DocumentReference, List<BaseObject>> xobjects = super.getLegacyXObjects();

            xobjects.put(new DocumentReference(this.space.wiki.getName(), "XWiki", "XWikiUsers"),
                new ArrayList<BaseObject>(this.memberObjects.values()));

            return xobjects;
        }

        @Override
        public void addUser(String userName)
        {
            TestWiki testWiki = space.getWiki();
            DocumentReference userDoc = documentReferenceResolver.resolve(userName,
                new SpaceReference("XWiki", new WikiReference(testWiki.getName())));

            String uname;
            if (userDoc.getWikiReference().getName().equals(testWiki.getName())) {
                uname = userDoc.getName();
            } else {
                uname = entityReferenceSerializer.serialize(userDoc);
            }

            Set<String> groups = testWiki.groupsForUser.get(uname);
            if (groups == null) {
                groups = new HashSet<String>();
                testWiki.groupsForUser.put(uname, groups);
            }
            groups.add(getName());

            testWiki.groups.get(getName()).add(uname);

            this.memberObjects.put(uname, mockGroupBaseObject(uname));
        }

        public void removeUser(String userName)
        {
            TestWiki testWiki = space.getWiki();
            DocumentReference userDoc = documentReferenceResolver.resolve(userName,
                new SpaceReference("XWiki", new WikiReference(testWiki.getName())));

            String uname;
            if (userDoc.getWikiReference().getName().equals(testWiki.getName())) {
                uname = userDoc.getName();
            } else {
                uname = entityReferenceSerializer.serialize(userDoc);
            }

            this.memberObjects.remove(uname);
        }
    }

    private class TestDocument extends TestAcl
    {
        protected final XWikiDocument mockedDocument;

        protected final TestSpace space;

        protected final String name;

        protected final String alt;

        protected final String creator;

        TestDocument(final String name, final TestSpace space, final String creator, final Boolean isNew)
        {
            this(name, space, creator, isNew, null);
        }

        TestDocument(final String name, final TestSpace space, final String creator, final Boolean isNew, String alt)
        {
            this.space = space;
            this.name = name;
            this.creator = creator;
            this.alt = alt;

            mockedDocument = mockery.mock(XWikiDocument.class, new Formatter()
                .format("%s:%s.%s", getSpace().getWiki().getName(), getSpace().getName(), getName()).toString());

            final DocumentReference documentReference =
                new DocumentReference(getSpace().getWiki().getName(), getSpace().getName(), getName());

            mockery.checking(new Expectations()
            {
                {
                    allowing(mockedDocument)
                        .getXObjects(with(equal(new LocalDocumentReference("XWiki", "XWikiRights"))));
                    will(new CustomAction("return a vector of rights")
                    {
                        @Override
                        public Object invoke(Invocation invocation)
                        {
                            return getLegacyDocumentRights();
                        }
                    });
                    allowing(mockedDocument)
                        .getXObjects(with(equal(new LocalDocumentReference("XWiki", "XWikiGlobalRights"))));
                    will(new CustomAction("return a vector of rights")
                    {
                        @Override
                        public Object invoke(Invocation invocation)
                        {
                            return getLegacyGlobalRights();
                        }
                    });
                    allowing(mockedDocument)
                        .getXObjects(new DocumentReference(space.wiki.getName(), "XWiki", "XWikiRights"));
                    will(new CustomAction("return a vector of rights")
                    {
                        @Override
                        public Object invoke(Invocation invocation)
                        {
                            return getLegacyDocumentRights();
                        }
                    });
                    allowing(mockedDocument)
                        .getXObjects(new DocumentReference(space.wiki.getName(), "XWiki", "XWikiGlobalRights"));
                    will(new CustomAction("return a vector of rights")
                    {
                        @Override
                        public Object invoke(Invocation invocation)
                        {
                            return getLegacyGlobalRights();
                        }
                    });
                    allowing(mockedDocument).getCreatorReference();
                    will(returnValue(documentReferenceResolver.resolve(TestDocument.this.creator, documentReference)));
                    allowing(mockedDocument).getDocumentReference();
                    will(returnValue(documentReference));
                    allowing(mockedDocument).isNew();
                    will(returnValue(isNew));
                    allowing(mockedDocument).getXObjects();
                    will(returnValue(Collections.emptyMap()));
                    allowing(mockedDocument)
                        .getXObjects(XWikiGroupsDocumentInitializer.XWIKI_GROUPS_DOCUMENT_REFERENCE);
                    will(returnValue(Collections.emptyList()));
                }
            });

            if (legacymock) {
                mockery.checking(new Expectations()
                {
                    {
                        allowing(mockedDocument).getObjects("XWiki.XWikiGlobalRights");
                        will(new CustomAction("return a vector of global rights")
                        {
                            @Override
                            public Object invoke(Invocation invocation)
                            {
                                return getLegacyGlobalRights();
                            }
                        });
                        allowing(mockedDocument).getObjects("XWiki.XWikiRights");
                        will(new CustomAction("return a vector of document rights")
                        {
                            @Override
                            public Object invoke(Invocation invocation)
                            {
                                return getLegacyDocumentRights();
                            }
                        });
                        allowing(mockedDocument).getWikiName();
                        will(returnValue(getSpace().getWiki().getName()));
                        allowing(mockedDocument).getDatabase();
                        will(returnValue(getSpace().getWiki().getName()));
                        allowing(mockedDocument).getSpace();
                        will(returnValue(getSpace().getName()));
                    }
                });
            }
        }

        public XWikiDocument getDocument()
        {
            return mockedDocument;
        }

        public Vector<BaseObject> getLegacyGlobalRights()
        {
            if ("XWikiPreferences".equals(getName())) {
                return getSpace().getWiki().getLegacyRightObjects();
            } else if ("WebPreferences".equals(getName())) {
                return getSpace().getLegacyRightObjects();
            }
            return null;
        }

        public Vector<BaseObject> getLegacyDocumentRights()
        {
            return getLegacyRightObjects();
        }

        public Map<DocumentReference, List<BaseObject>> getLegacyXObjects()
        {
            Map<DocumentReference, List<BaseObject>> objects = new HashMap<DocumentReference, List<BaseObject>>();

            Vector<BaseObject> globalRights = getLegacyGlobalRights();
            if (globalRights != null) {
                objects.put(new DocumentReference(this.space.wiki.getName(), "XWiki", "XWikiGlobalRights"),
                    globalRights);
            }
            Vector<BaseObject> rights = getLegacyDocumentRights();
            if (globalRights != null) {
                objects.put(new DocumentReference(this.space.wiki.getName(), "XWiki", "XWikiRights"), rights);
            }

            return objects;
        }

        @Override
        EntityType getType()
        {
            return EntityType.DOCUMENT;
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public String getPrettyName()
        {
            return (this.alt != null) ? this.alt : this.name;
        }

        TestSpace getSpace()
        {
            return this.space;
        }
    }

    public static int objectNumber = 0;

    private abstract class TestAcl implements HasAcl
    {
        private final Map<String, Set<String>> allowUser = new HashMap<String, Set<String>>();

        private final Map<String, Set<String>> denyUser = new HashMap<String, Set<String>>();

        private final Map<String, Set<String>> allowGroup = new HashMap<String, Set<String>>();

        private final Map<String, Set<String>> denyGroup = new HashMap<String, Set<String>>();

        private List<BaseObject> mockedObjects = null;

        private void addType(Map<String, Set<String>> map, String key, String type)
        {
            Set<String> types = map.get(key);

            if (types == null) {
                types = new HashSet<String>();
                map.put(key, types);
            }

            types.add(type);
        }

        private void addRightType(Map<String, Set<String>> map, String key, String type)
        {
            if (type == null) {
                for (Right right : Right.getEnabledRights(getType())) {
                    addType(map, key, right.toString());
                }
            } else {
                addType(map, key, type);
            }
        }

        @Override
        public void addAllowUser(String name, String type)
        {
            addRightType(this.allowUser, name, type);
        }

        @Override
        public void addDenyUser(String name, String type)
        {
            addRightType(this.denyUser, name, type);
        }

        @Override
        public void addAllowGroup(String name, String type)
        {
            addRightType(this.allowGroup, name, type);
        }

        @Override
        public void addDenyGroup(String name, String type)
        {
            addRightType(this.denyGroup, name, type);
        }

        abstract EntityType getType();

        abstract String getName();

        abstract String getPrettyName();

        public Vector<BaseObject> getLegacyRightObjects()
        {
            if (this.mockedObjects == null) {
                this.mockedObjects = new ArrayList<BaseObject>();
                for (Map.Entry<String, Set<String>> entry : this.allowUser.entrySet()) {
                    for (String type : entry.getValue()) {
                        this.mockedObjects.add(mockRightBaseObject(entry.getKey(), type, true, true));
                    }
                }
                for (Map.Entry<String, Set<String>> entry : this.denyUser.entrySet()) {
                    for (String type : entry.getValue()) {
                        this.mockedObjects.add(mockRightBaseObject(entry.getKey(), type, true, false));
                    }
                }

                for (Map.Entry<String, Set<String>> entry : this.allowGroup.entrySet()) {
                    for (String type : entry.getValue()) {
                        this.mockedObjects.add(mockRightBaseObject(entry.getKey(), type, false, true));
                    }
                }

                for (Map.Entry<String, Set<String>> entry : this.denyGroup.entrySet()) {
                    for (String type : entry.getValue()) {
                        this.mockedObjects.add(mockRightBaseObject(entry.getKey(), type, false, false));
                    }
                }
            }

            return this.mockedObjects.size() == 0 ? null : new Vector<BaseObject>(this.mockedObjects);
        }

        private BaseObject mockRightBaseObject(final String name, final String type, final boolean isUser,
            final boolean allow)
        {
            objectNumber++;

            final BaseObject rightBaseObjects =
                mockery.mock(BaseObject.class, getName() + objectNumber + ' ' + name + ' ' + type + ' ' + allow);

            final String usersString = isUser ? name : "";
            final String groupsString = isUser ? "" : name;
            final String levelsString = type;

            mockery.checking(new Expectations()
            {
                {
                    allowing(rightBaseObjects).getIntValue("allow");
                    will(returnValue(allow ? 1 : 0));

                    allowing(rightBaseObjects).getStringValue("users");
                    will(returnValue(usersString));
                    allowing(rightBaseObjects).getStringValue("groups");
                    will(returnValue(groupsString));
                    allowing(rightBaseObjects).getStringValue("levels");
                    will(returnValue(levelsString));
                }
            });

            return rightBaseObjects;
        }
    }

    // Modifications

    public void addUser(String userName, String wikiName)
    {
        TestWiki wiki = this.wikis.get(wikiName);

        wiki.addUser(userName);
    }

    public void deleteUser(String userName, String wikiName)
    {
        TestWiki wiki = this.wikis.get(wikiName);

        wiki.deleteUser(userName);
    }

    public void addGroup(String groupName, String wikiName)
    {
        TestWiki wiki = wikis.get(wikiName);

        wiki.addGroup(groupName);
    }

    public void deleteGroup(String groupName, String wikiName)
    {
        TestWiki wiki = wikis.get(wikiName);

        wiki.deleteGroup(groupName);
    }

    public TestDocument addDocument(DocumentReference documentReference, String creator, boolean isNew)
    {
        TestWiki wiki = wikis.get(documentReference.getWikiReference().getName());

        return wiki.addDocument(documentReference, creator, isNew);
    }

    public void deleteDocument(DocumentReference documentReference)
    {
        TestWiki wiki = wikis.get(documentReference.getWikiReference().getName());

        wiki.deleteDocument(documentReference);
    }

    public void notifyDocumentModified(DocumentReference documentReference)
    {
        TestWiki wiki = wikis.get(documentReference.getWikiReference().getName());

        XWikiDocument document = wiki.getDocument(documentReference);

        // Send event
        observationManager.notify(new DocumentUpdatedEvent(documentReference), document, context);
    }
}
