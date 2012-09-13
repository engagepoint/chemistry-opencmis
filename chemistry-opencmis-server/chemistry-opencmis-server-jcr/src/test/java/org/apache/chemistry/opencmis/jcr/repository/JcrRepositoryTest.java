package org.apache.chemistry.opencmis.jcr.repository;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.jcr.JcrRepository;
import org.apache.chemistry.opencmis.jcr.JcrTypeManager;
import org.apache.chemistry.opencmis.jcr.PathManager;
import org.apache.chemistry.opencmis.jcr.impl.DefaultDocumentTypeHandler;
import org.apache.chemistry.opencmis.jcr.impl.DefaultFolderTypeHandler;
import org.apache.chemistry.opencmis.jcr.impl.DefaultUnversionedDocumentTypeHandler;
import org.apache.chemistry.opencmis.jcr.type.JcrTypeHandlerManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The test covers org.apache.chemistry.opencmis.jcr.JcrRepository class.
 * 
 * @author sergey.nikolaew@engagepoint.com
 *
 */
public class JcrRepositoryTest  {
	
	 
	private static Logger log = LoggerFactory.getLogger(JcrRepositoryTest.class);
	private Repository transientRepo;
	private JcrRepository jcrRepo;
	private Session session;
	private JcrTypeManager typeManager;
	private final String mountPath = "/";
	
	
	@Before
	public void setUp() throws Exception {
		transientRepo = new TransientRepository();
		session = transientRepo.login();
		typeManager = new JcrTypeManager();
		PathManager pathManger = new PathManager(mountPath);
        JcrTypeHandlerManager typeHandlerManager = createTypeHandlerManager(pathManger, typeManager);
        jcrRepo = new JcrRepository(transientRepo, pathManger, typeManager, typeHandlerManager);
	}

	@After
	public void tearDown() throws Exception {
		((TransientRepository) transientRepo).shutdown();
	}
	
	private JcrTypeHandlerManager createTypeHandlerManager(PathManager pathManager, JcrTypeManager typeManager) {
	        JcrTypeHandlerManager typeHandlerManager = new JcrTypeHandlerManager(pathManager, typeManager);
	        typeHandlerManager.addHandler(new DefaultFolderTypeHandler());
	        typeHandlerManager.addHandler(new DefaultDocumentTypeHandler());
	        typeHandlerManager.addHandler(new DefaultUnversionedDocumentTypeHandler());
	        return typeHandlerManager;
	}
	
	@Test
	public void testRootFolder(){
		ObjectData objData = jcrRepo.getObjectByPath(session, "/", null, false, false,null,false);
		log.info("Root folder ID: " + objData.getId());
		Assert.assertNotNull(objData);
	}
	
	
	
}
