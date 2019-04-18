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
package org.xwiki.annotation;

import java.io.IOException;
import java.util.Collection;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.xwiki.annotation.io.IOService;
import org.xwiki.annotation.io.IOServiceException;
import org.xwiki.annotation.io.IOTargetService;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;

/**
 * Mock setup for the annotations tests, mocking the {@link IOService} and {@link IOTargetService} to provide documents
 * functions for the data in the test files.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class AnnotationsMockSetup
{
    /**
     * Mockery to setup IO services in this test, setup as a JUnit4 mockery so that tests fail when expectations are not
     * met so that we test components through invocation expectations.
     */
    protected Mockery mockery = new JUnit4Mockery();

    /**
     * IOTargetService used by this test.
     */
    protected IOTargetService ioTargetService;

    /**
     * IOService used in this test.
     */
    protected IOService ioService;

    /**
     * The document factory used to load documents from the test files.
     */
    protected TestDocumentFactory docFactory;

    /**
     * Builds an annotation mock setup registering mocked {@link IOService} and {@link IOTargetService} to manipulate
     * documents from the test description files.
     *
     * @param componentManager the component manager to register the services with
     * @param docFactory the document factory used to load documents from the test files
     * @throws ComponentRepositoryException if the components cannot be registered
     */
    public AnnotationsMockSetup(ComponentManager componentManager, TestDocumentFactory docFactory)
        throws ComponentRepositoryException
    {
        // IOTargetService mockup
        ioTargetService = mockery.mock(IOTargetService.class);
        DefaultComponentDescriptor<IOTargetService> iotsDesc = new DefaultComponentDescriptor<IOTargetService>();
        iotsDesc.setRoleType(IOTargetService.class);
        componentManager.registerComponent(iotsDesc, ioTargetService);

        // IOService mockup
        ioService = mockery.mock(IOService.class);
        DefaultComponentDescriptor<IOService> ioDesc = new DefaultComponentDescriptor<IOService>();
        ioDesc.setRoleType(IOService.class);
        componentManager.registerComponent(ioDesc, ioService);

        this.docFactory = docFactory;
    }

    /**
     * Sets up the expectations for the {@link IOService} and {@link IOTargetService} to return correctly the values in
     * the test files for {@code docName}. Call this function when operating with mocked documents to provide all the
     * information in the test file (document source, rendered contents, annotations).
     *
     * @param docName the name of the document to setup expectations for
     * @throws IOServiceException if something wrong happens while mocking the documents access
     * @throws IOException if something wrong happens while mocking the documents access
     */
    public void setupExpectations(final String docName) throws IOServiceException, IOException
    {
        mockery.checking(new Expectations()
        {
            {
                MockDocument mDoc = docFactory.getDocument(docName);

                allowing(ioService).getAnnotations(with(docName));
                will(returnValue(mDoc.getAnnotations()));

                allowing(ioService).updateAnnotations(with(docName), with(any(Collection.class)));
                // update the list of document annotations
                will(new Action()
                {
                    @Override
                    public void describeTo(Description description)
                    {
                        description.appendText("Updates the annotations");
                    }

                    @Override
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        String documentName = (String) invocation.getParameter(0);
                        MockDocument document = docFactory.getDocument(documentName);
                        Collection<Annotation> annList = (Collection<Annotation>) invocation.getParameter(1);
                        for (Annotation ann : annList) {
                            Annotation toUpdate = getAnnotation(ann.getId(), document.getAnnotations());
                            // remove toUpdate and add ann
                            if (toUpdate != null) {
                                document.getAnnotations().remove(toUpdate);
                            }
                            document.getAnnotations().add(ann);
                        }
                        return null;
                    }

                    private Annotation getAnnotation(String annId, Collection<Annotation> list)
                    {
                        for (Annotation ann : list) {
                            if (ann.getId().equals(annId)) {
                                return ann;
                            }
                        }

                        return null;
                    }
                });

                allowing(ioTargetService).getSource(with(docName));
                will(returnValue(mDoc.getSource()));

                allowing(ioTargetService).getSourceSyntax(with(docName));
                will(returnValue(mDoc.getSyntax()));
            }
        });
    }

    /**
     * @return the mockery
     */
    public Mockery getMockery()
    {
        return mockery;
    }

    /**
     * @return the ioTargetService
     */
    public IOTargetService getIoTargetService()
    {
        return ioTargetService;
    }

    /**
     * @return the ioService
     */
    public IOService getIoService()
    {
        return ioService;
    }

    /**
     * @return the docFactory
     */
    public TestDocumentFactory getDocFactory()
    {
        return docFactory;
    }
}
