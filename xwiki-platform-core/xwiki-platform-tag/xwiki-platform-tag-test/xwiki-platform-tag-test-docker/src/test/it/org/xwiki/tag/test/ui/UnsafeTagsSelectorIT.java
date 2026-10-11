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

package org.xwiki.tag.test.ui;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of the tag queries when the {@code unsafe} right check strategy is configured, since the counting of the tags
 * is then fully delegated to the database.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@UITest(properties = {
    // The strategy is switched from within the tests by {{groovy}} pages, which the programming rights checker would
    // otherwise block.
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:Test\\.Execute\\..*"
})
class UnsafeTagsSelectorIT
{
    private static final String SPACE = "UnsafeTagsSelectorIT";

    /**
     * The system properties prefixed with {@code xconf.} are merged into {@code xwiki.properties}, and
     * {@code DefaultTagsSelector} reads the strategy on each call, so it can be switched without restarting the
     * instance.
     */
    private static final String STRATEGY_PROPERTY = "xconf.xwikiproperties.tag.rightCheckStrategy.hint";

    private static final String UNSAFE_HINT = "unsafe";

    @BeforeAll
    void setUp(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        // setProperty returns the previously set value, which must be empty since the default strategy is configured
        // by the absence of the property.
        assertEquals("", setup.executeWikiPlain("""
            {{groovy}}
            System.setProperty("%s", "%s")
            {{/groovy}}
            """.formatted(STRATEGY_PROPERTY, UNSAFE_HINT), Syntax.XWIKI_2_1).trim());

        createTaggedPage(setup, "Page1", "AlphaTag|BetaTag");
        createTaggedPage(setup, "Page2", "alphatag");
        createTaggedPage(setup, "Page3", "BetaTag");
    }

    @AfterAll
    void tearDown(TestUtils setup) throws Exception
    {
        // Restore the default strategy, the other tests of the suite share this instance. clearProperty returns the
        // value that was set, which also checks that the property was still the one these tests installed.
        assertEquals(UNSAFE_HINT, setup.executeWikiPlain("""
            {{groovy}}
            System.clearProperty("%s")
            {{/groovy}}
            """.formatted(STRATEGY_PROPERTY), Syntax.XWIKI_2_1).trim());
    }

    /**
     * The counting is performed by the database, which groups the tags by their exact case, so the case variants of a
     * same tag still have to be merged afterwards.
     */
    @Test
    @Order(1)
    void getTagCountMergesTheCaseVariantsOfATag(TestUtils setup) throws Exception
    {
        assertEquals("alpha=[2] beta=[2] missing=[]", setup.executeWikiPlain(
            "{{velocity}}#set ($counts = $xwiki.tag.getTagCount())"
            + "alpha=[$!counts.get('AlphaTag')] beta=[$!counts.get('BetaTag')] "
            + "missing=[$!counts.get('NoSuchTag')]{{/velocity}}", Syntax.XWIKI_2_1));
    }

    /**
     * The counts must also be correct when the query is restricted to a space, since the space restriction is injected
     * in the middle of the tag query.
     */
    @Test
    @Order(2)
    void getTagCountForSpace(TestUtils setup) throws Exception
    {
        assertEquals("alpha=[2] beta=[2]", setup.executeWikiPlain(
            "{{velocity}}#set ($counts = $xwiki.tag.getTagCount('" + SPACE + "'))"
            + "alpha=[$!counts.get('AlphaTag')] beta=[$!counts.get('BetaTag')]{{/velocity}}", Syntax.XWIKI_2_1));
    }

    /**
     * Unlike the counts, the tags themselves are returned with each of their case variants.
     */
    @Test
    @Order(3)
    void getAllTagsReturnsEachCaseVariant(TestUtils setup) throws Exception
    {
        assertEquals("alpha=[true] alphaVariant=[true] beta=[true] missing=[false]", setup.executeWikiPlain(
            "{{velocity}}#set ($allTags = $xwiki.tag.getAllTags())"
            + "alpha=[$allTags.contains('AlphaTag')] alphaVariant=[$allTags.contains('alphatag')] "
            + "beta=[$allTags.contains('BetaTag')] missing=[$allTags.contains('NoSuchTag')]{{/velocity}}",
            Syntax.XWIKI_2_1));
    }

    private void createTaggedPage(TestUtils setup, String pageName, String tags) throws Exception
    {
        DocumentReference reference = new DocumentReference("xwiki", List.of(SPACE), pageName);
        setup.rest().delete(reference);
        setup.rest().addObject(reference, "XWiki.TagClass", "tags", tags);
    }
}
