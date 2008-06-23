package org.xwiki.platform.patchservice;

import java.util.Date;

import org.jmock.Mock;
import org.xwiki.platform.patchservice.hook.PatchCreator;
import org.xwiki.platform.patchservice.impl.PatchImpl;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.test.AbstractXWikiComponentTestCase;

public class HookTest extends AbstractXWikiComponentTestCase
{
    XWikiContext context;

    private Mock mockXWiki;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.context = new XWikiContext();
        this.mockXWiki =
            mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class}, new Object[] {new XWikiConfig(),
            this.context});
        BaseClass bclass = new BaseClass();
        bclass.addTextField("changedProperty", "Changed Property", 20);
        bclass.addTextField("changedProperty2", "Changed Property 2", 20);
        bclass.addTextField("addedProperty", "Added Property", 20);
        bclass.addTextAreaField("textarea", "Textarea property", 60, 10);
        bclass.addNumberField("number", "Number Property", 10, "integer");
        this.mockXWiki.stubs().method("getClass").with(eq("XWiki.SomeClass"), eq(this.context)).will(
            returnValue(bclass));
        this.mockXWiki.stubs().method("getClass").with(eq(""), eq(this.context)).will(returnValue(new BaseClass()));
        this.context.setWiki((XWiki) this.mockXWiki.proxy());
    }

    public void testChangingOneLineWithNoEndlines() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setContent("First line");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setContent("First long line");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getContent(), origDoc.getContent());
    }

    public void testChangingOneLineWithEndlineBefore() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setContent("First line\n");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setContent("First long line");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getContent(), origDoc.getContent());
    }

    public void testChangingOneLineWithEndlineAfter() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setContent("First line");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setContent("First long line\n");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getContent(), origDoc.getContent());
    }

    public void testChangingOneLineWithBothEndlines() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setContent("First line\n");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setContent("First long line\n");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getContent(), origDoc.getContent());
    }

    public void testRemovingNewLine() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setContent("First line\n");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setContent("First line");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getContent(), origDoc.getContent());
    }

    public void testAddingNewLine() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setContent("First line");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setContent("First line\n");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getContent(), origDoc.getContent());
    }

    public void testDocNoChanges() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setContent("First line");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setContent("First line");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getContent(), origDoc.getContent());
    }

    public void testAddingManyEndlines() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setContent("First line");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setContent("First line\n\n\n");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getContent(), origDoc.getContent());
    }

    public void testSeveralContentChanges() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setContent("First line\nSecond line\n\nFourth line\nFifth line\nSixth line\n" + "Seventh line");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setContent("First long line\nSecond line\nThird line\nFifth line\nSixths line\n"
            + "Seventh line\n\n\n\n   Eight line   ");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getContent(), origDoc.getContent());
    }

    public void testRemovingAllContent() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setContent("First line\n");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setContent("");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getContent(), origDoc.getContent());
    }

    public void testRemovingAllContentExceptNewLine() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setContent("First line\n");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setContent("\n");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getContent(), origDoc.getContent());
    }

    public void testReplacingContentWithNewLine() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setContent("First line");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setContent("\n");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getContent(), origDoc.getContent());
    }

    public void testPropertyChanges() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.setCreator("XWiki.User");
        origDoc.setCreationDate(new Date());
        origDoc.setDate(origDoc.getCreationDate());
        origDoc.setAuthor("XWiki.User");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setParent("XWiki.ParentDocument");
        newDoc.setAuthor("XWiki.AnotherUser");
        newDoc.setTranslation(1);
        newDoc.setDate(new Date(origDoc.getDate().getTime() + 60000));
        newDoc.setLanguage("fr");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertEquals(newDoc.getAuthor(), origDoc.getAuthor());
        assertEquals(newDoc.getCreator(), origDoc.getCreator());
        assertEquals(newDoc.getParent(), origDoc.getParent());
        assertEquals(newDoc.getTranslation(), origDoc.getTranslation());
        assertEquals(newDoc.getLanguage(), origDoc.getLanguage());
        assertEquals(newDoc.getDate(), origDoc.getDate());
    }

    public void testClassChanges() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.getxWikiClass().addTextField("fieldToKeep", "This field will be unchanged", 20);
        origDoc.getxWikiClass().addTextField("fieldToRemove", "This field will be removed", 20);
        origDoc.getxWikiClass().addBooleanField("fieldToChange", "This field will be changed", "truefalse");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.getxWikiClass().removeField("fieldToRemove");
        newDoc.getxWikiClass().addBooleanField("addedField", "This field was added", "yesno");
        ((BooleanClass) newDoc.getxWikiClass().getField("fieldToChange")).setPrettyName("This field was changed");
        ((BooleanClass) newDoc.getxWikiClass().getField("fieldToChange")).setDisplayType("yesno");
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, null);
        p.apply(origDoc, null);
        assertNull(origDoc.getxWikiClass().getField("fieldToRemove"));
    }

    public void testObjectChanges() throws Exception
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
        origDoc.newObject("XWiki.SomeClass", this.context);
        origDoc.setStringValue("XWiki.SomeClass", "changedProperty", "old value");
        origDoc.setStringValue("XWiki.SomeClass", "changedProperty2", "old value");
        BaseObject obj = origDoc.newObject("XWiki.SomeClass", this.context);
        obj.setStringValue("changedProperty", "value");
        XWikiDocument newDoc = (XWikiDocument) origDoc.clone();
        newDoc.setStringValue("XWiki.SomeClass", "changedProperty", "new value");
        newDoc.setStringValue("XWiki.SomeClass", "changedProperty2", "new value");
        newDoc.setStringValue("XWiki.SomeClass", "addedProperty", "added value");
        obj = newDoc.newObject("XWiki.SomeClass", this.context);
        obj.setStringValue("addedProperty", "added value in added object");
        newDoc.removeObject(newDoc.getObject("XWiki.SomeClass", 1));
        PatchImpl p = (PatchImpl) new PatchCreator().getPatch(origDoc, newDoc, this.context);

        p.apply(origDoc, this.context);
        assertEquals(newDoc.getObject("XWiki.SomeClass", 0).displayView("changedProperty", this.context), origDoc
            .getObject("XWiki.SomeClass", 0).displayView("changedProperty", this.context));
        assertEquals(newDoc.getObject("XWiki.SomeClass", 0).displayView("changedProperty2", this.context), origDoc
            .getObject("XWiki.SomeClass", 0).displayView("changedProperty2", this.context));
        assertEquals(newDoc.getObject("XWiki.SomeClass", 0).displayView("addedProperty", this.context), origDoc
            .getObject("XWiki.SomeClass", 0).displayView("addedProperty", this.context));
        assertEquals(newDoc.getObject("XWiki.SomeClass", 2).displayView("addedProperty", this.context), origDoc
            .getObject("XWiki.SomeClass", 2).displayView("addedProperty", this.context));
        assertEquals(null, origDoc.getObject("XWiki.SomeClass", 1));
    }

    public void testAttachmentChanges() throws XWikiException
    {
        XWikiDocument origDoc = new XWikiDocument("XWiki", "Document");
    }
}
