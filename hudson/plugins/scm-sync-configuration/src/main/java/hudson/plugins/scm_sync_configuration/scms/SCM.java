package hudson.plugins.scm_sync_configuration.scms;

import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.scms.impl.ScmSyncNoSCM;
import hudson.plugins.scm_sync_configuration.scms.impl.ScmSyncSubversionSCM;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.util.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public abstract class SCM {
	
	protected static final Logger LOGGER = Logger.getLogger(SCM.class.getName());
	
	protected static final List<SCM> SCM_IMPLEMENTATIONS = new ArrayList<SCM>(){ {
		add(new ScmSyncNoSCM());
		add(new ScmSyncSubversionSCM());
	} };
	
	transient protected String title;
	transient protected String scmClassName;
	transient protected String configPage;
	transient protected String repositoryUrlHelpPath;
	
	protected SCM(String _title, String _configPage, String _scmClassName, String _repositoryUrlHelpPath){
		this.title = _title;
		this.configPage = _configPage;
		this.scmClassName = _scmClassName;
		this.repositoryUrlHelpPath = _repositoryUrlHelpPath;
	}
	
	public String getTitle(){
		return this.title;
	}
	
	public String getConfigPage(){
		return this.configPage;
	}

	public String getSCMClassName() {
		return this.scmClassName;
	}
	
	public Descriptor<? extends hudson.scm.SCM> getSCMDescriptor(){
		return Hudson.getInstance().getDescriptorByName(getSCMClassName());
	}
	
	public String getRepositoryUrlHelpPath() {
		return this.repositoryUrlHelpPath;
	}

	public ScmRepository getConfiguredRepository(ScmManager scmManager, String scmRepositoryURL) {
		SCMCredentialConfiguration credentials = extractScmCredentials( extractScmUrlFrom(scmRepositoryURL) );
		if(credentials == null){
			return null;
		}

		LOGGER.info("Creating SCM repository object for url : "+scmRepositoryURL);
        ScmRepository repository = null;
        try {
			repository = scmManager.makeScmRepository( scmRepositoryURL );
		} catch (ScmRepositoryException e) {
			LOGGER.throwing(ScmManager.class.getName(), "makeScmRepository", e);
			LOGGER.severe("Error creating ScmRepository : "+e.getMessage());
		} catch (NoSuchScmProviderException e) {
			LOGGER.throwing(ScmManager.class.getName(), "makeScmRepository", e);
			LOGGER.severe("Error creating ScmRepository : "+e.getMessage());
		}
        if(repository == null){
        	return null;
        }

        ScmProviderRepository scmRepo = repository.getProviderRepository();

        // TODO: uncomment this ??? (MRELEASE-76)
        //scmRepo.setPersistCheckout( false );

        // TODO: instead of creating a SCMCredentialConfiguration, create a ScmProviderRepository
        if ( repository.getProviderRepository() instanceof ScmProviderRepositoryWithHost )
        {
    		LOGGER.info("Populating host data into SCM repository object ...");
            ScmProviderRepositoryWithHost repositoryWithHost =
                (ScmProviderRepositoryWithHost) repository.getProviderRepository();
            String host = repositoryWithHost.getHost();

            int port = repositoryWithHost.getPort();

            if ( port > 0 )
            {
                host += ":" + port;
            }
        }

        if(credentials != null){
    		LOGGER.info("Populating credentials data into SCM repository object ...");
	        if ( !StringUtils.isEmpty( credentials.getUsername() ) )
	        {
	            scmRepo.setUser( credentials.getUsername() );
	        }
	        if ( !StringUtils.isEmpty( credentials.getPassword() ) )
	        {
	            scmRepo.setPassword( credentials.getPassword() );
	        }
	
	        if ( scmRepo instanceof ScmProviderRepositoryWithHost )
	        {
	            ScmProviderRepositoryWithHost repositoryWithHost = (ScmProviderRepositoryWithHost) scmRepo;
	            if ( !StringUtils.isEmpty( credentials.getPrivateKey() ) )
	            {
	                repositoryWithHost.setPrivateKey( credentials.getPrivateKey() );
	            }
	
	            if ( !StringUtils.isEmpty( credentials.getPassphrase() ) )
	            {
	                repositoryWithHost.setPassphrase( credentials.getPassphrase() );
	            }
	        }
        }
        
        return repository;
	}
	
	public abstract String createScmUrlFromRequest(StaplerRequest req);
	public abstract String extractScmUrlFrom(String scmUrl);
	public abstract SCMCredentialConfiguration extractScmCredentials(String scmRepositoryURL);

	public static SCM valueOf(String scmId){
		for(SCM scm : SCM_IMPLEMENTATIONS){
			if(scmId.equals(scm.getId())){
				return scm;
			}
		}
		return null;
	}
	
	public static SCM[] values(){
		return SCM_IMPLEMENTATIONS.toArray(new SCM[0]);
	}
	
    public String toString(){
        return new ToStringBuilder(this).append("class", getClass().getName()).append("title", title).append("scmClassName", scmClassName)
                .append("configPage", configPage).append("repositoryUrlHelpPath", repositoryUrlHelpPath).toString();
    }
    
    public String getId(){
    	return getClass().getName();
    }
    
    public static class SCMXStreamConverter implements Converter {
    	public boolean canConvert(Class type) {
    		return SCM.class.isAssignableFrom(type);
    	}
    	
    	public void marshal(Object source, HierarchicalStreamWriter writer,
    			MarshallingContext context) {
    		SCM scm = (SCM)source;
    		// "class" attribute is used for backward compatibility...
    		// (SCM was, initially, an enum type)
    		// Underlying line is useless : XStream is already creating a class attribute...
    		//writer.addAttribute("class", scm.getId());
    	}
    	public Object unmarshal(HierarchicalStreamReader reader,
    			UnmarshallingContext context) {
    		String id = reader.getAttribute("class");
    		// For backward compatibility purposes
    		if(SCM.class.getName().equals(id)){
    			id = ScmSyncSubversionSCM.class.getName();
    		}
    		return SCM.valueOf(id);
    	}
    }
}
