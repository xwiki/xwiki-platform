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

package org.xwiki.security.authorization;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.testwikis.TestDefinition;
import org.xwiki.security.authorization.testwikis.TestDefinitionParser;
import org.xwiki.security.authorization.testwikis.TestDocument;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.TestSpace;
import org.xwiki.security.authorization.testwikis.TestWiki;
import org.xwiki.security.authorization.testwikis.internal.parser.DefaultTestDefinitionParser;
import org.xwiki.security.authorization.testwikis.internal.parser.XWikiConstants;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.xwiki.security.authorization.Right.ADMIN;
import static org.xwiki.security.authorization.Right.CREATE_WIKI;
import static org.xwiki.security.authorization.Right.CREATOR;
import static org.xwiki.security.authorization.Right.DELETE;
import static org.xwiki.security.authorization.Right.ILLEGAL;
import static org.xwiki.security.authorization.Right.LOGIN;
import static org.xwiki.security.authorization.Right.PROGRAM;
import static org.xwiki.security.authorization.Right.REGISTER;
import static org.xwiki.security.authorization.Right.SCRIPT;


/**
 * Abstract class that should be inherited when writing tests case based on XML based mocked wikis for testing
 * authorization.
 *
 * @version $Id$
 * @since 5.0M2
 */
@ComponentList({DefaultStringEntityReferenceResolver.class, DefaultStringEntityReferenceSerializer.class,
    DefaultEntityReferenceProvider.class, DefaultModelConfiguration.class})
public abstract class AbstractAuthorizationTestCase
{
    /** SuperAdmin user. */
    protected static final DocumentReference SUPERADMIN = new DocumentReference("anyWiki", "anySpace", "SuperAdmin");

    /** VIEW, EDIT, COMMENT, DELETE, REGISTER, LOGIN, SCRIPT, ADMIN, PROGRAM, CREATE_WIKI. */
    protected static final RightSet ALL_RIGHTS = new RightSet();

    /** VIEW, EDIT, COMMENT, DELETE, REGISTER, LOGIN, SCRIPT, ADMIN, CREATE_WIKI. */
    protected static final RightSet ALL_RIGHTS_EXCEPT_PROGRAMING = new RightSet();

    /** VIEW, EDIT, COMMENT, DELETE, REGISTER, LOGIN, SCRIPT, ADMIN. */
    protected static final RightSet ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI = new RightSet();

    /** VIEW, EDIT, COMMENT, DELETE, REGISTER, LOGIN, SCRIPT. */
    protected static final RightSet ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI = new RightSet();

    /** VIEW, EDIT, COMMENT, DELETE, SCRIPT, ADMIN. */
    protected static final RightSet ALL_SPACE_RIGHTS = new RightSet();

    /** VIEW, EDIT, COMMENT, REGISTER, LOGIN. */
    protected static final RightSet DEFAULT_DOCUMENT_RIGHTS = new RightSet();

    /** VIEW, EDIT, COMMENT, DELETE, SCRIPT. */
    protected static final RightSet ALL_DOCUMENT_RIGHTS = new RightSet();

    /**
     * Reset and fill the static variables.
     */
    @BeforeAll
    public static void initialize()
    {
        ALL_RIGHTS.clear();
        ALL_RIGHTS_EXCEPT_PROGRAMING.clear();
        ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI.clear();
        ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI.clear();
        ALL_SPACE_RIGHTS.clear();
        DEFAULT_DOCUMENT_RIGHTS.clear();
        ALL_DOCUMENT_RIGHTS.clear();
        for (Right right : Right.values()) {
            if (right != ILLEGAL && right != CREATOR) {
                ALL_RIGHTS.add(right);
                if (right != PROGRAM) {
                    ALL_RIGHTS_EXCEPT_PROGRAMING.add(right);
                    if (right != CREATE_WIKI) {
                        ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI.add(right);
                    }
                    if (right != ADMIN && right != CREATE_WIKI) {
                        ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI.add(right);
                        if (right != LOGIN && right != REGISTER) {
                            ALL_DOCUMENT_RIGHTS.add(right);
                        }
                        if (right != DELETE && right != SCRIPT) {
                            DEFAULT_DOCUMENT_RIGHTS.add(right);
                        }
                    }
                    if (right != LOGIN && right != REGISTER && right != CREATE_WIKI) {
                        ALL_SPACE_RIGHTS.add(right);
                    }
                }
            }
        }
    }

    @InjectComponentManager
    public MockitoComponentManager componentManager;

    /** Current wiki mock. */
    protected TestDefinition testDefinition;

    /**
     * Initialize the test by loading wikis form XML.
     * @param filename the filename without extension
     * @return the test wikis, also available using {@link #testDefinition}
     * @throws Exception on error.
     */
    protected TestDefinition initialiseWikiMock(String filename) throws Exception
    {
        TestDefinitionParser parser = new DefaultTestDefinitionParser();

        EntityReferenceResolver<String> resolver =
            componentManager.getInstance(EntityReferenceResolver.TYPE_STRING);
        EntityReferenceSerializer<String> serializer =
            componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);

        testDefinition = parser.parse("testwikis" + File.separator + filename + ".xml", resolver, serializer);
        return testDefinition;
    }

    /**
     * @param name user name.
     * @return a reference to a user in the main wiki.
     */
    protected DocumentReference getXUser(String name)
    {
        return new DocumentReference(name, getXSpace(XWikiConstants.XWIKI_SPACE));
    }

    /**
     * @param name user name.
     * @param wiki subwiki name.
     * @return a reference to a user in a sub wiki.
     */
    protected DocumentReference getUser(String name, String wiki)
    {
        return new DocumentReference(name,
            new SpaceReference(XWikiConstants.XWIKI_SPACE, testDefinition.getWiki(wiki).getWikiReference()));
    }

    /**
     * @return a reference to the main wiki.
     */
    protected WikiReference getXWiki()
    {
        return testDefinition.getMainWiki().getWikiReference();
    }

    /**
     * @param name subwiki name.
     * @return a reference to a subwiki.
     */
    protected WikiReference getWiki(String name)
    {
        return new WikiReference(name);
    }

    /**
     * @param space space name.
     * @return a reference to a space in the main wiki.
     */
    protected SpaceReference getXSpace(String space)
    {
        return new SpaceReference(space, testDefinition.getMainWiki().getWikiReference());
    }

    /**
     * @param space space name.
     * @param wiki subwiki name.
     * @return a reference to a space in a subwiki.
     */
    protected SpaceReference getSpace(String space, String wiki)
    {
        return new SpaceReference(space, new WikiReference(wiki));
    }

    /**
     * @param name document name.
     * @param space space name.
     * @return a reference to a document in a space of the main wiki.
     */
    protected DocumentReference getXDoc(String name, String space)
    {
        return new DocumentReference(name, new SpaceReference(space, testDefinition.getMainWiki().getWikiReference()));
    }

    /**
     * @param name document name.
     * @param space space name.
     * @param wiki subwiki name.
     * @return a reference to a document in a space of a subwiki.
     */
    protected DocumentReference getDoc(String name, String space, String wiki)
    {
        return new DocumentReference(name, new SpaceReference(space, new WikiReference(wiki)));
    }

    /**
     * @param user user reference.
     * @return a pretty name for the user based on entities "alt" attributes or their names.
     */
    protected String getUserReadableName(DocumentReference user) {
        if (user == null) {
            return "Public";
        }

        TestEntity userEntity = testDefinition.searchEntity(user);
        String result = (userEntity != null && userEntity instanceof TestDocument)
            ? ((TestDocument) userEntity).getDescription() : null;
        result = (result != null) ? result : user.getName();

        TestWiki wiki = testDefinition.getWiki(user.getWikiReference());
        String name = (wiki != null) ? wiki.getDescription() : null;
        name = (name != null) ? name : user.getWikiReference().getName();
        if (!name.startsWith("any")) {
            result += " from " + name;
        }

        return result;
    }

    /**
     * @param entity user reference.
     * @return a pretty name for the user based on entities "alt" attributes or their names.
     */
    protected String getEntityReadableName(EntityReference entity) {
        if (entity == null) {
            return "Main Wiki";
        }

        StringBuilder result = null;

        if (entity.getType() == EntityType.DOCUMENT) {
            TestEntity docEntity = testDefinition.searchEntity(entity);
            String name = (docEntity != null && docEntity instanceof TestDocument)
                ? ((TestDocument)docEntity).getDescription() : null;
            name = (name != null) ? name : entity.getName();
            result = new StringBuilder(name);
            entity = entity.getParent();
        }

        if (entity.getType() == EntityType.SPACE) {
            TestEntity spaceEntity = testDefinition.searchEntity(entity);
            String name = (spaceEntity != null && spaceEntity instanceof TestSpace)
                ? ((TestSpace) spaceEntity).getDescription() : null;
            name = (name != null) ? name : entity.getName();
            if (result == null || !name.startsWith("any")) {
                if (result != null) {
                    result.append(" in ");
                    result.append(name);
                } else {
                    result = new StringBuilder(name);
                }
            }
            entity = entity.getParent();
        }

        if (entity.getType() == EntityType.WIKI) {
            TestEntity wikiEntity = testDefinition.getWiki(new WikiReference(entity));
            String name = (wikiEntity != null) ? ((TestWiki) wikiEntity).getDescription() : null;
            name = (name != null) ? name : entity.getName();
            if (result == null || !name.startsWith("any")) {
                if (result != null) {
                    result.append(" from ");
                    result.append(name);
                } else {
                    result = new StringBuilder(name);
                }
            }
        }

        return result.toString();
    }
}
