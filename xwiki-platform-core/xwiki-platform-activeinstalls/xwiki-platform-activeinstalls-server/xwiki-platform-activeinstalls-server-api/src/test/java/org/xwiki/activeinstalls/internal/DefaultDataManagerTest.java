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
package org.xwiki.activeinstalls.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.*;
import org.xwiki.activeinstalls.ActiveInstallsConfiguration;
import org.xwiki.activeinstalls.internal.server.DefaultDataManager;
import org.xwiki.activeinstalls.server.DataManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.github.tlrx.elasticsearch.test.EsSetup;

import net.sf.json.JSONObject;

import static com.github.tlrx.elasticsearch.test.EsSetup.deleteAll;
import static com.github.tlrx.elasticsearch.test.EsSetup.index;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Integration tests for {@link org.xwiki.activeinstalls.internal.server.DefaultDataManager}.
 *
 * @version $Id$
 * @since 5.2RC1
 */
@ComponentList({
    DefaultDataManager.class,
    DefaultJestClientManager.class
})
public class DefaultDataManagerTest
{
    private static final DateTimeFormatter DATE_FORMATTER = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    public static EsSetup esSetup;

    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    @BeforeClass
    public static void setUpClass() throws Exception
    {
        esSetup = new EsSetup();
        esSetup.execute(deleteAll());
    }

    @Before
    public void setUp() throws Exception
    {
        // Add index 1 (non snapshot version)
        addIndex("id1", DATE_FORMATTER.print(new Date().getTime()), "5.2-milestone-2");

        // Add index 2 (date is one year earlier than index1 and version is SNAPSHOT)
        DateTime dt = new DateTime();
        dt = dt.plusDays(-365);
        addIndex("id2", DATE_FORMATTER.print(dt), "5.2-SNAPSHOT");

        // Add index 3 (version is SNAPSHOT)
        addIndex("id3", DATE_FORMATTER.print(new Date().getTime()), "5.3-SNAPSHOT");

        ActiveInstallsConfiguration configuration =
            this.componentManager.registerMockComponent(ActiveInstallsConfiguration.class);
        when(configuration.getPingInstanceURL()).thenReturn("http://localhost:9200");
    }

    @AfterClass
    public static void tearDownClass()
    {
        esSetup.terminate();
    }

    @Test
    public void getInstallCountMatchingAll() throws Exception
    {
        DataManager manager = this.componentManager.getInstance(DataManager.class);
        String query = "*";
        assertEquals(3, manager.getInstallCount(query));
    }

    @Test
    public void getInstallCountMatchingDate() throws Exception
    {
        DataManager manager = this.componentManager.getInstance(DataManager.class);
        DateTime dt = new DateTime();
        dt = dt.plusDays(-30);
        String query = String.format("date:[%s TO *]",  DATE_FORMATTER.print(dt));
        assertEquals(2, manager.getInstallCount(query));
    }

    @Test
    public void getInstallCountMatchingDateAndVersion() throws Exception
    {
        DataManager manager = this.componentManager.getInstance(DataManager.class);
        DateTime dt = new DateTime();
        dt = dt.plusDays(-30);
        String query = String.format(
            "date:[%s TO *] AND distributionVersion:5.2-milestone-2", DATE_FORMATTER.print(dt));
        assertEquals(1, manager.getInstallCount(query));

    }

    private void addIndex(String id, String date, String version)
    {
        Map jsonMap = new HashMap();
        jsonMap.put("date", date);
        jsonMap.put("distributionVersion", version);
        String json = JSONObject.fromObject(jsonMap).toString();
        esSetup.execute(index("installs", "install", id).withSource(json));
    }
}
