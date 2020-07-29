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
package org.xwiki.ratings.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.ratings.Rating;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiStoreInterface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.ratings.RatingsManager.RATINGS_CLASSREFERENCE;
import static org.xwiki.ratings.RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR;
import static org.xwiki.ratings.RatingsManager.RATING_CLASS_FIELDNAME_PARENT;

/**
 * Tests for {@link DefaultRatingsManager}.
 *
 * @version $Id$
 * @since 12.6
 */
@ComponentTest
public class DefaultRatingsManagerTest
{
    private static final DocumentReference RATING_CLASS_REFERENCE =
        new DocumentReference(RATINGS_CLASSREFERENCE, new WikiReference("xwiki"));

    @InjectMockComponents
    private DefaultRatingsManager defaultRatingsManager;

    @MockComponent
    @Named("compactwiki")
    protected EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xWiki;

    @BeforeEach
    public void setup()
    {
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xWiki);
        when(this.context.getWikiReference()).thenReturn(new WikiReference("xwiki"));
    }

    @Test
    public void getUserRatings() throws Exception
    {
        String statement = "select distinct doc.fullName, obj.number, doc.date "
            + "from XWikiDocument as doc, BaseObject as obj, StringProperty as authorProp "
            + "where doc.fullName=obj.name and doc.translation=0 and obj.className=:ratingClassName "
            + "and obj.id=authorProp.id.id and authorProp.id.name=:authorPropertyName and authorProp.value=:authorValue"
            + " and obj.name not in ("
            + "select obj2.name from BaseObject as obj2, StringProperty as statusprop "
            + "where obj.id=obj2.id and obj2.className=:ratingClassName and obj2.id=statusprop.id.id and "
            + "statusprop.id.name=:statusPropertyName and "
            + "(statusprop.value=:statusModerated or statusprop.value=:statusRefused)"
            + ") order by doc.date asc";

        DocumentReference userDocReference = new DocumentReference("mywiki", "Foo", "Bar");
        when(userReferenceSerializer.serialize(CurrentUserReference.INSTANCE)).thenReturn(userDocReference);
        when(this.entityReferenceSerializer.serialize(userDocReference)).thenReturn("mywiki:XWiki.Foo");

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(statement, Query.HQL)).thenReturn(query);

        when(query.bindValue("ratingClassName", "XWiki.RatingsClass")).thenReturn(query);
        when(query.bindValue("authorPropertyName", RATING_CLASS_FIELDNAME_AUTHOR)).thenReturn(query);
        when(query.bindValue("authorValue", "mywiki:XWiki.Foo")).thenReturn(query);
        when(query.bindValue("statusPropertyName", "status")).thenReturn(query);
        when(query.bindValue("statusModerated", "moderated")).thenReturn(query);
        when(query.bindValue("statusRefused", "refused")).thenReturn(query);
        when(query.setLimit(26)).thenReturn(query);
        when(query.setOffset(12)).thenReturn(query);

        DocumentReference pageRated1 = new DocumentReference("mywiki", "Some", "Thing");
        DocumentReference pageRated2 = new DocumentReference("mywiki", "Foo", "Foo");
        DocumentReference pageRated3 = new DocumentReference("mywiki", "Bar", "Baz");

        when(query.execute()).thenReturn(Arrays.asList(
            new Object[] { "mywiki:Some.Thing", 0 },
            new Object[] { "mywiki:Foo.Foo", 3 },
            new Object[] { "mywiki:Bar.Baz", 0 },
            new Object[] { "mywiki:Some.Thing", 2 }
        ));

        when(this.documentReferenceResolver.resolve("mywiki:Some.Thing")).thenReturn(pageRated1);
        when(this.documentReferenceResolver.resolve("mywiki:Foo.Foo")).thenReturn(pageRated2);
        when(this.documentReferenceResolver.resolve("mywiki:Bar.Baz")).thenReturn(pageRated3);


        XWikiDocument result1Doc = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(pageRated1, this.context)).thenReturn(result1Doc);
        BaseObject obj1 = mock(BaseObject.class);
        BaseObject obj2 = mock(BaseObject.class);
        when(result1Doc.getXObject(RATING_CLASS_REFERENCE, 0)).thenReturn(obj1);
        when(result1Doc.getXObject(RATING_CLASS_REFERENCE, 2)).thenReturn(obj2);

        XWikiDocument result2Doc = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(pageRated2, this.context)).thenReturn(result2Doc);
        BaseObject obj3 = mock(BaseObject.class);
        when(result2Doc.getXObject(RATING_CLASS_REFERENCE, 3)).thenReturn(obj3);

        XWikiDocument result3Doc = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(pageRated3, this.context)).thenReturn(result3Doc);
        BaseObject obj4 = mock(BaseObject.class);
        when(result3Doc.getXObject(RATING_CLASS_REFERENCE, 0)).thenReturn(obj4);

        List<Rating> expectedRatings = Arrays.asList(
            new DefaultRating(pageRated1, obj1, this.context, this.defaultRatingsManager),
            new DefaultRating(pageRated2, obj3, this.context, this.defaultRatingsManager),
            new DefaultRating(pageRated3, obj4, this.context, this.defaultRatingsManager),
            new DefaultRating(pageRated1, obj2, this.context, this.defaultRatingsManager)
        );

        List<Rating> ratings = this.defaultRatingsManager.getRatings(CurrentUserReference.INSTANCE, 12, 26, true);

        assertEquals(expectedRatings, ratings);

        // Ensure that all arguments are binded
        verify(query).bindValue("ratingClassName", "XWiki.RatingsClass");
        verify(query).bindValue("authorPropertyName", RATING_CLASS_FIELDNAME_AUTHOR);
        verify(query).bindValue("authorValue", "mywiki:XWiki.Foo");
        verify(query).bindValue("statusPropertyName", "status");
        verify(query).bindValue("statusModerated", "moderated");
        verify(query).bindValue("statusRefused", "refused");
        verify(query).setLimit(26);
        verify(query).setOffset(12);
    }
}
