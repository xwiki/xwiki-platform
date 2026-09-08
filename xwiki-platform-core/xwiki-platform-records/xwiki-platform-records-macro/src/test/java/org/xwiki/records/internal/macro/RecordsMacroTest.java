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

import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.livedata.internal.LiveDataRenderer;
import org.xwiki.livedata.internal.LiveDataRendererParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.records.macro.RecordsMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationContext;
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
import static org.mockito.ArgumentMatchers.eq;
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
    @Named("compactwiki")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private final Block renderedBlock = new GroupBlock(List.of());

    private MacroTransformationContext context;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.context = new MacroTransformationContext(new TransformationContext());
        when(this.entityReferenceSerializer.serialize(DATA_TYPE)).thenReturn(SERIALIZED_DATA_TYPE);
        when(this.liveDataSourceManager.get(any(Source.class))).thenReturn(Optional.of(this.liveDataSource));
        when(this.liveDataSource.getProperties()).thenReturn(this.propertyStore);
        // What the liveTable property store reports: the entry metadata, the data type's fields, and the Live Data
        // pseudo-columns, all in one list.
        when(this.propertyStore.get()).thenReturn(List.of(
            descriptor("doc.title"), descriptor("doc.location"),
            descriptor("_actions"), descriptor("_avatar"),
            descriptor("first_name"), descriptor("last_name"), descriptor("email")));
        when(this.liveDataRenderer.execute(any(LiveDataRendererParameters.class), eq((String) null), anyBoolean()))
            .thenReturn(this.renderedBlock);
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
        parameters.setProperties("doc.title,status,budget");
        parameters.setFilters("status=Active&client=Acme");
        parameters.setSort("budget:desc");
        parameters.setLayouts("table,cards");
        parameters.setLimit(25);
        parameters.setDescription("Acme projects, most recent first");
        parameters.setId("projects");

        LiveDataRendererParameters liveDataParameters = execute(parameters);

        assertEquals("liveTable", liveDataParameters.getSource());
        assertEquals("doc.title,status,budget", liveDataParameters.getProperties());
        assertEquals("status=Active&client=Acme", liveDataParameters.getFilters());
        assertEquals("budget:desc", liveDataParameters.getSort());
        assertEquals("table,cards", liveDataParameters.getLayouts());
        assertEquals(25, liveDataParameters.getLimit());
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
    void executeDisplaysEveryFieldOfTheDataTypeWhenNoColumnIsGiven() throws Exception
    {
        // The liveTable source needs an explicit column list: an absent one yields a table with no columns rather
        // than one with every column, which is the opposite of the parameter's documented default.
        LiveDataRendererParameters liveDataParameters = execute(newParameters());

        assertEquals("first_name,last_name,email", liveDataParameters.getProperties());
    }

    @Test
    void executeLeavesTheEntryMetadataOutOfTheDefaultColumns() throws Exception
    {
        // doc.* columns are offered to the author but are not part of "every field of this data type", and the
        // pseudo-columns are affordances rather than data.
        String properties = execute(newParameters()).getProperties();

        assertFalse(properties.contains("doc."));
        assertFalse(properties.contains("_actions"));
        assertFalse(properties.contains("_avatar"));
    }

    @Test
    void executeKeepsTheAuthoredColumnsAndDoesNotQueryTheDataType() throws Exception
    {
        RecordsMacroParameters parameters = newParameters();
        parameters.setProperties("doc.title,email");

        assertEquals("doc.title,email", execute(parameters).getProperties());
        verify(this.propertyStore, never()).get();
    }

    @Test
    void executeReportsADataTypeWhoseFieldsCannotBeRead() throws Exception
    {
        when(this.propertyStore.get()).thenThrow(new LiveDataException("no such class"));

        MacroExecutionException exception = assertThrows(MacroExecutionException.class,
            () -> this.macro.execute(newParameters(), null, this.context));

        assertEquals("Failed to read the fields of [Clients.Code.ProjectClass].", exception.getMessage());
    }

    @Test
    void executeReportsAMissingLiveTableSource()
    {
        when(this.liveDataSourceManager.get(any(Source.class))).thenReturn(Optional.empty());

        MacroExecutionException exception = assertThrows(MacroExecutionException.class,
            () -> this.macro.execute(newParameters(), null, this.context));

        assertEquals("The [liveTable] Live Data source is not available.", exception.getMessage());
    }

    @Test
    void executeLeavesTheParametersLiveDataDefaultsAlone() throws Exception
    {
        // The macro must not invent values for what the author left empty, otherwise a Records table and a liveData
        // macro with the same parameters would not behave the same way.
        LiveDataRendererParameters liveDataParameters = execute(newParameters());

        assertNull(liveDataParameters.getFilters());
        assertNull(liveDataParameters.getSort());
        assertNull(liveDataParameters.getId());
        assertNull(liveDataParameters.getDescription());
        assertNull(liveDataParameters.getOffset());
    }

    @Test
    void executeAppliesTheParameterDefaults() throws Exception
    {
        LiveDataRendererParameters liveDataParameters = execute(newParameters());

        assertEquals("table", liveDataParameters.getLayouts());
        assertEquals(15, liveDataParameters.getLimit());
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

        this.macro.execute(newParameters(), null, new MacroTransformationContext(transformationContext));

        verify(this.liveDataRenderer).execute(any(LiveDataRendererParameters.class), eq((String) null), eq(true));
    }

    @Test
    void executeRejectsAMissingDataType()
    {
        MacroExecutionException exception =
            assertThrows(MacroExecutionException.class,
                () -> this.macro.execute(new RecordsMacroParameters(), null, this.context));

        assertEquals("The [class] parameter is mandatory.", exception.getMessage());
    }

    @Test
    void executeWrapsALiveDataFailure() throws Exception
    {
        LiveDataException cause = new LiveDataException("no such source");
        when(this.liveDataRenderer.execute(any(LiveDataRendererParameters.class), eq((String) null), anyBoolean()))
            .thenThrow(cause);

        MacroExecutionException exception = assertThrows(MacroExecutionException.class,
            () -> this.macro.execute(newParameters(), null, this.context));

        assertEquals("Failed to render the Records macro.", exception.getMessage());
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

    /**
     * @param id the property identifier
     * @return a property descriptor carrying just that identifier
     */
    private static LiveDataPropertyDescriptor descriptor(String id)
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setId(id);
        return descriptor;
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
        this.macro.execute(parameters, null, this.context);

        ArgumentCaptor<LiveDataRendererParameters> captor =
            ArgumentCaptor.forClass(LiveDataRendererParameters.class);
        verify(this.liveDataRenderer).execute(captor.capture(), eq((String) null), anyBoolean());
        return captor.getValue();
    }
}
