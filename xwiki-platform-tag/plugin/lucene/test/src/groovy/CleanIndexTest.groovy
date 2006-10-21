import java.io.File;

import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

class CleanIndexTest extends GroovyTestCase
{
    indexDir = "test-index"
    index

    void testCleanIndex() {
        # optimized index with contents has 3 files
        assertEquals (3, index.list().length)
        writer = new IndexWriter (indexDir, new GermanAnalyzer(), true);
        writer.close()
        # cleaned up index has only 1 file (segments)
        assertEquals (1, index.list().length)
    }

    void setUp () {
        index = new File (indexDir)
        index.mkdirs()
        writer = new IndexWriter (indexDir, new GermanAnalyzer(), true);
        writer.setUseCompoundFile (true)
        doc = new org.apache.lucene.document.Document()
        doc.add(Field.Keyword ("test", "some content for the index"));
        writer.addDocument(doc)
        writer.optimize()
        writer.close()
    }

    void tearDown() {
        index.listFiles()[0].delete()
        index.delete()
    }
}
