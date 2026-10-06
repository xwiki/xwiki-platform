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
package org.xwiki.records.internal.macro;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.livedata.internal.LiveDataRenderer;
import org.xwiki.livedata.internal.LiveDataRendererParameters;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.records.macro.RecordsMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.listener.ListenerRegistry;
import org.xwiki.rendering.internal.parser.plain.PlainTextBlockParser;
import org.xwiki.rendering.internal.parser.plain.PlainTextStreamParser;
import org.xwiki.rendering.internal.plain.Plain10SyntaxProvider;
import org.xwiki.rendering.internal.renderer.xwiki20.reference.XWiki20ResourceReferenceTypeSerializer;
import org.xwiki.rendering.internal.renderer.xwiki21.XWikiSyntaxBlockRenderer;
import org.xwiki.rendering.internal.renderer.xwiki21.XWikiSyntaxRenderer;
import org.xwiki.rendering.internal.renderer.xwiki21.XWikiSyntaxRendererFactory;
import org.xwiki.rendering.internal.renderer.xwiki21.reference.InterWikiReferenceTypeSerializer;
import org.xwiki.rendering.internal.renderer.xwiki21.reference.XWiki21ResourceReferenceTypeSerializer;
import org.xwiki.rendering.internal.renderer.xwiki21.reference.XWikiSyntaxImageReferenceSerializer;
import org.xwiki.rendering.internal.renderer.xwiki21.reference.XWikiSyntaxLinkReferenceSerializer;
import org.xwiki.rendering.internal.syntax.DefaultSyntaxRegistry;
import org.xwiki.rendering.internal.xwiki21.XWiki21SyntaxProvider;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.util.IdGenerator;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RecordsMacro}.
 * <p>
 * The macro is a translation layer onto Live Data, so these tests are about the translation: they assert what the
 * macro hands to {@link LiveDataRenderer} rather than what the rendered table looks like, which is Live Data's own
 * concern and is covered by its own tests.
 *
 * @version $Id$
 */
// The real plain text parser and XWiki syntax renderer, since what is under test is the escaping they perform.
@ComponentList({
    PlainTextBlockParser.class, PlainTextStreamParser.class,
    XWikiSyntaxBlockRenderer.class, XWikiSyntaxRenderer.class, XWikiSyntaxRendererFactory.class,
    XWikiSyntaxLinkReferenceSerializer.class, XWikiSyntaxImageReferenceSerializer.class,
    XWiki21ResourceReferenceTypeSerializer.class, InterWikiReferenceTypeSerializer.class,
    XWiki20ResourceReferenceTypeSerializer.class, ListenerRegistry.class,
    DefaultSyntaxRegistry.class, Plain10SyntaxProvider.class, XWiki21SyntaxProvider.class
})
@ComponentTest
class RecordsMacroTest
{
    private static final DocumentReference DATA_TYPE =
        new DocumentReference("xwiki", List.of("Clients", "Code"), "ProjectClass");

    private static final String SERIALIZED_DATA_TYPE = "Clients.Code.ProjectClass";

    @InjectMockComponents
    private RecordsMacro macro;

    @MockComponent
    private LiveDataRenderer liveDataRenderer;

    @MockComponent
    private LiveDataSourceManager liveDataSourceManager;

    @Mock
    private LiveDataSource liveDataSource;

    @Mock
    private LiveDataPropertyDescriptorStore propertyStore;

    @MockComponent
    @Named("liveTable")
    private Provider<LiveDataConfiguration> sourceDefaults;

    @MockComponent
    private ContextualLocalizationManager localization;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    // Needed by the syntax registry that the XWiki syntax renderer looks the syntaxes up in.
    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    private final Block renderedBlock = new GroupBlock(List.of());

    private MacroTransformationContext context;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.context = new MacroTransformationContext(new TransformationContext());
        // The document's generator: shared by every id the page builds, headings included.
        this.context.setXDOM(new XDOM(List.of(), new IdGenerator()));
        when(this.entityReferenceSerializer.serialize(DATA_TYPE)).thenReturn(SERIALIZED_DATA_TYPE);
        when(this.documentAccessBridge.exists(DATA_TYPE)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, DATA_TYPE)).thenReturn(true);
        // Echoes the key and the arguments, which is what the tests assert on.
        when(this.localization.getTranslationPlain(any(String.class), any(Object[].class)))
            .thenAnswer(invocation -> invocation.getArgument(0) + Arrays.toString(
                Arrays.copyOfRange(invocation.getArguments(), 1, invocation.getArguments().length)));
        when(this.liveDataSourceManager.get(any(Source.class))).thenReturn(Optional.of(this.liveDataSource));
        when(this.liveDataSource.getProperties()).thenReturn(this.propertyStore);
        // What the liveTable property store reports: the page metadata, the data type's fields, and the Live Data
        // pseudo-columns, all in one list.
        when(this.propertyStore.get()).thenReturn(List.of(
            descriptor("doc.title"), descriptor("doc.location"),
            descriptor("_actions"), descriptor("_avatar"),
            descriptor("first_name"), descriptor("last_name"), descriptor("email")));
        when(this.liveDataRenderer.execute(any(LiveDataRendererParameters.class), eq((String) null), anyBoolean()))
            .thenReturn(this.renderedBlock);
        // The defaults of the liveTable source for the types the tests use, as its configuration declares them.
        LiveDataMeta meta = new LiveDataMeta();
        meta.setPropertyTypes(List.of(
            propertyType("String", true, "text"),
            propertyType("Number", true, "number"),
            propertyType("Boolean", true, "boolean"),
            propertyType("Password", false, null)));
        LiveDataConfiguration defaults = new LiveDataConfiguration();
        defaults.setMeta(meta);
        when(this.sourceDefaults.get()).thenReturn(defaults);
    }

    @Test
    void executeReturnsWhatLiveDataRendered() throws Exception
    {
        List<Block> blocks = this.macro.execute(newParameters(), null, this.context);

        assertEquals(1, blocks.size());
        assertSame(this.renderedBlock, blocks.get(0));
    }

    @Test
    void executeMapsEveryParameterOntoLiveData() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        parameters.setProperties("doc.title,first_name,email");
        parameters.setFilters("first_name=Ann&email=ann%40acme.com");
        parameters.setSort("last_name:desc");
        parameters.setLayouts("cards");
        parameters.setDescription("Acme projects, most recent first");
        parameters.setId("projects");

        LiveDataRendererParameters liveDataParameters = execute(parameters);

        assertEquals("liveTable", liveDataParameters.getSource());
        assertEquals("doc.title,first_name,email", liveDataParameters.getProperties());
        assertEquals("first_name=Ann&email=ann%40acme.com", liveDataParameters.getFilters());
        assertEquals("last_name:desc", liveDataParameters.getSort());
        assertEquals("cards", liveDataParameters.getLayouts());
        assertEquals("Acme projects, most recent first", liveDataParameters.getDescription());
        assertEquals("projects", liveDataParameters.getId());
    }

    @Test
    void executePassesTheDataTypeAsASourceParameter() throws Exception
    {
        LiveDataRendererParameters liveDataParameters = execute(newParameters());

        assertEquals("className=Clients.Code.ProjectClass&translationPrefix=platform.index.",
            liveDataParameters.getSourceParameters());
    }

    @Test
    void executeUrlEncodesTheSourceParameters() throws Exception
    {
        // A class reference is unlikely to hold these, but the receiving end URL-decodes every source parameter, so
        // a value that is not encoded here would be silently truncated at the first separator.
        when(this.entityReferenceSerializer.serialize(DATA_TYPE)).thenReturn("Space.A&B=C");

        LiveDataRendererParameters liveDataParameters = execute(newParameters());

        assertEquals("className=Space.A%26B%3DC&translationPrefix=platform.index.",
            liveDataParameters.getSourceParameters());
    }

    @Test
    void executeDisplaysTheTitleAndEveryFieldWhenNoColumnIsGiven() throws Exception
    {
        // The liveTable source needs an explicit column list: an absent one yields a table with no columns rather
        // than one with every column, which is the opposite of the parameter's documented default.
        LiveDataRendererParameters liveDataParameters = execute(newParameters());

        assertEquals("doc.title,first_name,last_name,email", liveDataParameters.getProperties());
    }

    @Test
    void executeOpensTheDefaultColumnsWithTheEntryTitle() throws Exception
    {
        // Without it a reader cannot tell one entry from another, nor reach the page an entry lives in.
        assertTrue(execute(newParameters()).getProperties().startsWith("doc.title,"));
    }

    @Test
    void executeLeavesTheOtherMetadataAndThePseudoColumnsOutOfTheDefault() throws Exception
    {
        // The title is the only piece of page metadata the default wants; the rest is the author's to add. The
        // pseudo-columns are affordances rather than data.
        String properties = execute(newParameters()).getProperties();

        assertFalse(properties.contains("doc.location"));
        assertFalse(properties.contains("_actions"));
        assertFalse(properties.contains("_avatar"));
    }

    @Test
    void executeStillIdentifiesTheEntriesOfADataTypeWithoutFields() throws Exception
    {
        // A data type with no field of its own still gets a table a reader can use, rather than no columns at all.
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title"), descriptor("_actions")));

        assertEquals("doc.title", execute(newParameters()).getProperties());
    }

    @Test
    void executeKeepsTheAuthoredColumns() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        parameters.setProperties("doc.title,email");

        assertEquals("doc.title,email", execute(parameters).getProperties());
    }

    @Test
    void executeKeepsTheLiveDataPseudoColumnsTheAuthorNamed() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        parameters.setProperties("doc.title,_actions");
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title")));

        assertEquals("doc.title,_actions", execute(parameters).getProperties());
    }

    @Test
    void executeSkipsAColumnTheDataTypeNoLongerHasAndSaysSo() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        parameters.setProperties("doc.title, budget ,email,,status");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertEquals("doc.title,email", capture().getProperties());
        assertEquals(List.of(
            warning("rendering.macro.records.warning.columnSkipped[budget, Clients.Code.ProjectClass]"),
            warning("rendering.macro.records.warning.columnSkipped[status, Clients.Code.ProjectClass]"),
            this.renderedBlock), blocks);
    }

    @Test
    void executeEscapesTheWikiSyntaxOfAFieldNameInAWarning() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        parameters.setProperties("**x**,email");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        // The name comes from the wiki and the message macro parses its content, so the name must not be read as
        // syntax.
        assertEquals("rendering.macro.records.warning.columnSkipped[~*~*x~*~*, Clients.Code.ProjectClass]",
            ((MacroBlock) blocks.get(0)).getContent());
    }

    @Test
    void executeFailsWhenAMessageCannotBeEscaped() throws Exception
    {
        ParseException cause = new ParseException("parse failure");
        Parser failingParser = mock(Parser.class);
        when(failingParser.parse(any(Reader.class))).thenThrow(cause);
        // The real plain text parser never fails, so this is the only way to reach the failure.
        ReflectionUtils.setFieldValue(this.macro, "plainParser", failingParser);
        when(this.documentAccessBridge.exists(DATA_TYPE)).thenReturn(false);

        MacroExecutionException exception = assertThrows(MacroExecutionException.class,
            () -> this.macro.execute(newParameters(), null, this.context));

        assertSame(cause, exception.getCause());
    }

    @Test
    void executeFallsBackToTheDefaultColumnsWhenNoneOfTheAuthoredOnesIsLeft() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        parameters.setProperties("budget");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertEquals("doc.title,first_name,last_name,email", capture().getProperties());
        assertEquals(2, blocks.size());
    }

    @Test
    void executeDropsASortCriterionOnAFieldTheDataTypeNoLongerHas() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        parameters.setSort("budget:desc,email,last_name:asc,status");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertEquals("email,last_name:asc", capture().getSort());
        assertEquals(List.of(
            warning("rendering.macro.records.warning.sortSkipped[budget, Clients.Code.ProjectClass]"),
            warning("rendering.macro.records.warning.sortSkipped[status, Clients.Code.ProjectClass]"),
            this.renderedBlock), blocks);
    }

    @Test
    void executeLeavesNoSortWhenEveryCriterionIsDropped() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        parameters.setSort("budget:desc");

        assertNull(execute(parameters).getSort());
    }

    @Test
    void executeDropsAFilterOnAFieldTheDataTypeNoLongerHas() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        parameters.setFilters("budget=1%262&email=a&first_name=b&%62udget=3");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertEquals("email=a&first_name=b", capture().getFilters());
        assertEquals(List.of(
            warning("rendering.macro.records.warning.filterSkipped[budget, Clients.Code.ProjectClass]"),
            warning("rendering.macro.records.warning.filterSkipped[budget, Clients.Code.ProjectClass]"),
            this.renderedBlock), blocks);
    }

    @Test
    void executePassesEncodedFreeTextValuesThrough() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        // What the dialog saves for the typed values 100% and C++.
        parameters.setFilters("first_name=100%25&last_name=C%2B%2B");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertEquals("first_name=100%25&last_name=C%2B%2B", capture().getFilters());
        assertEquals(List.of(this.renderedBlock), blocks);
    }

    @Test
    void executeDropsAFilterTheRendererCannotDecodeAndSaysSo() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        // A bare % in a value, then in a field: both make the renderer fail on the whole table.
        parameters.setFilters("first_name=100%&email=a&b%=c");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertEquals("email=a", capture().getFilters());
        assertEquals(List.of(
            warning("rendering.macro.records.warning.filterUnreadable[first_name=100%]"),
            warning("rendering.macro.records.warning.filterUnreadable[b%=c]"),
            this.renderedBlock), blocks);
    }

    @Test
    void executeLeavesNoFilterWhenEveryConstraintIsDropped() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        parameters.setFilters("budget=1");

        assertNull(execute(parameters).getFilters());
    }

    @Test
    void executeDropsAFilterOnAFieldThatCannotBeFilteredOnAndSaysSo() throws Exception
    {
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title"), descriptor("first_name"),
            descriptor("secret", "Password"), descriptor("_actions", null)));
        RecordsMacroParameters parameters = newParameters();
        parameters.setFilters("secret=a&first_name=b&_actions=c");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertEquals("first_name=b", capture().getFilters());
        assertEquals(List.of(
            warning("rendering.macro.records.warning.filterUnsupported[secret]"),
            warning("rendering.macro.records.warning.filterUnsupported[_actions]"),
            this.renderedBlock), blocks);
    }

    @Test
    void executeDropsAFilterValueTheTypeOfItsFieldNoLongerAllowsAndSaysSo() throws Exception
    {
        // price used to be a String and active a String, and the filters were authored then.
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title"), descriptor("first_name"),
            descriptor("price", "Number"), descriptor("active", "Boolean")));
        RecordsMacroParameters parameters = newParameters();
        parameters.setFilters(
            "price=cheap&price=%2012.5%20&price=&active=yes&active=1&active=0&active=&first_name=12");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertEquals("price=%2012.5%20&price=&active=1&active=0&active=&first_name=12", capture().getFilters());
        assertEquals(List.of(
            warning("rendering.macro.records.warning.filterTypeChanged[price]"),
            warning("rendering.macro.records.warning.filterTypeChanged[active]"),
            this.renderedBlock), blocks);
    }

    @Test
    void executeMatchesABooleanFilterAgainstTheValuesOfItsOwnFilter() throws Exception
    {
        // The page metadata booleans are matched with true and false, not with 1 and 0.
        LiveDataPropertyDescriptor hidden = descriptor("doc.hidden", "Boolean");
        hidden.setFilter(new FilterDescriptor("boolean"));
        hidden.getFilter().setParameter("trueValue", true);
        hidden.getFilter().setParameter("falseValue", false);
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title"), hidden));
        RecordsMacroParameters parameters = newParameters();
        parameters.setFilters("doc.hidden=true&doc.hidden=1");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertEquals("doc.hidden=true", capture().getFilters());
        assertEquals(List.of(
            warning("rendering.macro.records.warning.filterTypeChanged[doc.hidden]"),
            this.renderedBlock), blocks);
    }

    @Test
    void executeLetsTheDescriptorOfAFieldOverrideTheDefaultsOfItsType() throws Exception
    {
        LiveDataPropertyDescriptor tags = descriptor("tags");
        // What the source says of a multiple selection list.
        tags.setSortable(false);
        LiveDataPropertyDescriptor code = descriptor("code", "Password");
        code.setFilterable(true);
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title"), tags, code));
        RecordsMacroParameters parameters = newParameters();
        parameters.setFilters("tags=a&code=b");
        parameters.setSort("tags,doc.title:desc");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        LiveDataRendererParameters liveDataParameters = capture();
        assertEquals("tags=a&code=b", liveDataParameters.getFilters());
        assertEquals("doc.title:desc", liveDataParameters.getSort());
        assertEquals(List.of(
            warning("rendering.macro.records.warning.sortUnsupported[tags, doc.title]"),
            this.renderedBlock), blocks);
    }

    @Test
    void executeDropsASortCriterionOnAFieldThatCannotBeSortedOnAndNamesTheNextOne() throws Exception
    {
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title"), descriptor("first_name"),
            descriptor("secret", "Password"), descriptor("hint", "ComputedField")));
        RecordsMacroParameters parameters = newParameters();
        parameters.setSort("secret:desc,first_name:asc,hint");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertEquals("first_name:asc", capture().getSort());
        assertEquals(List.of(
            warning("rendering.macro.records.warning.sortUnsupported[secret, first_name]"),
            warning("rendering.macro.records.warning.sortUnsupported[hint, first_name]"),
            this.renderedBlock), blocks);
    }

    @Test
    void executeNamesTheFirstColumnWhenNoSortCriterionIsLeft() throws Exception
    {
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title"), descriptor("first_name"),
            descriptor("secret", "Password")));
        RecordsMacroParameters parameters = newParameters();
        // Live Data sorts on the first column that is not a pseudo-column when it is given no sort.
        parameters.setProperties("_actions,first_name,secret");
        parameters.setSort("secret");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertNull(capture().getSort());
        assertEquals(List.of(
            warning("rendering.macro.records.warning.sortUnsupported[secret, first_name]"),
            this.renderedBlock), blocks);
    }

    @Test
    void executeSaysTheTableIsNotSortedWhenTheFirstColumnCannotBeSortedOnEither() throws Exception
    {
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title"), descriptor("first_name"),
            descriptor("secret", "Password")));
        RecordsMacroParameters parameters = newParameters();
        parameters.setProperties("secret,first_name");
        parameters.setSort("secret");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertNull(capture().getSort());
        assertEquals(List.of(
            warning("rendering.macro.records.warning.sortUnsupportedUnsorted[secret]"),
            this.renderedBlock), blocks);
    }

    @Test
    void executeLeavesNoFilterWhenEveryConstraintIsOnAFieldThatCannotBeFilteredOn() throws Exception
    {
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title"), descriptor("secret", "Password")));
        RecordsMacroParameters parameters = newParameters();
        parameters.setFilters("secret=a");

        assertNull(execute(parameters).getFilters());
    }

    @Test
    void executeSaysTheTableIsNotSortedWhenTheSourceDoesNotDescribeTheFirstColumn() throws Exception
    {
        // Live Data does not sort on a column it has no descriptor for.
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("first_name"), descriptor("secret", "Password")));
        RecordsMacroParameters parameters = newParameters();
        parameters.setSort("secret");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        assertEquals("doc.title,first_name,secret", capture().getProperties());
        assertEquals(List.of(
            warning("rendering.macro.records.warning.sortUnsupportedUnsorted[secret]"),
            this.renderedBlock), blocks);
    }

    @Test
    void executeIgnoresAFieldWithoutIdentifierAndTheRepeatsOfAField() throws Exception
    {
        LiveDataPropertyDescriptor repeat = descriptor("first_name", "Password");
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title"), descriptor(null),
            descriptor("first_name"), repeat));
        RecordsMacroParameters parameters = newParameters();
        parameters.setSort("first_name");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        LiveDataRendererParameters liveDataParameters = capture();
        assertEquals("doc.title,first_name", liveDataParameters.getProperties());
        assertEquals("first_name", liveDataParameters.getSort());
        assertEquals(List.of(this.renderedBlock), blocks);
    }

    @Test
    void executeKeepsTheAuthoredParametersOfAReaderWhoCannotViewTheDataType() throws Exception
    {
        // The source reports the fields of a data type only to a reader who can view it.
        when(this.authorization.hasAccess(Right.VIEW, DATA_TYPE)).thenReturn(false);
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title"), descriptor("doc.location")));
        RecordsMacroParameters parameters = newParameters();
        parameters.setProperties("doc.title,first_name,email");
        parameters.setFilters("first_name=Ann");
        parameters.setSort("last_name:desc");

        List<Block> blocks = this.macro.execute(parameters, null, this.context);

        LiveDataRendererParameters liveDataParameters = capture();
        assertEquals("doc.title,first_name,email", liveDataParameters.getProperties());
        assertEquals("first_name=Ann", liveDataParameters.getFilters());
        assertEquals("last_name:desc", liveDataParameters.getSort());
        assertEquals(List.of(this.renderedBlock), blocks);
    }

    @Test
    void executeShowsTheTitleAloneByDefaultToAReaderWhoCannotViewTheDataType() throws Exception
    {
        when(this.authorization.hasAccess(Right.VIEW, DATA_TYPE)).thenReturn(false);
        when(this.propertyStore.get()).thenReturn(List.of(descriptor("doc.title"), descriptor("doc.location")));

        assertEquals("doc.title", execute(newParameters()).getProperties());
    }

    @Test
    void executeReplacesTheTableWithAMessageWhenTheDataTypeNoLongerExists() throws Exception
    {
        when(this.documentAccessBridge.exists(DATA_TYPE)).thenReturn(false);

        List<Block> blocks = this.macro.execute(newParameters(), null, this.context);

        assertEquals(List.of(new MacroBlock("error", Map.of(),
            "rendering.macro.records.error.dataTypeMissing[Clients.Code.ProjectClass]", false)), blocks);
        verify(this.liveDataRenderer, never()).execute(any(LiveDataRendererParameters.class), anyString(),
            anyBoolean());
    }

    @Test
    void executeReportsADataTypeWhoseFieldsCannotBeRead() throws Exception
    {
        when(this.propertyStore.get()).thenThrow(new LiveDataException("no such class"));

        MacroExecutionException exception = assertThrows(MacroExecutionException.class,
            () -> this.macro.execute(newParameters(), null, this.context));

        assertEquals("rendering.macro.records.error.fieldsUnreadable[Clients.Code.ProjectClass]",
            exception.getMessage());
    }

    @Test
    void executeReportsADataTypeWhoseExistenceCannotBeChecked() throws Exception
    {
        when(this.documentAccessBridge.exists(DATA_TYPE)).thenThrow(new Exception("no store"));

        MacroExecutionException exception = assertThrows(MacroExecutionException.class,
            () -> this.macro.execute(newParameters(), null, this.context));

        assertEquals("rendering.macro.records.error.fieldsUnreadable[Clients.Code.ProjectClass]",
            exception.getMessage());
    }

    @Test
    void executeReportsAMissingLiveTableSource()
    {
        when(this.liveDataSourceManager.get(any(Source.class))).thenReturn(Optional.empty());

        MacroExecutionException exception = assertThrows(MacroExecutionException.class,
            () -> this.macro.execute(newParameters(), null, this.context));

        assertEquals("rendering.macro.records.error.renderFailed[]", exception.getMessage());
    }

    @Test
    void executeGeneratesAnIdentifierWhenTheAuthorSuppliedNone() throws Exception
    {
        // Live Data builds the table description's element id from it, so an absent one is not an option.
        assertEquals("records", execute(newParameters()).getId());
    }

    @Test
    void executeSuffixesTheGeneratedIdentifierOfEveryFurtherTableOnThePage() throws Exception
    {
        assertEquals("records", executeOn(this.context, newParameters()).getId());
        assertEquals("records-1", executeOn(this.context, newParameters()).getId());
        assertEquals("records-2", executeOn(this.context, newParameters()).getId());
    }

    @Test
    void executeKeepsAnAuthoredIdentifierThatIsAlreadyUnique() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        parameters.setId("projects");

        assertEquals("projects", execute(parameters).getId());
    }

    @Test
    void executeSuffixesAnAuthoredIdentifierAnotherTableAlreadyUses() throws Exception
    {
        RecordsMacroParameters first = newParameters();
        first.setId("projects");
        RecordsMacroParameters second = newParameters();
        second.setId("projects");

        assertEquals("projects", executeOn(this.context, first).getId());
        assertEquals("projects-1", executeOn(this.context, second).getId());
    }

    @Test
    void executeIsNotConfusedByAnIdentifierStartingWithADigit() throws Exception
    {
        // The generator takes the first character of an id as its prefix and rejects a non-letter, so this would
        // otherwise fail rather than render.
        RecordsMacroParameters parameters = newParameters();
        parameters.setId("2026projects");

        assertEquals("records2026projects", execute(parameters).getId());
    }

    @Test
    void executeDoesNotCollideWithAnIdentifierThePageAlreadyUsed() throws Exception
    {
        // A heading rendered before the macro takes its id from the same generator.
        this.context.getXDOM().getIdGenerator().generateUniqueId("records", "");

        assertEquals("records-1", execute(newParameters()).getId());
    }

    @Test
    void executeCopesWithNoDocumentToBeUniqueAgainst() throws Exception
    {
        // A macro executed outside a document has no XDOM, and must still hand Live Data an id.
        MacroTransformationContext bare = new MacroTransformationContext(new TransformationContext());

        assertEquals("records", executeOn(bare, newParameters()).getId());
    }

    @Test
    void executeLeavesTheParametersLiveDataDefaultsAlone() throws Exception
    {
        // The macro must not invent values for what the author left empty, otherwise a Records table and a liveData
        // macro with the same parameters would not behave the same way. The identifier is the exception, and has
        // its own tests: Live Data needs one whether the author supplied it or not.
        LiveDataRendererParameters liveDataParameters = execute(newParameters());

        assertNull(liveDataParameters.getFilters());
        assertNull(liveDataParameters.getSort());
        assertNull(liveDataParameters.getDescription());
        assertNull(liveDataParameters.getOffset());
        // The page size is deliberately not a macro parameter, so it stays Live Data's own default.
        assertNull(liveDataParameters.getLimit());
    }

    @Test
    void executeAppliesTheParameterDefaults() throws Exception
    {
        LiveDataRendererParameters liveDataParameters = execute(newParameters());

        assertEquals("table", liveDataParameters.getLayouts());
    }

    @Test
    void executePassesNoAdvancedConfigurationSoThatTheContentStaysTrusted() throws Exception
    {
        this.macro.execute(newParameters(), null, this.context);

        // LiveDataRenderer only treats the content as trusted when the advanced configuration is blank or the author
        // holds script right. Passing one here would downgrade every table authored without that right.
        verify(this.liveDataRenderer).execute(any(LiveDataRendererParameters.class), eq((String) null), eq(false));
    }

    @Test
    void executePropagatesTheRestrictedFlag() throws Exception
    {
        TransformationContext transformationContext = new TransformationContext();
        transformationContext.setRestricted(true);

        MacroTransformationContext restrictedContext = new MacroTransformationContext(transformationContext);
        restrictedContext.setXDOM(new XDOM(List.of(), new IdGenerator()));
        this.macro.execute(newParameters(), null, restrictedContext);

        verify(this.liveDataRenderer).execute(any(LiveDataRendererParameters.class), eq((String) null), eq(true));
    }

    @Test
    void executeRejectsAMissingDataType()
    {
        MacroExecutionException exception =
            assertThrows(MacroExecutionException.class,
                () -> this.macro.execute(new RecordsMacroParameters(), null, this.context));

        assertEquals("rendering.macro.records.error.noDataType[]", exception.getMessage());
    }

    @Test
    void executeWrapsALiveDataFailure() throws Exception
    {
        LiveDataException cause = new LiveDataException("no such source");
        when(this.liveDataRenderer.execute(any(LiveDataRendererParameters.class), eq((String) null), anyBoolean()))
            .thenThrow(cause);

        MacroExecutionException exception = assertThrows(MacroExecutionException.class,
            () -> this.macro.execute(newParameters(), null, this.context));

        assertEquals("rendering.macro.records.error.renderFailed[]", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void theMacroRendersABlockAndIsolatesItsExecution()
    {
        // The table is a block, so it cannot be rendered inline, and it must not leak its Live Data configuration
        // into the surrounding transformation.
        assertFalse(this.macro.supportsInlineMode());
        assertTrue(this.macro.isExecutionIsolated(newParameters(), null));
    }

    private static Block warning(String message)
    {
        return new MacroBlock("warning", Map.of(), message, false);
    }

    /**
     * @param id the property identifier
     * @return a property descriptor carrying just that identifier
     */
    private static LiveDataPropertyDescriptor descriptor(String id)
    {
        return descriptor(id, "String");
    }

    /**
     * @param id the property identifier
     * @param type the property type
     * @return a property descriptor carrying just that identifier and type, leaving the rest to the type's defaults
     */
    private static LiveDataPropertyDescriptor descriptor(String id, String type)
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setId(id);
        descriptor.setType(type);
        return descriptor;
    }

    /**
     * @param id the type identifier
     * @param enabled whether the fields of the type can be sorted and filtered on
     * @param filter the filter of the type, {@code null} for none
     * @return the defaults of the type
     */
    private static LiveDataPropertyDescriptor propertyType(String id, boolean enabled, String filter)
    {
        LiveDataPropertyDescriptor type = new LiveDataPropertyDescriptor();
        type.setId(id);
        type.setSortable(enabled);
        type.setFilterable(enabled);
        if (filter != null) {
            type.setFilter(new FilterDescriptor(filter));
            if ("boolean".equals(filter)) {
                type.getFilter().setParameter("trueValue", 1);
                type.getFilter().setParameter("falseValue", 0);
            }
        }
        return type;
    }

    /**
     * @return parameters holding only the mandatory data type, so that a test states just what it is about
     */
    private RecordsMacroParameters newParameters()
    {
        RecordsMacroParameters parameters = new RecordsMacroParameters();
        parameters.setDataType(DATA_TYPE);
        return parameters;
    }

    /**
     * Executes the macro and captures what it handed to the renderer.
     *
     * @param parameters the macro parameters
     * @return the Live Data parameters the macro built
     */
    private LiveDataRendererParameters execute(RecordsMacroParameters parameters) throws Exception
    {
        return executeOn(this.context, parameters);
    }

    /**
     * Executes the macro in a given context and captures what it handed to the renderer.
     *
     * Several tests execute the macro more than once in the same context, to exercise what a page holding more than
     * one table produces, so the captured value is the last one.
     *
     * @param context the transformation context to execute in
     * @param parameters the macro parameters
     * @return the Live Data parameters the macro built
     */
    private LiveDataRendererParameters executeOn(MacroTransformationContext context,
        RecordsMacroParameters parameters) throws Exception
    {
        this.macro.execute(parameters, null, context);
        return capture();
    }

    /**
     * @return the Live Data parameters the macro handed to the renderer last
     */
    private LiveDataRendererParameters capture() throws Exception
    {
        ArgumentCaptor<LiveDataRendererParameters> captor =
            ArgumentCaptor.forClass(LiveDataRendererParameters.class);
        verify(this.liveDataRenderer, atLeastOnce()).execute(captor.capture(), eq((String) null), anyBoolean());
        return captor.getValue();
    }
}
