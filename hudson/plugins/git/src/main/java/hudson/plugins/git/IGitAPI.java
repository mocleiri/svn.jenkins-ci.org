package hudson.plugins.git;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Tag;
import org.spearce.jgit.transport.RemoteConfig;

public interface IGitAPI {
    String getGitExe();
	
    boolean hasGitRepo() throws GitException;
	boolean hasGitModules() throws GitException;
	
	void submoduleInit()  throws GitException;
    void submoduleUpdate()  throws GitException;
    
    public void fetch(String repository, String refspec) throws GitException;
    void fetch(RemoteConfig remoteRepository);
    
    void fetch() throws GitException;
    void push(String revspec) throws GitException;
    void merge(String revSpec) throws GitException;
    void clone(RemoteConfig source) throws GitException;
    
    ObjectId revParse(String revName) throws GitException;
    List<Branch> getBranches() throws GitException;
    List<Branch> getRemoteBranches() throws GitException;
    List<Branch> getBranchesContaining(String revspec) throws GitException;
    
    List<IndexEntry> lsTree(String treeIsh) throws GitException;
    
    List<ObjectId> revListBranch(String branchId) throws GitException;
    List<ObjectId> revListAll() throws GitException;
    
    String describe(String commitIsh) throws GitException;
    
    List<Tag> getTagsOnCommit(String revName) throws GitException, IOException;
    
    void tag(String tagName, String comment) throws GitException;
    void deleteTag(String tagName) throws GitException;
    
    void changelog(String revFrom, String revTo, OutputStream fos) throws GitException;
	void checkout(String revToBuild) throws GitException;
  
	void add(String filePattern) throws GitException;
	void branch(String name) throws GitException;

    void commit(File f) throws GitException;

    ObjectId mergeBase(ObjectId sha1, ObjectId sha12);

    
}
