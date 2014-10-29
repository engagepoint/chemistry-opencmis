package org.apache.chemistry.opencmis.jcr.query;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.jcr.AbstractJcrSessionTest;
import org.apache.chemistry.opencmis.jcr.impl.DefaultDocumentTypeHandler;
import org.apache.chemistry.opencmis.jcr.impl.DefaultUnversionedDocumentTypeHandler;
import org.apache.chemistry.opencmis.jcr.type.JcrTypeHandlerManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

/**
 * That test checks the results of CMIS queries.
 *
 * @author: vadym.karko
 * @since: 6/3/14 3:02 PM
 */
public class QueryTest extends AbstractJcrSessionTest
{
    private List<String> garbage = new ArrayList<String>(); // used for deleting all created documents

    @Override
    protected void addToTypeHandlerManager(final JcrTypeHandlerManager typeHandlerManager)
    {
        typeHandlerManager.addHandler(new DefaultDocumentTypeHandler());
        typeHandlerManager.addHandler(new DefaultUnversionedDocumentTypeHandler());
        typeHandlerManager.addHandler(new DefaultTestTypeHandler());
    }

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        String root = getRootFolder().getId();
        createFolder("TemplateG1", root);
        createFolder("TemplateG2", root);
        createFolder("TemplateG3", root, "tag1", "tag2", "tag3");
        createFolder("TemplateG4", root, "tag1", "tag2", "tag3", "tag4");
        createFolder("TemplateG5", root, "tag1", "tag2", "tag3", "tag5");
    }

    @After
    public void cleanUp() throws Exception
    {
        for (String o : garbage)
        {
            getJcrRepository().deleteObject(getSession(), o, true);
        }
    }


    @Test
    public void shouldQueryIn() throws Exception
    {
        verifyQueryResults(
                "SELECT * FROM cmis:folder WHERE cmis:name IN ('TemplateG1', 'TemplateG2', 'TemplateG3')",
                "TemplateG1", "TemplateG2", "TemplateG3"
        );
    }

    @Test
    public void shouldQueryNotIn() throws Exception
    {
        verifyQueryResults(
                "SELECT * FROM cmis:folder WHERE cmis:name NOT IN ('TemplateG1', 'TemplateG2', 'TemplateG3')",
                "TemplateG4", "TemplateG5"
        );
    }

    @Test
    public void shouldQueryNotEqualsAny() throws Exception
    {
        verifyQueryResults(
                "SELECT * FROM cmis:folder WHERE 'tag4' = ANY test:tags",
                "TemplateG4"
        );
    }

    @Test
    public void shouldQueryNotAnyIn() throws Exception
    {
        verifyQueryResults(
                "SELECT * FROM cmis:folder WHERE ANY test:tags IN ('tag4', 'tag5')",
                "TemplateG4", "TemplateG5"
        );
    }

    @Test
    public void shouldQueryNotAnyNotIn() throws Exception
    {
        verifyQueryResults(
                "SELECT * FROM cmis:folder WHERE ANY test:tags NOT IN ('tag4', 'tag5')",
                "TemplateG1", "TemplateG2", "TemplateG3"
        );
    }


    /**
     * Executes a query and verifies that founded results are equals to expected results
     * @param sql SQL query string
     * @param field name of filed, to extract values from results
     * @param expected array of expected results
     */
    private void verifyQueryResults(String sql, Object... expected)
    {
        ObjectList results = getJcrRepository().query(getSession(), sql, false, false, null, null);

        Object[] actual = extractNamesFromQueryResults(results);

        assertArrayEquals("Query result should be equals", expected, actual);
    }

    /**
     * Retrieves names from query into array
     * @param results query results
     * @return array of names from all query results
     */
    private Object[] extractNamesFromQueryResults(ObjectList results)
    {
        List<Object> list = new ArrayList<Object>();

        for (ObjectData i : results.getObjects())
        {
            list.add(i.getProperties().getProperties().get("cmis:name").getFirstValue());
        }

        return list.toArray();
    }

    /**
     * Creates a folder
     * @param name folder's name
     * @param parent folder's parent ID
     * @return an ID of created folder
     */
    private String createFolder(String name, String parent, String... tags)
    {
        PropertiesImpl properties = new PropertiesImpl();

        properties.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, "cmis:folder"));
        properties.addProperty(new PropertyStringImpl(PropertyIds.NAME, name));

        if (tags.length > 0) properties.addProperty(new PropertyStringImpl("test:tags", Arrays.asList(tags)));

        String id = getJcrRepository().createFolder(getSession(), properties, parent);
        garbage.add(id);

        return id;
    }
}
