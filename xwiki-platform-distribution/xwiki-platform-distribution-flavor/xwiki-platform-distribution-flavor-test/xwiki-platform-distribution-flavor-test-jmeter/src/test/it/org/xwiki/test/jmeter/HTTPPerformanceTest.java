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
package org.xwiki.test.jmeter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarException;
import org.xwiki.xar.XarPackage;

public class HTTPPerformanceTest
{
    private static final DefaultStringEntityReferenceSerializer SERIALIZER =
        new DefaultStringEntityReferenceSerializer(new DefaultSymbolScheme());

    protected static List<DocumentReference> readXarContents(String fileName, String patternFilter) throws Exception
    {
        Collection<XarEntry> entries = XarPackage.getEntries(new File(fileName));

        List<DocumentReference> result = new ArrayList<DocumentReference>(entries.size());

        WikiReference wikiReference = new WikiReference("xwiki");

        for (XarEntry entry : entries) {
            result.add(new DocumentReference(entry, wikiReference));
        }

        return result;
    }

    private static void addXarFiles(List<HTTPSampler> samplers) throws UnsupportedEncodingException, XarException,
        IOException
    {
        String path = System.getProperty("pathToDocuments");
        String patternFilter = System.getProperty("documentsToTest");

        Pattern pattern = patternFilter == null ? null : Pattern.compile(patternFilter);

        for (XarEntry xarEntry : XarPackage.getEntries(new File(path))) {
            if (pattern == null || pattern.matcher(SERIALIZER.serialize(xarEntry)).matches()) {
                samplers.add(createSample(xarEntry, "get"));
                samplers.add(createSample(xarEntry, "view"));
            }
        }
    }

    private static HTTPSampler createSample(LocalDocumentReference documentReference, String action)
        throws UnsupportedEncodingException
    {
        return createSample(SERIALIZER.serialize(documentReference) + " (" + action + ")",
            "/xwiki/bin/" + action + "/" + URLEncoder.encode(documentReference.getParent().getName(), "UTF8") + "/"
                + URLEncoder.encode(documentReference.getName(), "UTF8"));
    }

    @BeforeClass
    public static void before() throws IOException
    {
        FileUtils.writeByteArrayToFile(new File("target/jmeter/home/bin/httpclient.parameters"),
            IOUtils.toByteArray(HTTPPerformanceTest.class.getResource("/jmeterbin/httpclient.parameters")));
        FileUtils.writeByteArrayToFile(new File("target/jmeter/home/bin/jmeter.properties"),
            IOUtils.toByteArray(HTTPPerformanceTest.class.getResource("/jmeterbin/jmeter.properties")));
        FileUtils.writeByteArrayToFile(new File("target/jmeter/home/bin/saveservice.properties"),
            IOUtils.toByteArray(HTTPPerformanceTest.class.getResource("/jmeterbin/saveservice.properties")));
        FileUtils.writeByteArrayToFile(new File("target/jmeter/home/bin/upgrade.properties"),
            IOUtils.toByteArray(HTTPPerformanceTest.class.getResource("/jmeterbin/upgrade.properties")));
    }

    private static HTTPSampler createSample(String name, String path)
    {
        HTTPSampler httpSampler = new HTTPSampler();

        httpSampler.setDomain("localhost");
        httpSampler.setPort(Integer.valueOf(XWikiExecutor.DEFAULT_PORT));
        httpSampler.setMethod("GET");

        httpSampler.setName(path);
        httpSampler.setPath(path);

        return httpSampler;
    }

    public void execute(List<HTTPSampler> samplers)
    {
        execute(samplers, null, null);
    }

    public void execute(List<HTTPSampler> samplers, String user, String password)
    {
        // jmeter.properties
        JMeterUtils.loadJMeterProperties("target/jmeter/home/bin/saveservice.properties");
        JMeterUtils.setLocale(Locale.ENGLISH);
        JMeterUtils.setJMeterHome("target/jmeter/home");

        // Result collector
        ResultCollector resultCollector = new ResultCollector();
        resultCollector.setFilename("target/jmeter/report.jtl");
        SampleSaveConfiguration saveConfiguration = new SampleSaveConfiguration();
        saveConfiguration.setAsXml(true);
        saveConfiguration.setCode(true);
        saveConfiguration.setLatency(true);
        saveConfiguration.setTime(true);
        saveConfiguration.setTimestamp(true);
        resultCollector.setSaveConfig(saveConfiguration);

        // Thread Group
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("xwiki");
        threadGroup.setNumThreads(1);
        threadGroup.setRampUp(1);
        LoopController loopCtrl = new LoopController();
        loopCtrl.setLoops(5);
        loopCtrl.setFirst(true);
        threadGroup.setSamplerController(loopCtrl);

        HashTree threadGroupTree = new HashTree();
        threadGroupTree.add(samplers);

        // Test plan
        TestPlan testPlan = new TestPlan("ping");

        HashTree testPlanTree = new HashTree();
        testPlanTree.add(threadGroup, threadGroupTree);
        testPlanTree.add(resultCollector);

        HashTree hashTree = new HashTree();
        hashTree.add(testPlan, testPlanTree);

        // Engine
        StandardJMeterEngine jm = new StandardJMeterEngine("localhost");

        jm.configure(hashTree);

        jm.run();
    }

    // Tests

    @Test
    public void guest() throws FileNotFoundException, Exception
    {
        List<HTTPSampler> samplers = new ArrayList<HTTPSampler>();

        samplers.add(createSample("root", "/xwiki/"));
        samplers.add(createSample("Main.WebHome (edit)", "/xwiki/bin/edit/Main/WebHome"));

        addXarFiles(samplers);

        execute(samplers);
    }
}
