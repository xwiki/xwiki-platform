package com.xpn.xwiki.plugin.charts.tests;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class AllTests extends TestSuite {
    
    public AllTests() {
        addTest(new TestSuite(ObjectsTest.class));
        addTest(new TestSuite(RadeoxHelperTest.class));
        addTest(new TestSuite(RadeoxHelperBug.class));
        addTest(new TestSuite(DefaultDataSourceTest.class));
        addTest(new TestSuite(TableDataSourceTest.class));
        addTest(new TestSuite(DataSourceFactoryTest.class));
        addTest(new TestSuite(ChartParamsTest.class));
    }
     
    public static void main(String[] args) {
        TestRunner.run(new AllTests());
    }
}
