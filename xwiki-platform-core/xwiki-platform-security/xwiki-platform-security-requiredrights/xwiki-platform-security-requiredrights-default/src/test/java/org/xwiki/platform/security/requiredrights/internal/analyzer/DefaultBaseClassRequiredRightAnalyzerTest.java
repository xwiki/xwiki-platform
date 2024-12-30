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
package org.xwiki.platform.security.requiredrights.internal.analyzer;

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryException;
import org.xwiki.query.hql.internal.HQLStatementValidator;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ComputedFieldClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Component test for {@link DefaultBaseClassRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
@ComponentTest
@ComponentList({
    VelocityDetector.class,
    ComputedFieldClassRequiredRightAnalyzer.class,
    DBListClassRequiredRightAnalyzer.class,
    PropertyClassRequiredRightAnalyzer.class,
    XClassWikiContentAnalyzer.class
})
class DefaultBaseClassRequiredRightAnalyzerTest
{
    protected static final ClassPropertyReference PROPERTY_REFERENCE = mock();

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final Syntax SYNTAX = mock();

    @InjectMockComponents
    private DefaultBaseClassRequiredRightAnalyzer defaultBaseClassRequiredrightAnalyzer;

    @MockComponent
    private QueryBuilder<DBListClass> dbListClassQueryBuilder;

    @MockComponent
    private HibernateStore hibernate;

    @MockComponent
    private ContentParser contentParser;

    @MockComponent
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @MockComponent
    private HQLStatementValidator hqlStatementValidator;

    @MockComponent
    @Named("stringCode")
    private BlockSupplierProvider<String> stringCodeBlockSupplierProvider;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> contextComponentManager;

    @BeforeEach
    void beforeEach()
    {
        when(this.contextComponentManager.get()).thenReturn(this.componentManager);
    }

    @Test
    void analyzeWithEmptyCustomDisplay() throws Exception
    {
        PropertyClass propertyClass = mock();
        when(propertyClass.getCustomDisplay()).thenReturn("");

        assertWikiContentAnalysis(propertyClass, "", false);
    }

    @Test
    void analyzeWithWikiCustomDisplay() throws Exception
    {
        String content = "Wiki content.";
        PropertyClass propertyClass = mock();
        when(propertyClass.getCustomDisplay()).thenReturn(content);

        assertWikiContentAnalysis(propertyClass, content, true);
    }

    @Test
    void analyzeEmptyComputedProperty() throws Exception
    {
        ComputedFieldClass computedFieldClass = mock(ComputedFieldClass.class);
        when(computedFieldClass.getScript()).thenReturn("");

        assertWikiContentAnalysis(computedFieldClass, "", false);
    }

    @Test
    void analyzeWikiComputedProperty() throws Exception
    {
        String content = "Computed property.";
        ComputedFieldClass computedFieldClass = mock(ComputedFieldClass.class);
        when(computedFieldClass.getScript()).thenReturn(content);

        assertWikiContentAnalysis(computedFieldClass, content, true);
    }

    @Test
    void analyzeComputedPropertyWhenParsingThrows() throws Exception
    {
        ComputedFieldClass computedFieldClass = mock(ComputedFieldClass.class);
        when(computedFieldClass.getScript()).thenReturn("Script");

        when(this.contentParser.parse("Script", SYNTAX, DOCUMENT_REFERENCE))
            .thenThrow(new ParseException("Test Exception"));

        List<RequiredRightAnalysisResult> results =
            this.defaultBaseClassRequiredrightAnalyzer.analyze(mockBaseClass(computedFieldClass));

        assertEquals(1, results.size());
        RequiredRightAnalysisResult result = results.get(0);
        assertEquals(computedFieldClass.getReference(), result.getEntityReference());
        assertEquals(List.of(RequiredRight.MAYBE_PROGRAM), result.getRequiredRights());
    }

    private void assertWikiContentAnalysis(PropertyClass propertyClass, String expectedContent, boolean hasWikiContent)
        throws ParseException, MissingParserException, RequiredRightsException
    {
        BaseClass xClass = mockBaseClass(propertyClass);
        XDOM xdom = new XDOM(List.of(new WordBlock("TestContent")));
        when(this.contentParser.parse(expectedContent, SYNTAX, DOCUMENT_REFERENCE)).thenReturn(xdom);

        RequiredRightAnalysisResult expectedResult = mock();
        when(this.xdomRequiredRightAnalyzer.analyze(xdom)).thenReturn(List.of(expectedResult));

        List<RequiredRightAnalysisResult> results = this.defaultBaseClassRequiredrightAnalyzer.analyze(xClass);

        if (hasWikiContent) {
            assertEquals(1, results.size());
            RequiredRightAnalysisResult result = results.get(0);
            assertEquals(expectedResult, result);
            verify(this.contentParser).parse(expectedContent, SYNTAX, DOCUMENT_REFERENCE);
            verify(this.xdomRequiredRightAnalyzer).analyze(xdom);
            assertEquals(PROPERTY_REFERENCE,
                xdom.getMetaData().getMetaData(XDOMRequiredRightAnalyzer.ENTITY_REFERENCE_METADATA));
        } else {
            verifyNoInteractions(this.contentParser);
            verifyNoInteractions(this.xdomRequiredRightAnalyzer);
            assertEquals(List.of(), results);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "select doc.title from XWikiDocument as doc, false, false",
        "select doc.fullName from XWikiDocument as doc, false, true",
        "#set($x = 1) SELECT * FROM XWikiDocument, true, false",
        " , false, true"
    })
    void analyzeDBListClassSQL(String sql, boolean requiresScript, boolean isSafe) throws Exception
    {
        DBListClass dbListClass = mock(DBListClass.class);
        when(dbListClass.getSql()).thenReturn(sql);
        BaseClass xClass = mockBaseClass(dbListClass);

        Query query = mock();
        when(query.getStatement()).thenReturn(sql);
        when(this.dbListClassQueryBuilder.build(dbListClass)).thenReturn(query);

        when(this.hqlStatementValidator.isSafe(sql)).thenReturn(isSafe);

        List<RequiredRightAnalysisResult> results =
            this.defaultBaseClassRequiredrightAnalyzer.analyze(xClass);

        if (requiresScript) {
            assertEquals(1, results.size());
            RequiredRightAnalysisResult result = results.get(0);
            assertEquals(PROPERTY_REFERENCE, result.getEntityReference());
            assertEquals(RequiredRight.SCRIPT_AND_MAYBE_PROGRAM, result.getRequiredRights());
        } else if (isSafe) {
            assertEquals(List.of(), results);
        } else {
            assertEquals(1, results.size());
            RequiredRightAnalysisResult result = results.get(0);
            assertEquals(dbListClass.getReference(), result.getEntityReference());
            assertEquals(List.of(RequiredRight.PROGRAM), result.getRequiredRights());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "select doc.title from XWikiDocument as doc, false",
        "select doc.fullName from XWikiDocument as doc, true",
    })
    void analyzeDBListClassWithNamedQuery(String hqlQuery, boolean safe) throws Exception
    {
        DBListClass dbListClass = mock(DBListClass.class);
        String queryName = "namedQuery";
        when(dbListClass.getSql()).thenReturn(queryName);
        Query query = mock();
        when(query.isNamed()).thenReturn(true);
        when(query.getStatement()).thenReturn(queryName);
        when(this.dbListClassQueryBuilder.build(dbListClass)).thenReturn(query);
        when(this.hibernate.getConfiguration()).thenReturn(mock());
        when(this.hibernate.getConfiguration().getNamedQueries()).thenReturn(Map.of(queryName, mock()));
        when(this.hibernate.getConfiguration().getNamedQueries()
            .get(queryName).getQuery()).thenReturn(hqlQuery);

        when(this.hqlStatementValidator.isSafe(hqlQuery)).thenReturn(safe);

        List<RequiredRightAnalysisResult> results =
            this.defaultBaseClassRequiredrightAnalyzer.analyze(mockBaseClass(dbListClass));

        if (safe) {
            assertEquals(List.of(), results);
        } else {
            assertEquals(1, results.size());
            RequiredRightAnalysisResult result = results.get(0);
            assertEquals(PROPERTY_REFERENCE, result.getEntityReference());
            assertEquals(List.of(RequiredRight.PROGRAM), result.getRequiredRights());
        }
    }

    @Test
    void analyzeDBListClassWithQueryException() throws RequiredRightsException, QueryException
    {
        DBListClass dbListClass = mock(DBListClass.class);
        when(dbListClass.getSql()).thenReturn("query");

        when(this.dbListClassQueryBuilder.build(dbListClass)).thenThrow(new QueryException("Error parsing query",
            mock(), new Exception("Test")));

        List<RequiredRightAnalysisResult> results =
            this.defaultBaseClassRequiredrightAnalyzer.analyze(mockBaseClass(dbListClass));

        assertEquals(1, results.size());
        RequiredRightAnalysisResult result = results.get(0);
        assertEquals(PROPERTY_REFERENCE, result.getEntityReference());
        assertEquals(List.of(RequiredRight.MAYBE_PROGRAM), result.getRequiredRights());
    }

    private static BaseClass mockBaseClass(PropertyClass propertyClass)
    {
        when(propertyClass.getReference()).thenReturn(PROPERTY_REFERENCE);
        BaseClass xClass = mock();
        when(xClass.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(xClass.getFieldList()).thenReturn(List.of(propertyClass));
        XWikiDocument document = mock();
        when(document.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(document.getSyntax()).thenReturn(SYNTAX);
        when(document.getXClass()).thenReturn(xClass);
        when(xClass.getOwnerDocument()).thenReturn(document);
        when(propertyClass.getOwnerDocument()).thenReturn(document);
        return xClass;
    }
}
