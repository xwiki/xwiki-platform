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
package org.xwiki.uiextension;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.velocity.VelocityContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.uiextension.internal.WikiUIExtension;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * Unit tests for {@link WikiUIExtension}.
 *
 * @version $Id$
 * @since 4.2M3
 */
public class WikiUIExtensionTest
{
    private static final DocumentReference CLASS_REF = new DocumentReference("xwiki", "XWiki", "UIExtensionClass");

    private static final DocumentReference DOC_REF = new DocumentReference("xwiki", "XWiki", "MyUIExtension");

    private ObjectReference objectReference;

    private WikiUIExtension wikiUIExtension;

    private VelocityManager velocityManager;

    private Transformation transformation;

    private XDOM xdom;

    private Mockery mockery = new JUnit4Mockery();

    private Mockery getMockery()
    {
        return mockery;
    }

    @Before
    public void configure() throws Exception
    {
        final ComponentManager componentManager = getMockery().mock(ComponentManager.class);
        transformation = getMockery().mock(Transformation.class, "macro");
        final Execution execution = getMockery().mock(Execution.class);
        velocityManager = getMockery().mock(VelocityManager.class);
        objectReference = new ObjectReference(CLASS_REF.toString() + "[1]", DOC_REF);
        final EntityReferenceSerializer<String> serializer = getMockery().mock(EntityReferenceSerializer.class);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentManager).getInstance(Transformation.class, "macro");
                will(returnValue(transformation));
                oneOf(componentManager).getInstance(Execution.class);
                will(returnValue(execution));
                oneOf(componentManager).getInstance(VelocityManager.class);
                will(returnValue(velocityManager));
                oneOf(componentManager).getInstance(EntityReferenceSerializer.TYPE_STRING);
                will(returnValue(serializer));
                oneOf(serializer).serialize(objectReference);
                will(returnValue("xwiki:XWiki.MyUIExtension^xwiki:XWiki.UIExtensionClass[1]"));
            }
        });

        xdom = new XDOM(new ArrayList<Block>());
        Map<String, String> parameters = new HashMap();
        parameters.put("key", "value=foo");

        wikiUIExtension = new WikiUIExtension(objectReference, "name", "epId", xdom, Syntax.XWIKI_2_1, parameters,
            componentManager);
    }

    @Test
    public void getXDOMWhenTransformationFails() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                // it should fail silently
                oneOf(transformation).transform(with(any(XDOM.class)), with(any(TransformationContext.class)));
                will(throwException(new TransformationException("")));
            }
        });

        wikiUIExtension.execute();
    }

    @Test
    public void getDOM() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(transformation).transform(with(any(XDOM.class)), with(any(TransformationContext.class)));
            }
        });

        Assert.assertEquals("name", wikiUIExtension.getId());
        Assert.assertEquals("epId", wikiUIExtension.getExtensionPointId());
        Assert.assertEquals(MapUtils.EMPTY_MAP, wikiUIExtension.getHandledMethods());
        Assert.assertEquals(ListUtils.EMPTY_LIST, wikiUIExtension.getImplementedInterfaces());
        Assert.assertEquals(DOC_REF, wikiUIExtension.getDocumentReference());
        Assert.assertEquals(UIExtension.class, wikiUIExtension.getRoleType());
        Assert.assertEquals("xwiki:XWiki.MyUIExtension^xwiki:XWiki.UIExtensionClass[1]",
            wikiUIExtension.getRoleHint());
        wikiUIExtension.execute();
    }

    @Test
    public void getParametersWithAnEqualSignInAValue() throws Exception
    {
        final VelocityEngine velocityEngine = getMockery().mock(VelocityEngine.class);
        final VelocityContext velocityContext = new VelocityContext();

        getMockery().checking(new Expectations()
        {
            {
                oneOf(velocityManager).getVelocityEngine();
                will(returnValue(velocityEngine));
                oneOf(velocityManager).getVelocityContext();
                will(returnValue(velocityContext));
                oneOf(velocityEngine).evaluate(with(any(VelocityContext.class)), with(any(StringWriter.class)),
                    with(equal("")), with(equal("value=foo")));
                will(returnValue(true));
            }
        });

        Assert.assertEquals("", wikiUIExtension.getParameters().get("key"));
    }

    @Test
    public void getParametersWhenVelocityFails() throws Exception
    {
        final VelocityEngine velocityEngine = getMockery().mock(VelocityEngine.class);
        final VelocityContext velocityContext = new VelocityContext();

        getMockery().checking(new Expectations()
        {
            {
                oneOf(velocityManager).getVelocityEngine();
                will(returnValue(velocityEngine));
                oneOf(velocityManager).getVelocityContext();
                will(returnValue(velocityContext));
                // It should fail silently
                oneOf(velocityEngine).evaluate(with(any(VelocityContext.class)), with(any(StringWriter.class)),
                    with(equal("")), with(equal("value=foo")));
                will(throwException(new XWikiVelocityException("")));
            }
        });

        Assert.assertEquals(0, wikiUIExtension.getParameters().size());
    }
}
