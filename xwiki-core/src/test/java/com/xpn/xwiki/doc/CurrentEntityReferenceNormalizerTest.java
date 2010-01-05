package com.xpn.xwiki.doc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;
import org.junit.Assert;
import org.junit.Before;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceNormalizer;
import org.xwiki.test.AbstractComponentTestCase;

public class CurrentEntityReferenceNormalizerTest extends AbstractComponentTestCase
{
    private static final String CURRENT_WIKI = "currentwiki";

    private static final String CURRENT_SPACE = "currentspace";

    private static final String CURRENT_PAGE = "currentpage";

    private EntityReferenceNormalizer normalizer;

    private XWikiContext context;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.context = new XWikiContext();

        Execution execution = getComponentManager().lookup(Execution.class);
        execution.getContext().setProperty("xwikicontext", this.context);
        Utils.setComponentManager(getComponentManager());

        this.normalizer = getComponentManager().lookup(EntityReferenceNormalizer.class, "current");
    }

    @org.junit.Test
    public void testNormalizeAttachmentReferenceWhenMissingParentsAndNoContextDocument()
    {
        EntityReference reference = new EntityReference("filename", EntityType.ATTACHMENT);
        normalizer.normalize(reference);
        Assert.assertEquals("WebHome", reference.getParent().getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        Assert.assertEquals("Main", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        Assert.assertEquals("xwiki", reference.getParent().getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }

    @org.junit.Test
    public void testNormalizeAttachmentReferenceWhenMissingParentsAndContextDocument()
    {
        this.context.setDoc(new XWikiDocument(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE));

        EntityReference reference = new EntityReference("filename", EntityType.ATTACHMENT);
        normalizer.normalize(reference);
        Assert.assertEquals(CURRENT_PAGE, reference.getParent().getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        Assert.assertEquals(CURRENT_SPACE, reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        Assert.assertEquals(CURRENT_WIKI, reference.getParent().getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }
}
