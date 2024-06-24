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

import java.util.ArrayList;
import java.util.List;

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
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
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

import static org.mockito.ArgumentMatchers.any;
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

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @BeforeEach
    void before() throws Exception
    {
        when(this.macroTransformationContext.getTransformationContext()).thenReturn(this.transformationContext);
    }

    @Test
    void executeWithoutParams() throws Exception
    {
        StringBuilder expectedConfig = new StringBuilder();
        expectedConfig.append("{");
        expectedConfig.append("  'query':{".trim());
        expectedConfig.append("    'properties':[],'source':{},'filters':[],'sort':[],'offset':0,'limit':15".trim());
        expectedConfig.append("  },".trim());
        expectedConfig.append("  'data':{'count':0,'entries':[]},".trim());
        expectedConfig.append("  'meta':{".trim());
        expectedConfig.append("    'layouts':[".trim());
        expectedConfig.append("      {'id':'table','name':'table','icon':{}},".trim());
        expectedConfig.append("      {'id':'cards','name':'cards','icon':{}}".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'defaultLayout':'table',".trim());
        expectedConfig.append("    'propertyDescriptors':[],".trim());
        expectedConfig.append("    'propertyTypes':[],".trim());
        expectedConfig.append("    'filters':[".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'text',".trim());
        expectedConfig.append("        'defaultOperator':'contains',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'contains','name':'contains'},".trim());
        expectedConfig.append("          {'id':'startsWith','name':'startsWith'},".trim());
        expectedConfig.append("          {'id':'equals','name':'equals'}".trim());
        expectedConfig.append("        ]".trim());
        expectedConfig.append("      },".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'number',".trim());
        expectedConfig.append("        'defaultOperator':'equals',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'equals','name':'='},".trim());
        expectedConfig.append("          {'id':'less','name':'<'},".trim());
        expectedConfig.append("          {'id':'greater','name':'>'}".trim());
        expectedConfig.append("        ]".trim());
        expectedConfig.append("      },".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'boolean',".trim());
        expectedConfig.append("        'defaultOperator':'equals',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'equals','name':'equals'}".trim());
        expectedConfig.append("        ]".trim());
        expectedConfig.append("      },".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'date',".trim());
        expectedConfig.append("        'defaultOperator':'between',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'between','name':'between'},".trim());
        expectedConfig.append("          {'id':'before','name':'before'},".trim());
        expectedConfig.append("          {'id':'after','name':'after'},".trim());
        expectedConfig.append("          {'id':'contains','name':'contains'}".trim());
        expectedConfig.append("        ],".trim());
        expectedConfig.append("        'dateFormat':'yyyy/MM/dd HH:mm'".trim());
        expectedConfig.append("      },".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'list',".trim());
        expectedConfig.append("        'defaultOperator':'contains',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'equals','name':'equals'},".trim());
        expectedConfig.append("          {'id':'startsWith','name':'startsWith'},".trim());
        expectedConfig.append("          {'id':'contains','name':'contains'},".trim());
        expectedConfig.append("          {'id':'empty','name':'empty'}".trim());
        expectedConfig.append("        ]".trim());
        expectedConfig.append("      }".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'defaultFilter':'text',".trim());
        expectedConfig.append("    'displayers':[".trim());
        expectedConfig.append("      {'id':'text'},".trim());
        expectedConfig.append("      {'id':'link'},".trim());
        expectedConfig.append("      {'id':'html'},".trim());
        expectedConfig.append("      {'id':'actions'},".trim());
        expectedConfig.append("      {'id':'boolean'}".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'defaultDisplayer':'text',".trim());
        expectedConfig.append("    'pagination':{".trim());
        expectedConfig.append("      'maxShownPages':10,".trim());
        expectedConfig.append("      'pageSizes':[15,25,50,100],".trim());
        expectedConfig.append("      'showEntryRange':true,".trim());
        expectedConfig.append("      'showNextPrevious':true".trim());
        expectedConfig.append("    },".trim());
        expectedConfig.append("    'entryDescriptor':{},".trim());
        expectedConfig.append("    'actions':[".trim());
        expectedConfig.append("      {'id':'view','name':'view','icon':{}},".trim());
        expectedConfig.append("      {'id':'edit','name':'edit','icon':{}},".trim());
        expectedConfig.append("      {'id':'delete','name':'delete','icon':{'cssClass':'text-danger'}},".trim());
        expectedConfig.append("      {'id':'copy','name':'copy','icon':{}},".trim());
        expectedConfig.append("      {'id':'rename','name':'rename','icon':{}},".trim());
        expectedConfig.append("      {'id':'rights','name':'rights','icon':{}}".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'selection':{'enabled':false}".trim());
        expectedConfig.append("  }".trim());
        expectedConfig.append("}".trim());
        String expected =
            String.format("<div class=\"liveData loading\" data-config=\"%s\" "
                + "data-config-content-trusted=\"true\"></div>", escapeXML(json(expectedConfig.toString())));

        List<Block> blocks =
            this.liveDataMacro.execute(new LiveDataMacroParameters(), null, this.macroTransformationContext);
        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void execute() throws Exception
    {
        StringBuilder expectedConfig = new StringBuilder();
        expectedConfig.append("{");
        expectedConfig.append("  'id':'test',".trim());
        expectedConfig.append("  'query':{".trim());
        expectedConfig.append("    'properties':['avatar','firstName','lastName','position'],".trim());
        expectedConfig.append("    'source':{'id':'users','wiki':'dev','group':'apps'},".trim());
        expectedConfig.append("    'filters':[".trim());
        expectedConfig.append("      {'property':'firstName','constraints':[{'value':'m'}]},".trim());
        expectedConfig.append("      {'property':'position','constraints':[{'value':'lead'}]}".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'sort':[".trim());
        expectedConfig.append("      {'property':'firstName'},".trim());
        expectedConfig.append("      {'property':'lastName','descending':true},".trim());
        expectedConfig.append("      {'property':'position'}],".trim());
        expectedConfig.append("    'offset':20,".trim());
        expectedConfig.append("    'limit':10".trim());
        expectedConfig.append("  },".trim());
        expectedConfig.append("  'data':{'count':0,'entries':[]},".trim());
        expectedConfig.append("  'meta':{".trim());
        expectedConfig.append("    'layouts':[".trim());
        expectedConfig.append("      {'id':'table','name':'table','icon':{}},".trim());
        expectedConfig.append("      {'id':'cards','name':'cards','icon':{}}".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'defaultLayout':'table',".trim());
        expectedConfig.append("    'propertyDescriptors':[],".trim());
        expectedConfig.append("    'propertyTypes':[],".trim());
        expectedConfig.append("    'filters':[".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'text',".trim());
        expectedConfig.append("        'defaultOperator':'contains',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'contains','name':'contains'},".trim());
        expectedConfig.append("          {'id':'startsWith','name':'startsWith'},".trim());
        expectedConfig.append("          {'id':'equals','name':'equals'}".trim());
        expectedConfig.append("        ]".trim());
        expectedConfig.append("      },".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'number',".trim());
        expectedConfig.append("        'defaultOperator':'equals',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'equals','name':'='},".trim());
        expectedConfig.append("          {'id':'less','name':'<'},".trim());
        expectedConfig.append("          {'id':'greater','name':'>'}".trim());
        expectedConfig.append("        ]".trim());
        expectedConfig.append("      },".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'boolean',".trim());
        expectedConfig.append("        'defaultOperator':'equals',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'equals','name':'equals'}".trim());
        expectedConfig.append("        ]".trim());
        expectedConfig.append("      },".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'date',".trim());
        expectedConfig.append("        'defaultOperator':'between',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'between','name':'between'},".trim());
        expectedConfig.append("          {'id':'before','name':'before'},".trim());
        expectedConfig.append("          {'id':'after','name':'after'},".trim());
        expectedConfig.append("          {'id':'contains','name':'contains'}".trim());
        expectedConfig.append("        ],".trim());
        expectedConfig.append("        'dateFormat':'yyyy/MM/dd HH:mm'".trim());
        expectedConfig.append("      },".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'list',".trim());
        expectedConfig.append("        'defaultOperator':'contains',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'equals','name':'equals'},".trim());
        expectedConfig.append("          {'id':'startsWith','name':'startsWith'},".trim());
        expectedConfig.append("          {'id':'contains','name':'contains'},".trim());
        expectedConfig.append("          {'id':'empty','name':'empty'}".trim());
        expectedConfig.append("        ]".trim());
        expectedConfig.append("      }".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'defaultFilter':'text',".trim());
        expectedConfig.append("    'displayers':[".trim());
        expectedConfig.append("      {'id':'text'},".trim());
        expectedConfig.append("      {'id':'link'},".trim());
        expectedConfig.append("      {'id':'html'},".trim());
        expectedConfig.append("      {'id':'actions'},".trim());
        expectedConfig.append("      {'id':'boolean'}".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'defaultDisplayer':'text',".trim());
        expectedConfig.append("    'pagination':{".trim());
        expectedConfig.append("      'maxShownPages':10,".trim());
        expectedConfig.append("      'pageSizes':[10,15,25,50],".trim());
        expectedConfig.append("      'showEntryRange':true,".trim());
        expectedConfig.append("      'showNextPrevious':true,".trim());
        expectedConfig.append("      'showPageSizeDropdown':true".trim());
        expectedConfig.append("    },".trim());
        expectedConfig.append("    'entryDescriptor':{},".trim());
        expectedConfig.append("    'actions':[".trim());
        expectedConfig.append("      {'id':'view','name':'view','icon':{}},".trim());
        expectedConfig.append("      {'id':'edit','name':'edit','icon':{}},".trim());
        expectedConfig.append("      {'id':'delete','name':'delete','icon':{'cssClass':'text-danger'}},".trim());
        expectedConfig.append("      {'id':'copy','name':'copy','icon':{}},".trim());
        expectedConfig.append("      {'id':'rename','name':'rename','icon':{}},".trim());
        expectedConfig.append("      {'id':'rights','name':'rights','icon':{}}".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'selection':{'enabled':false},".trim());
        expectedConfig.append("    'description':'A description'".trim());
        expectedConfig.append("  }".trim());
        expectedConfig.append("}".trim());

        String expected = String.format("<div class=\"liveData loading\" id=\"test\" data-config=\"%s\" "
            + "data-config-content-trusted=\"true\"></div>", escapeXML(json(expectedConfig.toString())));

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


        StringBuilder expectedConfig = new StringBuilder();
        expectedConfig.append("{");
        expectedConfig.append("  'id':'test',".trim());
        expectedConfig.append("  'query':{".trim());
        expectedConfig.append("    'properties':['avatar','firstName','lastName','position'],".trim());
        expectedConfig.append("    'source':{'id':'users'},".trim());
        expectedConfig.append("    'filters':[".trim());
        expectedConfig.append("      {'property':'position','constraints':[{'value':'R&D'}]}".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'sort':[".trim());
        expectedConfig.append("      {'property':'firstName'}".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'offset':0,".trim());
        expectedConfig.append("    'limit':10".trim());
        expectedConfig.append("  },".trim());
        expectedConfig.append("  'data':{'count':0,'entries':[]},".trim());
        expectedConfig.append("  'meta':{".trim());
        expectedConfig.append("    'layouts':[".trim());
        expectedConfig.append("      {'id':'table','name':'table','icon':{}},".trim());
        expectedConfig.append("      {'id':'cards','name':'cards','icon':{}}".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'defaultLayout':'table',".trim());
        expectedConfig.append("    'propertyDescriptors':[],".trim());
        expectedConfig.append("    'propertyTypes':[],".trim());
        expectedConfig.append("    'filters':[".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'text',".trim());
        expectedConfig.append("        'defaultOperator':'contains',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'contains','name':'contains'},".trim());
        expectedConfig.append("          {'id':'startsWith','name':'startsWith'},".trim());
        expectedConfig.append("          {'id':'equals','name':'equals'}".trim());
        expectedConfig.append("        ]".trim());
        expectedConfig.append("      },".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'number',".trim());
        expectedConfig.append("        'defaultOperator':'equals',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'equals','name':'='},".trim());
        expectedConfig.append("          {'id':'less','name':'<'},".trim());
        expectedConfig.append("          {'id':'greater','name':'>'}".trim());
        expectedConfig.append("        ]".trim());
        expectedConfig.append("      },".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'boolean',".trim());
        expectedConfig.append("        'defaultOperator':'equals',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'equals','name':'equals'}".trim());
        expectedConfig.append("        ]".trim());
        expectedConfig.append("      },".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'date',".trim());
        expectedConfig.append("        'defaultOperator':'between',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'between','name':'between'},".trim());
        expectedConfig.append("          {'id':'before','name':'before'},".trim());
        expectedConfig.append("          {'id':'after','name':'after'},".trim());
        expectedConfig.append("          {'id':'contains','name':'contains'}".trim());
        expectedConfig.append("        ],".trim());
        expectedConfig.append("        'dateFormat':'yyyy/MM/dd HH:mm'".trim());
        expectedConfig.append("      },".trim());
        expectedConfig.append("      {".trim());
        expectedConfig.append("        'id':'list',".trim());
        expectedConfig.append("        'defaultOperator':'contains',".trim());
        expectedConfig.append("        'operators':[".trim());
        expectedConfig.append("          {'id':'equals','name':'equals'},".trim());
        expectedConfig.append("          {'id':'startsWith','name':'startsWith'},".trim());
        expectedConfig.append("          {'id':'contains','name':'contains'},".trim());
        expectedConfig.append("          {'id':'empty','name':'empty'}".trim());
        expectedConfig.append("        ]".trim());
        expectedConfig.append("      }".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'defaultFilter':'text',".trim());
        expectedConfig.append("    'displayers':[".trim());
        expectedConfig.append("      {'id':'text'},".trim());
        expectedConfig.append("      {'id':'link'},".trim());
        expectedConfig.append("      {'id':'html'},".trim());
        expectedConfig.append("      {'id':'actions'},".trim());
        expectedConfig.append("      {'id':'boolean'}".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'defaultDisplayer':'text',".trim());
        expectedConfig.append("    'pagination':{".trim());
        expectedConfig.append("      'maxShownPages':10,".trim());
        expectedConfig.append("      'pageSizes':[10,15,25,50,100],".trim());
        expectedConfig.append("      'showEntryRange':true,".trim());
        expectedConfig.append("      'showNextPrevious':true".trim());
        expectedConfig.append("    },".trim());
        expectedConfig.append("    'entryDescriptor':{},".trim());
        expectedConfig.append("    'actions':[".trim());
        expectedConfig.append("      {'id':'view','name':'view','icon':{}},".trim());
        expectedConfig.append("      {'id':'edit','name':'edit','icon':{}},".trim());
        expectedConfig.append("      {'id':'delete','name':'delete','icon':{'cssClass':'text-danger'}},".trim());
        expectedConfig.append("      {'id':'copy','name':'copy','icon':{}},".trim());
        expectedConfig.append("      {'id':'rename','name':'rename','icon':{}},".trim());
        expectedConfig.append("      {'id':'rights','name':'rights','icon':{}}".trim());
        expectedConfig.append("    ],".trim());
        expectedConfig.append("    'selection':{'enabled':false}".trim());
        expectedConfig.append("  }".trim());
        expectedConfig.append("}".trim());

        String expected = String.format("<div class=\"liveData loading\" id=\"test\" data-config=\"%s\" "
            + "data-config-content-trusted=\"false\"></div>", escapeXML(json(expectedConfig.toString())));

        List<Block> blocks = this.liveDataMacro.execute(parameters, json(advancedConfig.toString()),
            this.macroTransformationContext);
        assertBlocks(expected, blocks, this.rendererFactory);
    }

    private String json(String text)
    {
        return text.replace('\'', '"');
    }

    private String escapeXML(String value)
    {
        return StringEscapeUtils.escapeXml10(value).replace("{", "&#123;");
    }
}
