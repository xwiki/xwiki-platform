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

import org.junit.Assert;
import org.junit.Before;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.testwikibuilding.LegacyTestWiki;
import org.xwiki.security.internal.XWikiConstants;

import static org.xwiki.security.authorization.Right.ADMIN;
import static org.xwiki.security.authorization.Right.ILLEGAL;
import static org.xwiki.security.authorization.Right.LOGIN;
import static org.xwiki.security.authorization.Right.PROGRAM;
import static org.xwiki.security.authorization.Right.REGISTER;
import static org.xwiki.security.authorization.Right.values;

/**
 * Abstract class that should be inherited when writing tests case based on XML based mocked wikis for testing
 * authorization.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class AbstractAuthorizationTestCase extends AbstractWikiTestCase
{
    /** SuperAdmin user. */
    protected static final DocumentReference SUPERADMIN = new DocumentReference("anyWiki","anySpace","SuperAdmin");

    /** VIEW, EDIT, COMMENT, DELETE, REGISTER, LOGIN, ADMIN, PROGRAM. */
    protected static final RightSet ALL_RIGHTS = new RightSet();

    /** VIEW, EDIT, COMMENT, DELETE, REGISTER, LOGIN, ADMIN. */
    protected static final RightSet ALL_RIGHTS_BUT_PROGRAMING = new RightSet();

    /** VIEW, EDIT, COMMENT, DELETE, REGISTER, LOGIN. */
    protected static final RightSet ALL_RIGHTS_BUT_ADMIN = new RightSet();

    /** VIEW, EDIT, COMMENT, DELETE, REGISTER. */
    protected static final RightSet ALL_RIGHTS_BUT_LOGIN = new RightSet();

    /** VIEW, EDIT, COMMENT, DELETE, ADMIN. */
    protected static final RightSet ALL_SPACE_RIGHTS = new RightSet();

    /** VIEW, EDIT, COMMENT, DELETE. */
    protected static final RightSet ALL_DOCUMENT_RIGHTS = new RightSet();

    static {
        for(Right right : Right.values()) {
            if (right != ILLEGAL) {
                ALL_RIGHTS.add(right);
                if (right != PROGRAM) {
                    ALL_RIGHTS_BUT_PROGRAMING.add(right);
                    if (right != ADMIN) {
                        ALL_RIGHTS_BUT_ADMIN.add(right);
                        if (right != LOGIN) {
                            ALL_RIGHTS_BUT_LOGIN.add(right);
                            if (right != REGISTER) {
                                ALL_DOCUMENT_RIGHTS.add(right);
                            }
                        }
                    }
                    if (right != LOGIN && right != REGISTER) {
                        ALL_SPACE_RIGHTS.add(right);
                    }
                }
            }
        }
    }

    /** Component under test. */
    protected AuthorizationManager authorizationManager;

    /** Current wiki mock. */
    protected LegacyTestWiki wikisMock;


    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.authorizationManager = getComponentManager().getInstance(AuthorizationManager.class);
    }

    /**
     * Initialize the test by loading wikis form XML.
     * @param filename the filename without extension
     * @return the mock of wikis, also available using {@link #wikisMock}
     * @throws Exception on error.
     */
    protected LegacyTestWiki initialiseWikiMock(String filename) throws Exception
    {
        wikisMock = newTestWiki(filename + ".xml");
        return wikisMock;
    }

    /**
     * @param name user name.
     * @return a reference to a user in the main wiki.
     */
    protected DocumentReference getXUser(String name)
    {
        return getUser(name, wikisMock.getMainWikiName());
    }

    /**
     * @param name user name.
     * @param wiki subwiki name.
     * @return a reference to a user in a sub wiki.
     */
    protected DocumentReference getUser(String name, String wiki)
    {
        return new DocumentReference(wiki, XWikiConstants.XWIKI_SPACE, name);
    }

    /**
     * @return a reference to the main wiki.
     */
    protected WikiReference getXWiki()
    {
        return getWiki(wikisMock.getMainWikiName());
    }

    /**
     * @param wiki subwiki name.
     * @return a reference to a subwiki.
     */
    protected WikiReference getWiki(String wiki)
    {
        return new WikiReference(wiki);
    }

    /**
     * @param space space name.
     * @return a reference to a space in the main wiki.
     */
    protected SpaceReference getXSpace(String space)
    {
        return getSpace(space, wikisMock.getMainWikiName());
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
        return getDoc(name, space, wikisMock.getMainWikiName());
    }

    /**
     * @param name document name.
     * @param space space name.
     * @param wiki subwiki name.
     * @return a reference to a document in a space of a subwiki.
     */
    protected DocumentReference getDoc(String name, String space, String wiki)
    {
        return new DocumentReference(wiki, space, name);
    }

    /**
     * @param user user reference.
     * @return a pretty name for the user based on entities "alt" attributes or their names.
     */
    private String getUserReadableName(DocumentReference user) {
        if (user == null) {
            return "Public";
        }

        String result = wikisMock.getDocumentPrettyName(user);
        WikiReference wiki = user.getWikiReference();

        String name = wikisMock.getWikiPrettyName(wiki);
        if (!name.startsWith("any")) {
            result += " from " + name;
        }

        return result;
    }

    /**
     * @param entity user reference.
     * @return a pretty name for the user based on entities "alt" attributes or their names.
     */
    private String getEntityReadableName(EntityReference entity) {
        if (entity == null) {
            return "Main Wiki";
        }

        String result = "";

        if (entity.getType() == EntityType.DOCUMENT) {
            result = wikisMock.getDocumentPrettyName(new DocumentReference(entity));
            entity = entity.getParent();
        }

        if (entity.getType() == EntityType.SPACE) {
            String name = wikisMock.getSpacePrettyName(new SpaceReference(entity));
            if (result == "" || !name.startsWith("any")) {
                if (result != "") {
                    result += " in ";
                }
                result += name;
            }
            entity = entity.getParent();
        }

        if (entity.getType() == EntityType.WIKI) {
            String name = wikisMock.getWikiPrettyName(new WikiReference(entity));
            if (result == "" || !name.startsWith("any")) {
                if (result != "") {
                    result += " from ";
                }
                result += name;
            }
        }

        return result;
    }

    /**
     * Assert an allowed access for a given right for a given user on a given entity.
     * @param message the assert message
     * @param right the right to check for allowance
     * @param userReference the reference of the user to test.
     * @param entityReference the reference of the entity to test.
     * @throws Exception on error.
     */
    protected void assertAccessTrue(String message, Right right, DocumentReference userReference,
        EntityReference entityReference) throws Exception
    {
        Assert.assertTrue(message, this.authorizationManager.hasAccess(right, userReference, entityReference));
    }

    /**
     * Assert a denied access for a given right for a given user on a given entity.
     * @param message the assert message
     * @param right the right to check for denial
     * @param userReference the reference of the user to test.
     * @param entityReference the reference of the entity to test.
     * @throws Exception on error.
     */
    protected void assertAccessFalse(String message, Right right, DocumentReference userReference,
        EntityReference entityReference) throws Exception
    {
        Assert.assertFalse(message, this.authorizationManager.hasAccess(right, userReference, entityReference));
    }

    /**
     * Check all rights for access by given user on a given entity.
     * @param allowedRights the set of rights that should be allowed.
     * @param userReference the reference of the user to test.
     * @param entityReference the reference of the entity to test.
     * @throws Exception on error.
     */
    protected void assertAccess(RightSet allowedRights, DocumentReference userReference,
        EntityReference entityReference) throws Exception
    {
        for (Right right : values()) {
            if (allowedRights != null && allowedRights.contains(right)) {
                if(!this.authorizationManager.hasAccess(right, userReference, entityReference)) {
                    Assert.fail(String.format("[%s] should have [%s] right on [%s].",
                        getUserReadableName(userReference), right, getEntityReadableName(entityReference)));
                }
            } else {
                if(this.authorizationManager.hasAccess(right, userReference, entityReference)) {
                    Assert.fail(String.format("[%s] should not have [%s] right on [%s].",
                        getUserReadableName(userReference), right, getEntityReadableName(entityReference)));
                }
            }
        }
    }
}
