package com.engagepoint.ezdoc.cmis.tests;

import javax.jcr.Repository;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.jcr.JcrRepository;
import org.apache.chemistry.opencmis.jcr.JcrTypeManager;
import org.apache.chemistry.opencmis.jcr.PathManager;
import org.apache.chemistry.opencmis.jcr.impl.DefaultDocumentTypeHandler;
import org.apache.chemistry.opencmis.jcr.impl.DefaultFolderTypeHandler;
import org.apache.chemistry.opencmis.jcr.impl.DefaultUnversionedDocumentTypeHandler;
import org.apache.chemistry.opencmis.jcr.type.JcrTypeHandlerManager;
import org.apache.jackrabbit.core.TransientRepository;


/**
 * The test covers org.apache.chemistry.opencmis.jcr.JcrRepository class.
 * 
 * @author sergey
 *
 */
public class JcrRepositoryTest extends TestCase {
	
	private Repository transientRepo;
	private JcrRepository jcrRepo;
	private Session session;
	private JcrTypeManager typeManager;
	private static final String mountPath = "/";
	
	
	@Override
	protected void setUp() throws Exception {
		transientRepo = new TransientRepository();
		session = transientRepo.login();
		typeManager = new JcrTypeManager();
		PathManager pathManger = new PathManager(mountPath);
        JcrTypeHandlerManager typeHandlerManager = createTypeHandlerManager(pathManger, typeManager);
        jcrRepo = new JcrRepository(transientRepo, pathManger, typeManager, typeHandlerManager);
	}

	@Override
	protected void tearDown() throws Exception {
		((TransientRepository) transientRepo).shutdown();
	}
	
	protected JcrTypeHandlerManager createTypeHandlerManager(PathManager pathManager, JcrTypeManager typeManager) {
	        JcrTypeHandlerManager typeHandlerManager = new JcrTypeHandlerManager(pathManager, typeManager);
	        typeHandlerManager.addHandler(new DefaultFolderTypeHandler());
	        typeHandlerManager.addHandler(new DefaultDocumentTypeHandler());
	        typeHandlerManager.addHandler(new DefaultUnversionedDocumentTypeHandler());
	        return typeHandlerManager;
	}
	
	public void testRootFolder(){
		ObjectData objData = jcrRepo.getObjectByPath(session, "/", null, false, false,null,false);
		System.out.println(objData.getId());
		assertNotNull(objData);
	}
	
	
	
}
