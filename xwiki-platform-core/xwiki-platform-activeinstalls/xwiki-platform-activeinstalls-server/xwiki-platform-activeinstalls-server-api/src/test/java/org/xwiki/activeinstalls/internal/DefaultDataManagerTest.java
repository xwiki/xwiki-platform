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

import java.util.Collections;
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
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.github.tlrx.elasticsearch.test.EsSetup;
import com.google.gson.JsonObject;

import io.searchbox.params.Parameters;
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
    DefaultJestClientManager.class,
    DefaultConverterManager.class,
    ConvertUtilsConverter.class,
    EnumConverter.class,
    ContextComponentManagerProvider.class
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
        addIndex("id1", DATE_FORMATTER.print(new Date().getTime()), "5.2");

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
    public void searchInstallsMatchingAll() throws Exception
    {
        DataManager manager = this.componentManager.getInstance(DataManager.class);
        String query = "{ \"query\" : { \"match_all\": { } } }";
        JsonObject json = manager.searchInstalls("install", query, Collections.<String, Object>emptyMap());
        assertEquals(3, json.getAsJsonObject("hits").getAsJsonPrimitive("total").getAsLong());
    }

    @Test
    public void searchInstallsMatchingVersion() throws Exception
    {
        DataManager manager = this.componentManager.getInstance(DataManager.class);
        String query = "{ \"query\" : { \"term\": { \"distributionVersion\" : \"5.2\" } } }";
        JsonObject json = manager.searchInstalls("install", query, Collections.<String, Object>emptyMap());
        assertEquals(2, json.getAsJsonObject("hits").getAsJsonPrimitive("total").getAsLong());
    }

    @Test
    public void searchInstallsMatchingVersionAndDate() throws Exception
    {
        DataManager manager = this.componentManager.getInstance(DataManager.class);
        DateTime dt = new DateTime();
        dt = dt.plusDays(-30);
        String query = String.format("{ \"query\" : { \"query_string\": { \"query\" : \""
            + "date:[%s TO *] AND distributionVersion:5.2\" } } }", DATE_FORMATTER.print(dt));
        JsonObject json = manager.searchInstalls("install", query, Collections.<String, Object>emptyMap());
        assertEquals(1, json.getAsJsonObject("hits").getAsJsonPrimitive("total").getAsLong());
        assertEquals("5.2", json.getAsJsonObject("hits").getAsJsonArray("hits").get(0).getAsJsonObject()
            .getAsJsonObject("_source").getAsJsonPrimitive("distributionVersion").getAsString());
    }

    @Test
    public void searchInstallsExcludingSnapshotsAndUsingCountSearchType() throws Exception
    {
        DataManager manager = this.componentManager.getInstance(DataManager.class);
        String query = "{ \"query\" : { \"query_string\": { \"query\" : \"-distributionVersion:*SNAPSHOT\" } } }";
        JsonObject json = manager.searchInstalls("install", query,
            Collections.<String, Object>singletonMap(Parameters.SEARCH_TYPE, "count"));
        assertEquals(1, json.getAsJsonObject("hits").getAsJsonPrimitive("total").getAsLong());
    }

    @Test
    public void countInstallsMatchingAll() throws Exception
    {
        DataManager manager = this.componentManager.getInstance(DataManager.class);
        String query = "{ \"query\" : { \"match_all\": { } } }";
        JsonObject json = manager.countInstalls("install", query, Collections.<String, Object>emptyMap());
        assertEquals(3, json.getAsJsonPrimitive("count").getAsLong());
    }

    private void addIndex(String id, String date, String version)
    {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("date", date);
        jsonMap.put("distributionVersion", version);
        String json = JSONObject.fromObject(jsonMap).toString();
        esSetup.execute(index("installs", "install", id).withSource(json));
    }
}
