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
package org.xwiki.query.xwql.internal;

import static org.junit.Assert.fail;

import org.apache.commons.lang3.StringUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.query.xwql.internal.hql.XWQLtoHQLTranslator;

@RunWith(JMock.class)
public class XWQLtoHQLTranslatorTest
{
    private Mockery context = new JUnit4Mockery();

    private DocumentAccessBridge dab = context.mock(DocumentAccessBridge.class);

    private XWQLtoHQLTranslator translator = new XWQLtoHQLTranslator()
    {
        @Override
        public DocumentAccessBridge getDocumentAccessBridge()
        {
            return dab;
        }
    };

    @Before
    public void setUp() throws Exception
    {
        context.checking(new Expectations()
        {{
                allowing(dab).getPropertyType(with(any(String.class)), with(equal("number")));
                will(returnValue(null));

                allowing(dab).getPropertyType(with(any(String.class)), with(equal("category")));
                will(returnValue("DBStringListProperty"));

                allowing(dab).getPropertyType(with(any(String.class)), with(equal("stringlist")));
                will(returnValue("StringListProperty"));

                allowing(dab).getPropertyType(with(any(String.class)), with(any(String.class)));
                will(returnValue("StringProperty"));

                allowing(dab).isPropertyCustomMapped("Custom.Mapping", "cmprop");
                will(returnValue(true));

                allowing(dab).isPropertyCustomMapped(with(any(String.class)), with(any(String.class)));
                will(returnValue(false));
            }});
    }

    void assertTranslate(String input, String expectedOutput) throws Exception
    {
        String output = translator.translate(input);
        String exp[] = StringUtils.split(expectedOutput, " ");
        String actual[] = StringUtils.split(output, " ");
        for (int i = 0; i < Math.max(exp.length, actual.length); i++) {
            String e = i < exp.length ? exp[i] : null;
            String a = i < actual.length ? actual[i] : null;
            if (!StringUtils.equalsIgnoreCase(e, a)) {
                fail(String.format(
                    "translate assertion. input = [%s]\n expected output = [%s]\n actual output = [%s]\n first mismatch: [%s]!=[%s]",
                    input, expectedOutput, output, e, a));
            }
        }
    }

    @Test
    public void testDocument() throws Exception
    {
        assertTranslate("select doc from Document as doc", "select doc from XWikiDocument as doc");
        assertTranslate("select doc from Document as doc where doc.title like '%test'",
            "select doc from XWikiDocument as doc where doc.title like '%test'");
    }

    @Test
    public void testObject() throws Exception
    {
        assertTranslate("select doc from Document as doc, doc.object('XWiki.XWikiUsers') as user",
            "select doc from XWikiDocument as doc , BaseObject as user " +
                "where 1=1 and doc.fullName=user.name and user.className='XWiki.XWikiUsers'");
    }

    @Test
    public void testProperty() throws Exception
    {
        assertTranslate(
            "select doc from Document as doc, doc.object('XWiki.XWikiUsers') as user where user.email = 'some'",
            "select doc from XWikiDocument as doc , BaseObject as user , StringProperty as user_email1 " +
                "where ( user_email1.value = 'some' ) and doc.fullName=user.name and user.className='XWiki.XWikiUsers' and user_email1.id.id=user.id and user_email1.id.name='email'");
    }

    @Test
    public void testShort() throws Exception
    {
        assertTranslate("", "select doc.fullName from XWikiDocument as doc");
        assertTranslate("where doc.title like '%test'",
            "select doc.fullName from XWikiDocument as doc where doc.title like '%test'");
        assertTranslate("from doc.object('XWiki.XWikiUsers') as user",
            "select doc.fullName from XWikiDocument as doc , BaseObject as user " +
                "where 1=1 and doc.fullName=user.name and user.className='XWiki.XWikiUsers'");
        assertTranslate("from doc.object('XWiki.XWikiUsers') as user where user.email = 'some'",
            "select doc.fullName from XWikiDocument as doc , BaseObject as user , StringProperty as user_email1 " +
                "where ( user_email1.value = 'some' ) and doc.fullName=user.name and user.className='XWiki.XWikiUsers' and user_email1.id.id=user.id and user_email1.id.name='email'");
    }

    @Test
    public void testObjDeclInWhere() throws Exception
    {
        assertTranslate("where doc.object('XWiki.XWikiUsers').email = 'some'",
            "select doc.fullName from XWikiDocument as doc , BaseObject as _o1, StringProperty as _o1_email2 " +
                "where ( _o1_email2.value = 'some' ) and doc.fullName=_o1.name and _o1.className='XWiki.XWikiUsers' and _o1_email2.id.id=_o1.id and _o1_email2.id.name='email'");
    }

    @Test
    public void testObjDeclInWhereWithTwoInstances() throws Exception
    {
        assertTranslate(
            "where doc.object(XWiki.XWikiUsers).email = 'some' and doc.object(XWiki.XWikiUsers).first_name = 'Name'",
            "select doc.fullName from XWikiDocument as doc , BaseObject as _o1, " +
            "StringProperty as _o1_email2, StringProperty as _o1_first_name3 " +
            "where ( _o1_email2.value = 'some' and _o1_first_name3.value = 'Name' ) " +
            "and doc.fullName=_o1.name and _o1.className='XWiki.XWikiUsers' " +
            "and _o1_email2.id.id=_o1.id and _o1_email2.id.name='email' " +
            "and _o1_first_name3.id.id=_o1.id and _o1_first_name3.id.name='first_name'");
    }

    @Test
    public void testOrderBy() throws Exception
    {
        assertTranslate("order by doc.fullName",
            "select doc.fullName from XWikiDocument as doc order by doc.fullName");
        assertTranslate("from doc.object('XWiki.XWikiUsers') as user order by user.firstname",
            "select doc.fullName from XWikiDocument as doc , BaseObject as user , StringProperty as user_firstname1 " +
                "where 1=1 and doc.fullName=user.name and user.className='XWiki.XWikiUsers' and user_firstname1.id.id=user.id and user_firstname1.id.name='firstname' order by user_firstname1.value");
    }

    @Test
    public void testInternalProperty() throws Exception
    {
        assertTranslate("select doc from Document as doc, doc.object('Blog.Categories') as c order by c.number",
            "select doc from XWikiDocument as doc , BaseObject as c " +
                "where 1=1 and doc.fullName=c.name and c.className='Blog.Categories' order by c.number");
    }

    @Test
    public void testLists() throws Exception
    {
        // DBStringListProperty
        assertTranslate("from doc.object('XWiki.ArticleClass') as a where :cat member of a.category",
            "select doc.fullName from XWikiDocument as doc , BaseObject as a , DBStringListProperty as a_category1" +
                " where ( :cat in elements( a_category1.list ) ) and doc.fullName=a.name and a.className='XWiki.ArticleClass' and a_category1.id.id=a.id and a_category1.id.name='category'");
        // StringListProperty
        assertTranslate("from doc.object('XWiki.Class') as c where c.stringlist like '%some%'",
            "select doc.fullName from XWikiDocument as doc , BaseObject as c , StringListProperty as c_stringlist1" +
                " where ( c_stringlist1.textValue like '%some%' ) and doc.fullName=c.name and c.className='XWiki.Class' and c_stringlist1.id.id=c.id and c_stringlist1.id.name='stringlist'");
        // return DBStringListProperty
        assertTranslate("select distinct a.category from Document as doc, doc.object('XWiki.ArticleClass') as a",
            "select distinct elements(a_category1.list) from XWikiDocument as doc , BaseObject as a , DBStringListProperty as a_category1" +
                " where 1=1 and doc.fullName=a.name and a.className='XWiki.ArticleClass' and a_category1.id.id=a.id and a_category1.id.name='category'");
    }

    @Test
    public void testCustomMapping() throws Exception
    {
        // one CM prop
        assertTranslate("select doc from Document as doc, doc.object('Custom.Mapping') as c where c.cmprop='some'",
            "select doc from XWikiDocument as doc , BaseObject as c , Custom.Mapping as cCM1 " +
                "where ( cCM1.cmprop = 'some' ) and doc.fullName=c.name and c.id=cCM1.id");
        // CM and standard props 
        assertTranslate(
            "select doc from Document as doc, doc.object('Custom.Mapping') as c where c.cmprop='some' and c.prop=1",
            "select doc from XWikiDocument as doc , BaseObject as c , Custom.Mapping as cCM1, StringProperty as c_prop2 " +
                "where ( cCM1.cmprop = 'some' and c_prop2.value = 1 ) and doc.fullName=c.name and c.id=cCM1.id and c_prop2.id.id=c.id and c_prop2.id.name='prop'");
    }
}
