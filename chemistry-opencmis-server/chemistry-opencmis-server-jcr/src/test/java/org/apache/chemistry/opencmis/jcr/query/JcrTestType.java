package org.apache.chemistry.opencmis.jcr.query;

import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.server.ObjectInfoImpl;
import org.apache.chemistry.opencmis.jcr.JcrFolder;
import org.apache.chemistry.opencmis.jcr.JcrTypeManager;
import org.apache.chemistry.opencmis.jcr.PathManager;
import org.apache.chemistry.opencmis.jcr.type.JcrTypeHandlerManager;

import javax.jcr.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Helper class for tests. Instances of this class represent a <code>cmis:folder</code>, which is same
 * as <code>nt:folder</code> with additional multi-value property <code>test:tags</code> backed by an
 * underlying JCR <code>Node</code>.
 * 
 * @author: vadym.karko
 * @since: 6/6/14 10:13 AM
 */
public class JcrTestType extends JcrFolder
{
    public JcrTestType(Node node, JcrTypeManager typeManager, PathManager pathManager, JcrTypeHandlerManager typeHandlerManager)
    {
        super(node, typeManager, pathManager, typeHandlerManager);
    }

    @Override
    protected void compileProperties(PropertiesImpl properties, Set<String> filter, ObjectInfoImpl objectInfo)
            throws RepositoryException
    {
        super.compileProperties(properties, filter, objectInfo);

        String typeId = getTypeIdInternal();

        try
        {
            Property tags = getNode().getProperty("test:tags");
            List<String> list = new ArrayList<String>();
            for (Value value : tags.getValues())
            {
                list.add(value.getString());
            }
            addPropertyList(properties, typeId, filter, "test:tags", list);
        }
        catch (PathNotFoundException e) {}
    }
}
