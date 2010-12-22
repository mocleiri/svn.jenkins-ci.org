package hudson.plugins.scm_sync_configuration.scms;

import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.scms.impl.ScmSyncSubversionSCM;

import java.util.HashMap;
import java.util.Map;
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

public abstract class SCM {
	
	protected static final Logger LOGGER = Logger.getLogger(SCM.class.getName());
	
	protected static final Map<String, SCM> SCM_IMPLEMENTATIONS = new HashMap<String, SCM>(){ {
		put("SUBVERSION", new ScmSyncSubversionSCM());	
	} };
	
	protected String title;
	protected String scmClassName;
	protected String configPage;
	protected String repositoryUrlHelpPath;
	
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

	public static SCM valueOf(String scm){
		return SCM_IMPLEMENTATIONS.get(scm);
	}
	
	public static SCM[] values(){
		return SCM_IMPLEMENTATIONS.keySet().toArray(new SCM[0]);
	}
	
    public String toString(){
        return new ToStringBuilder(this).append("title", title).append("scmClassName", scmClassName)
                .append("configPage", configPage).append("repositoryUrlHelpPath", repositoryUrlHelpPath).toString();
    }
}
