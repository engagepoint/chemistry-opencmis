package org.apache.chemistry.opencmis.jcr.query;

import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;
import org.apache.chemistry.opencmis.jcr.JcrFolder;
import org.apache.chemistry.opencmis.jcr.JcrTypeManager;
import org.apache.chemistry.opencmis.jcr.impl.DefaultFolderTypeHandler;
import org.apache.chemistry.opencmis.jcr.impl.DefaultIdentifierMapBase;
import org.apache.jackrabbit.commons.cnd.CndImporter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.InputStreamReader;

/**
 * Helper class for tests. Type handler that provides <code>cmis:folder</code>, which is same
 * as <code>cmis:folder</code> with additional multi-value property <code>test:tags</code>.
 *
 * @author: vadym.karko
 * @since: 6/6/14 10:16 AM
 */
public class DefaultTestTypeHandler extends DefaultFolderTypeHandler
{
    @Override
    public TypeDefinition getTypeDefinition() {
        FolderTypeDefinitionImpl type = (FolderTypeDefinitionImpl) super.getTypeDefinition();

        type.addPropertyDefinition(JcrTypeManager.createPropDef("test:tags", "Tags", "Tags", PropertyType.STRING,
                Cardinality.MULTI, Updatability.READWRITE, false, false));

        return type;
    }

    private static class TestTypeIdentifierMap extends DefaultIdentifierMapBase
    {
        public TestTypeIdentifierMap()
        {
            super("nt:folder");
            cmis2Jcr.put("test:tags", "@test:tags");
        }
    }

    public IdentifierMap getIdentifierMap() {
        return new TestTypeIdentifierMap();
    }

    public JcrFolder getJcrNode(Node node) {
        return new JcrTestType(node, typeManager, pathManager, typeHandlerManager);
    }

    @Override
    protected void addMixins(Node node) throws RepositoryException
    {
        super.addMixins(node);

        try
        {
            CndImporter.registerNodeTypes(
                    new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test-type.cnd")),
                    node.getSession()
            );
        }
        catch (Exception e){ e.printStackTrace(); }

        node.addMixin("test:taggable");
    }
}
