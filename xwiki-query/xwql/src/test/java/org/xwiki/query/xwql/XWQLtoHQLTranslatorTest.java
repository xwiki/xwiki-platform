package org.xwiki.query.xwql;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.xwiki.query.xwql.QueryContext;
import org.xwiki.query.xwql.hql.Printer;
import org.xwiki.query.xwql.hql.PropertyPrinter;
import org.xwiki.query.xwql.hql.XWQLtoHQLTranslator;

public class XWQLtoHQLTranslatorTest extends TestCase
{
    XWQLtoHQLTranslator translator = new XWQLtoHQLTranslator() {
        @Override
        protected Printer getPrinter(QueryContext context) {
            return new Printer(context, translator) {
                @Override
                public PropertyPrinter getPropertyPrinter() {
                    return new PropertyPrinter() {
                        @Override
                        protected String getPropertyStoreClassName(String clas, String prop, Printer printer) {
                            if ("number".equals(prop)) {
                                return null;
                            } else {
                                return "StringProperty";
                            }
                        }
                    };
                }
            };
        }
    };

    void assertTranslate(String input, String expectedOutput) throws Exception
    {
        String output = translator.translate(input);
        String exp[] = StringUtils.split(expectedOutput, " ");
        String actual[] = StringUtils.split(output, " ");
        for (int i=0; i<Math.max(exp.length, actual.length); i++) {
            String e = i<exp.length ? exp[i] : null;
            String a = i<actual.length ? actual[i] : null;
            if (!StringUtils.equalsIgnoreCase(e, a)) {
                fail(String.format("translate assertion. input = [%s]\n expected output = [%s]\n actual output = [%s]\n first mismatch: [%s]!=[%s]", input, expectedOutput, output, e, a));
            }
        }
    }

    public void testDocument() throws Exception
    {
        assertTranslate("select doc from Document as doc", "select doc from XWikiDocument as doc");
        assertTranslate("select doc from Document as doc where doc.title like '%test'", "select doc from XWikiDocument as doc where doc.title like '%test'");
    }

    public void testObject() throws Exception
    {
        assertTranslate("select doc from Document as doc, doc.object('XWiki.XWikiUsers') as user", 
            "select doc from XWikiDocument as doc , BaseObject as user " +
            "where 1=1 and doc.fullName=user.name and user.className='XWiki.XWikiUsers'");
    }

    public void testProperty() throws Exception
    {
        assertTranslate("select doc from Document as doc, doc.object('XWiki.XWikiUsers') as user where user.email = 'some'", 
            "select doc from XWikiDocument as doc , BaseObject as user , StringProperty as user_email1 " +
            "where ( user_email1.value = 'some' ) and doc.fullName=user.name and user.className='XWiki.XWikiUsers' and user_email1.id.id=user.id and user_email1.id.name='email'");
    }

    public void testShort() throws Exception
    {
        assertTranslate("", "select doc.fullName from XWikiDocument as doc");
        assertTranslate("where doc.title like '%test'", "select doc.fullName from XWikiDocument as doc where doc.title like '%test'");
        assertTranslate("from doc.object('XWiki.XWikiUsers') as user",
            "select doc.fullName from XWikiDocument as doc , BaseObject as user " +
            "where 1=1 and doc.fullName=user.name and user.className='XWiki.XWikiUsers'");
        assertTranslate("from doc.object('XWiki.XWikiUsers') as user where user.email = 'some'", 
            "select doc.fullName from XWikiDocument as doc , BaseObject as user , StringProperty as user_email1 " +
            "where ( user_email1.value = 'some' ) and doc.fullName=user.name and user.className='XWiki.XWikiUsers' and user_email1.id.id=user.id and user_email1.id.name='email'");
    }

    public void testObjDeclInWhere() throws Exception
    {
        assertTranslate("where doc.object('XWiki.XWikiUsers').email = 'some'",
            "select doc.fullName from XWikiDocument as doc , BaseObject as _o1, StringProperty as _o1_email2 " +
            "where ( _o1_email2.value = 'some' ) and doc.fullName=_o1.name and _o1.className='XWiki.XWikiUsers' and _o1_email2.id.id=_o1.id and _o1_email2.id.name='email'");
    }

    public void testOrderBy() throws Exception
    {
        assertTranslate("order by doc.fullName", 
            "select doc.fullName from XWikiDocument as doc order by doc.fullName");
        assertTranslate("from doc.object('XWiki.XWikiUsers') as user order by user.firstname", 
            "select doc.fullName from XWikiDocument as doc , BaseObject as user , StringProperty as user_firstname1 " +
            "where 1=1 and doc.fullName=user.name and user.className='XWiki.XWikiUsers' and user_firstname1.id.id=user.id and user_firstname1.id.name='firstname' order by user_firstname1.value");
    }

    public void testInternalProperty() throws Exception
    {
        assertTranslate("select doc from Document as doc, doc.object('Blog.Categories') as c order by c.number", 
            "select doc from XWikiDocument as doc , BaseObject as c " +
            "where 1=1 and doc.fullName=c.name and c.className='Blog.Categories' order by c.number");
    }
}
