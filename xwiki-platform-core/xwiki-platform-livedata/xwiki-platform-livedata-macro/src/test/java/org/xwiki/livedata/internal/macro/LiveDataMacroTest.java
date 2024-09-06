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
package org.xwiki.livedata.internal.macro;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.internal.RestrictedConfigurationSourceProvider;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataActionDescriptor;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataEntryDescriptor;
import org.xwiki.livedata.LiveDataLayoutDescriptor;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPaginationConfiguration;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataSelectionConfiguration;
import org.xwiki.livedata.internal.DefaultLiveDataConfigurationResolver;
import org.xwiki.livedata.internal.LiveDataRenderer;
import org.xwiki.livedata.internal.LiveDataRendererConfiguration;
import org.xwiki.livedata.internal.StringLiveDataConfigurationResolver;
import org.xwiki.livedata.macro.LiveDataMacroParameters;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.internal.renderer.html5.HTML5Renderer;
import org.xwiki.rendering.internal.renderer.html5.HTML5RendererFactory;
import org.xwiki.rendering.internal.renderer.xhtml.image.DefaultXHTMLImageRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.image.DefaultXHTMLImageTypeRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.DefaultXHTMLLinkRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.DefaultXHTMLLinkTypeRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.xml.internal.html.DefaultHTMLElementSanitizer;
import org.xwiki.xml.internal.html.HTMLDefinitions;
import org.xwiki.xml.internal.html.HTMLElementSanitizerConfiguration;
import org.xwiki.xml.internal.html.MathMLDefinitions;
import org.xwiki.xml.internal.html.SVGDefinitions;
import org.xwiki.xml.internal.html.SecureHTMLElementSanitizer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.when;
import static org.xwiki.rendering.test.integration.junit5.BlockAssert.assertBlocks;

/**
 * Unit tests for {@link LiveDataMacro}.
 *
 * @version $Id$
 * @since 12.10
 */
@ComponentTest
@ComponentList({
    HTML5RendererFactory.class,
    HTML5Renderer.class,
    DefaultXHTMLLinkRenderer.class,
    DefaultXHTMLLinkTypeRenderer.class,
    DefaultXHTMLImageRenderer.class,
    DefaultXHTMLImageTypeRenderer.class,
    DefaultHTMLElementSanitizer.class,
    SecureHTMLElementSanitizer.class,
    HTMLElementSanitizerConfiguration.class,
    RestrictedConfigurationSourceProvider.class,
    HTMLDefinitions.class,
    MathMLDefinitions.class,
    SVGDefinitions.class,
    DefaultExecution.class,
    LiveDataRendererConfiguration.class,
    LiveDataRenderer.class,
    DefaultLiveDataConfigurationResolver.class,
    StringLiveDataConfigurationResolver.class
})
class LiveDataMacroTest
{
    @InjectMockComponents
    private LiveDataMacro liveDataMacro;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private IconManager iconManager;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @MockComponent
    private RenderingContext renderingContext;

    @MockComponent
    @Named("jsfx")
    private SkinExtension jsfx;

    @Inject
    @Named("html/5.0")
    private PrintRendererFactory rendererFactory;

    @Mock
    private MacroTransformationContext macroTransformationContext;

    @Mock
    private TransformationContext transformationContext;

    private LiveDataConfiguration liveDataConfiguration;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);

        // setup default LD configuration
        this.liveDataConfiguration = new LiveDataConfiguration();
        LiveDataQuery liveDataQuery = new LiveDataQuery();
        liveDataQuery.setLimit(15);
        liveDataQuery.setProperties(List.of());
        liveDataQuery.setSource(new LiveDataQuery.Source());
        liveDataQuery.setFilters(List.of());
        liveDataQuery.setSort(List.of());
        liveDataQuery.setOffset(0L);
        this.liveDataConfiguration.setQuery(liveDataQuery);

        LiveData liveData = new LiveData();
        liveData.setCount(0);
        this.liveDataConfiguration.setData(liveData);

        LiveDataMeta meta = new LiveDataMeta();
        LiveDataLayoutDescriptor tableLayout = new LiveDataLayoutDescriptor("table");
        tableLayout.setName("table");
        tableLayout.setIcon(Map.of());
        LiveDataLayoutDescriptor cardsLayout = new LiveDataLayoutDescriptor("cards");
        cardsLayout.setName("cards");
        cardsLayout.setIcon(Map.of());
        meta.setLayouts(List.of(
            tableLayout,
            cardsLayout
        ));
        meta.setDefaultLayout(tableLayout.getId());

        LiveDataPropertyDescriptor.OperatorDescriptor contains =
            new LiveDataPropertyDescriptor.OperatorDescriptor("contains", "contains");
        LiveDataPropertyDescriptor.OperatorDescriptor equals =
            new LiveDataPropertyDescriptor.OperatorDescriptor("equals", "equals");
        LiveDataPropertyDescriptor.OperatorDescriptor startsWith =
            new LiveDataPropertyDescriptor.OperatorDescriptor("startsWith", "startsWith");
        LiveDataPropertyDescriptor.FilterDescriptor textFilter =
            new LiveDataPropertyDescriptor.FilterDescriptor("text");
        textFilter.setOperators(List.of(contains, startsWith, equals));
        textFilter.setDefaultOperator("contains");

        LiveDataPropertyDescriptor.OperatorDescriptor equalsNumber =
            new LiveDataPropertyDescriptor.OperatorDescriptor("equals", "=");
        LiveDataPropertyDescriptor.OperatorDescriptor less =
            new LiveDataPropertyDescriptor.OperatorDescriptor("less", "<");
        LiveDataPropertyDescriptor.OperatorDescriptor greater =
            new LiveDataPropertyDescriptor.OperatorDescriptor("greater", ">");
        LiveDataPropertyDescriptor.FilterDescriptor numberFilter =
            new LiveDataPropertyDescriptor.FilterDescriptor("number");
        numberFilter.setOperators(List.of(equalsNumber, less, greater));
        numberFilter.setDefaultOperator("equals");

        LiveDataPropertyDescriptor.FilterDescriptor booleanFilter =
            new LiveDataPropertyDescriptor.FilterDescriptor("boolean");
        booleanFilter.setOperators(List.of(equals));
        booleanFilter.setDefaultOperator("equals");

        LiveDataPropertyDescriptor.OperatorDescriptor between =
            new LiveDataPropertyDescriptor.OperatorDescriptor("between", "between");
        LiveDataPropertyDescriptor.OperatorDescriptor before =
            new LiveDataPropertyDescriptor.OperatorDescriptor("before", "before");
        LiveDataPropertyDescriptor.OperatorDescriptor after =
            new LiveDataPropertyDescriptor.OperatorDescriptor("after", "after");
        LiveDataPropertyDescriptor.FilterDescriptor dateFilter =
            new LiveDataPropertyDescriptor.FilterDescriptor("date");
        dateFilter.setOperators(List.of(between, before, after, contains));
        dateFilter.setDefaultOperator("between");
        dateFilter.setParameter("dateFormat", "yyyy/MM/dd HH:mm");

        LiveDataPropertyDescriptor.OperatorDescriptor empty =
            new LiveDataPropertyDescriptor.OperatorDescriptor("empty", "empty");
        LiveDataPropertyDescriptor.FilterDescriptor listFilter =
            new LiveDataPropertyDescriptor.FilterDescriptor("list");
        listFilter.setOperators(List.of(equals, startsWith, contains, empty));
        listFilter.setDefaultOperator("contains");
        meta.setFilters(List.of(
            textFilter,
            numberFilter,
            booleanFilter,
            dateFilter,
            listFilter
        ));
        meta.setDefaultFilter("text");

        meta.setDisplayers(List.of(
            new LiveDataPropertyDescriptor.DisplayerDescriptor("text"),
            new LiveDataPropertyDescriptor.DisplayerDescriptor("link"),
            new LiveDataPropertyDescriptor.DisplayerDescriptor("html"),
            new LiveDataPropertyDescriptor.DisplayerDescriptor("actions"),
            new LiveDataPropertyDescriptor.DisplayerDescriptor("boolean")
        ));
        meta.setDefaultDisplayer("text");

        LiveDataPaginationConfiguration paginationConfiguration = new LiveDataPaginationConfiguration();
        paginationConfiguration.setMaxShownPages(10);
        paginationConfiguration.setPageSizes(List.of(15,25,50,100));
        paginationConfiguration.setShowEntryRange(true);
        paginationConfiguration.setShowNextPrevious(true);
        meta.setPagination(paginationConfiguration);

        meta.setPropertyDescriptors(List.of());
        meta.setPropertyTypes(List.of());
        meta.setEntryDescriptor(new LiveDataEntryDescriptor());

        LiveDataActionDescriptor view = new LiveDataActionDescriptor("view");
        view.setName("view");
        view.setIcon(Map.of());

        LiveDataActionDescriptor edit = new LiveDataActionDescriptor("edit");
        edit.setName("edit");
        edit.setIcon(Map.of());

        LiveDataActionDescriptor delete = new LiveDataActionDescriptor("delete");
        delete.setName("delete");
        delete.setIcon(Map.of("cssClass", "text-danger"));

        LiveDataActionDescriptor copy = new LiveDataActionDescriptor("copy");
        copy.setName("copy");
        copy.setIcon(Map.of());

        LiveDataActionDescriptor rename = new LiveDataActionDescriptor("rename");
        rename.setName("rename");
        rename.setIcon(Map.of());

        LiveDataActionDescriptor rights = new LiveDataActionDescriptor("rights");
        rights.setName("rights");
        rights.setIcon(Map.of());
        meta.setActions(List.of(view, edit, delete, copy, rename, rights));
        meta.setSelection(new LiveDataSelectionConfiguration());
        this.liveDataConfiguration.setMeta(meta);
    }

    @BeforeEach
    void before() throws Exception
    {
        when(this.macroTransformationContext.getTransformationContext()).thenReturn(this.transformationContext);
    }

    @Test
    void executeWithoutParams() throws Exception
    {

        String expected =
            String.format("<div class=\"liveData loading\" data-config=\"%s\" "
                + "data-config-content-trusted=\"true\"></div>",
                escapeXML(json(this.liveDataConfiguration)));

        List<Block> blocks =
            this.liveDataMacro.execute(new LiveDataMacroParameters(), null, this.macroTransformationContext);
        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void execute() throws Exception
    {
        this.liveDataConfiguration.setId("test");
        LiveDataQuery query = this.liveDataConfiguration.getQuery();
        query.setProperties(List.of("avatar", "firstName", "lastName", "position"));
        LiveDataQuery.Source source = new LiveDataQuery.Source("users");
        source.setParameter("wiki", "dev");
        source.setParameter("group", "apps");
        query.setSource(source);
        query.setFilters(List.of(
            new LiveDataQuery.Filter("firstName", "m"),
            new LiveDataQuery.Filter("position", "lead")
        ));
        query.setSort(List.of(
            new LiveDataQuery.SortEntry("firstName"),
            new LiveDataQuery.SortEntry("lastName", true),
            new LiveDataQuery.SortEntry("position")
        ));
        query.setOffset(20L);
        query.setLimit(10);

        LiveDataPaginationConfiguration pagination = this.liveDataConfiguration.getMeta().getPagination();
        pagination.setPageSizes(List.of(10,15,25,50));
        pagination.setShowPageSizeDropdown(true);

        this.liveDataConfiguration.getMeta().setDescription("A description");

        String expected = String.format("<div class=\"liveData loading\" id=\"test\" data-config=\"%s\" "
            + "data-config-content-trusted=\"true\"></div>", escapeXML(json(this.liveDataConfiguration)));

        LiveDataMacroParameters parameters = new LiveDataMacroParameters();
        parameters.setId("test");
        parameters.setSource("users");
        parameters.setSourceParameters("wiki=dev&group=apps");
        parameters.setProperties("avatar, firstName, lastName, position");
        parameters.setSort("firstName, lastName:desc, position:asc");
        parameters.setFilters("firstName=m&position=lead");
        parameters.setLimit(10);
        parameters.setOffset(20L);
        parameters.setLayouts("table, cards");
        parameters.setShowPageSizeDropdown(true);
        parameters.setPageSizes("15, 25, 50");
        parameters.setDescription("A description");

        List<Block> blocks = this.liveDataMacro.execute(parameters, null, this.macroTransformationContext);
        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWithContent() throws Exception
    {
        LiveDataMacroParameters parameters = new LiveDataMacroParameters();
        parameters.setId("test");
        parameters.setSource("users");
        parameters.setProperties("avatar, firstName, lastName, position");
        parameters.setSort("firstName");
        parameters.setLimit(10);

        StringBuilder advancedConfig = new StringBuilder();
        advancedConfig.append("{");
        advancedConfig.append("  'query': {".trim());
        advancedConfig.append("    'filters': [".trim());
        advancedConfig.append("      {'property': 'position', 'constraints':[{'value':'R&D'}]}".trim());
        advancedConfig.append("    ],".trim());
        advancedConfig.append("    'sort': [{'property': 'lastName', 'descending':true}],".trim());
        advancedConfig.append("    'limit': 15".trim());
        advancedConfig.append("  }".trim());
        advancedConfig.append("}");

        this.liveDataConfiguration.setId("test");
        LiveDataQuery query = this.liveDataConfiguration.getQuery();
        query.setProperties(List.of("avatar", "firstName", "lastName", "position"));
        LiveDataQuery.Source source = new LiveDataQuery.Source("users");
        query.setSource(source);
        query.setFilters(List.of(
            new LiveDataQuery.Filter("position", "R&D")
        ));
        query.setSort(List.of(
            new LiveDataQuery.SortEntry("firstName")
        ));
        query.setLimit(10);

        LiveDataPaginationConfiguration pagination = this.liveDataConfiguration.getMeta().getPagination();
        pagination.setPageSizes(List.of(10,15,25,50,100));

        String expected = String.format("<div class=\"liveData loading\" id=\"test\" data-config=\"%s\" "
            + "data-config-content-trusted=\"false\"></div>", escapeXML(json(this.liveDataConfiguration)));

        List<Block> blocks = this.liveDataMacro.execute(parameters, json(advancedConfig.toString()),
            this.macroTransformationContext);
        assertBlocks(expected, blocks, this.rendererFactory);
    }

    private String json(String text)
    {
        return text.replace('\'', '"');
    }

    private String json(LiveDataConfiguration liveDataConfiguration) throws JsonProcessingException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper.writeValueAsString(liveDataConfiguration);
    }

    private String escapeXML(String value)
    {
        return StringEscapeUtils.escapeXml10(value).replace("{", "&#123;");
    }
}
