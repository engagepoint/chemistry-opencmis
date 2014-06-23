package org.apache.chemistry.opencmis.jcr.query;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.jcr.AbstractJcrSessionTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

/**
 * That test checks the results of CMIS queries
 *
 * @author: vadym.karko
 * @since: 6/3/14 3:02 PM
 */
public class QueryTest extends AbstractJcrSessionTest
{
    private List<String> garbage = new ArrayList<String>(); // used for deleting all created documents

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        String root = getRootFolder().getId();
        createFolder("TemplateG1", root);
        createFolder("TemplateG2", root);
        createFolder("TemplateG3", root);
        createFolder("TemplateG4", root);
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
                "SELECT * FROM cmis:folder WHERE cmis:name IN ('TemplateG1', 'TemplateG2', 'TemplateG3')", "cmis:name",
                "TemplateG1", "TemplateG2", "TemplateG3"
        );
    }

    @Test
    public void shouldQueryNotIn() throws Exception
    {
        verifyQueryResults(
                "SELECT * FROM cmis:folder WHERE cmis:name NOT IN ('TemplateG1', 'TemplateG2', 'TemplateG3')", "cmis:name",
                "TemplateG4"
        );
    }


    /**
     * Executes a query and verifies that founded results are equals to expected results
     * @param sql SQL query string
     * @param field name of filed, to extract values from results
     * @param expected array of expected results
     */
    private void verifyQueryResults(String sql, String field, Object... expected)
    {
        ObjectList results = getJcrRepository().query(getSession(), sql, false, false, null, null);

        Object[] actual = extractFromQueryResults(results, field);

        assertArrayEquals("Query result should be equals", expected, actual);
    }

    /**
     * Retrieves values from query results from specified field into array
     * @param results query results
     * @param field name of filed, which values should be extracted
     * @return array of values from all query results
     */
    private Object[] extractFromQueryResults(ObjectList results, String field)
    {
        List<Object> list = new ArrayList<Object>();

        for (ObjectData i : results.getObjects())
        {
            list.add(i.getProperties().getProperties().get(field).getFirstValue());
        }

        return list.toArray();
    }

    /**
     * Creates a folder
     * @param name folder's name
     * @param parent folder's parent ID
     * @return an ID of created folder
     */
    private String createFolder(String name, String parent)
    {
        PropertiesImpl properties = new PropertiesImpl();

        properties.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, "cmis:folder"));
        properties.addProperty(new PropertyStringImpl(PropertyIds.NAME, name));

        String id = getJcrRepository().createFolder(getSession(), properties, parent);
        garbage.add(id);

        return id;
    }
}
