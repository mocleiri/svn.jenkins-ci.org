package hudson.plugins.git;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.plugins.git.util.GitUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A common usecase for git submodules is to have child submodules, and a parent 'configuration' project that ties the
 * correct versions together. It is useful to be able to speculatively compile all combinations of submodules, so that
 * you can _know_ if a particular combination is no longer compatible.
 * 
 * @author nigelmagnay
 */
public class SubmoduleCombinator
{
  IGitAPI      git;
  Launcher     launcher;
  FilePath     workspace;
  TaskListener listener;

  long         tid = new Date().getTime();
  long         idx = 1;
  
  Collection<SubmoduleConfig> submoduleConfig;
  
  public SubmoduleCombinator(IGitAPI git, Launcher launcher, TaskListener listener, FilePath workspace,
      Collection<SubmoduleConfig> cfg)
  {
    this.git = git;
    this.listener = listener;
    this.launcher = launcher;
    this.workspace = workspace;
    this.submoduleConfig = cfg;
  }

  public void createSubmoduleCombinations()
  {
    GitUtils gitUtils = new GitUtils(listener, git);

    Map<IndexEntry, Collection<Revision>> moduleBranches = new HashMap<IndexEntry, Collection<Revision>>();

    for (IndexEntry submodule : gitUtils.getSubmodules("HEAD"))
    {
      FilePath subdir = new FilePath(workspace, submodule.getFile());
      IGitAPI subGit = new GitAPI(git.getGitExe(), launcher, subdir, listener);
      
      Collection<Revision> items = new GitUtils(listener, subGit).getTipBranches();
      
      filterRevisions(submodule.getFile(), items);
      
      moduleBranches.put(submodule, items);
    }

    // Remove any uninteresting branches
    
    
    
    for (IndexEntry entry : moduleBranches.keySet())
    {
      listener.getLogger().print("Submodule " + entry.getFile() + " branches");
      for (Revision br : moduleBranches.get(entry))
      {
        listener.getLogger().print(" " + br.toString());

      }
      listener.getLogger().print("\n");
    }
    
    // Make all the possible combinations
    List<Map<IndexEntry, Revision>> combinations = createCombinations(moduleBranches);

    listener.getLogger().println("There are " + combinations.size() + " submodule/revision combinations possible");

    // Create a map which is SHA1 -> Submodule IDs that were present
    Map<String, List<IndexEntry>> entriesMap = new HashMap<String, List<IndexEntry>>();
    // Knock out already-defined configurations
    for (String sha1 : git.revList())
    {
      // What's the submodule configuration
      List<IndexEntry> entries = gitUtils.getSubmodules(sha1);
      entriesMap.put(sha1, entries);

    }

    for (List<IndexEntry> entries : entriesMap.values())
    {
      for (Iterator<Map<IndexEntry, Revision>> it = combinations.iterator(); it.hasNext();)
      {
        Map<IndexEntry, Revision> item = it.next();
        if (matches(item, entries))
        {
          it.remove();
          break;
        }

      }
    }
    
    listener.getLogger().println("There are " + combinations.size() + " configurations that could be generated.");
  
    String headSha1 = git.revParse("HEAD");

    
    // Make up the combinations
    
    for (Map<IndexEntry, Revision> combination : combinations)
    {
      // By default, use the head sha1
      String sha1 = headSha1;
      int min = Integer.MAX_VALUE;

      // But let's see if we can find the most appropriate place to create the branch
      for (String sha : entriesMap.keySet())
      {
        List<IndexEntry> entries = entriesMap.get(sha);
        int value = difference(combination, entries);
        if (value > 0 && value < min)
        {
          min = value;
          sha1 = sha;
        }

        if (min == 1) break; // look no further
      }
      
      git.checkout(sha1);
      makeCombination(combination);
    }
    
  }

  private Collection<Revision> filterRevisions(String name, Collection<Revision> items)
  {
    SubmoduleConfig config = getSubmoduleConfig(name);
    if (config == null) return items;

    for (Iterator<Revision> it = items.iterator(); it.hasNext();)
    {
      Revision r = it.next();
      if (!config.revisionMatchesInterest(r)) it.remove();
    }

    return items;
  }

  private SubmoduleConfig getSubmoduleConfig(String name)
  {
    for (SubmoduleConfig config : this.submoduleConfig)
    {
      if (config.getSubmoduleName().equals(name)) return config;
    }
    return null;
  }

  protected void makeCombination(Map<IndexEntry, Revision> settings)
  {
    // Assume we are checked out
    String name = "combine-" + tid + "-" + (idx++); 
    git.branch(name);
    git.checkout(name);
   
    String commit = "Hudson generated combination of:\n";
    
    for (IndexEntry submodule : settings.keySet())
    {
      Revision branch = settings.get(submodule);
      commit += "  " + submodule.getFile() + " " + branch.toString() + "\n";
    }
    
    listener.getLogger().print(commit);
    
    
    for (IndexEntry submodule : settings.keySet())
    {
      Revision branch = settings.get(submodule);
      FilePath subdir = new FilePath(workspace, submodule.getFile());
      IGitAPI subGit = new GitAPI(git.getGitExe(), launcher, subdir, listener);
      
      subGit.checkout(branch.sha1);
      git.add(submodule.file);
      
    }
    
    try
    {
      File f = File.createTempFile("gitcommit", ".txt");
      FileOutputStream fos = new FileOutputStream(f);
      fos.write(commit.getBytes());
      fos.close();
      git.commit(f);
      f.delete();
    }
    catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public int difference(Map<IndexEntry, Revision> item, List<IndexEntry> entries)
  {
      int difference = 0;
    if (entries.size() != item.keySet().size()) return -1;

      for (IndexEntry entry : entries)
      {
        Revision b = null;
      for (IndexEntry e : item.keySet())
      {
        if (e.getFile().equals(entry.getFile())) b = item.get(e);
      }

      if (b == null) return -1;

      if (!entry.object.equals(b.getSha1())) difference++;

      }
      return difference;
  }

  protected boolean matches(Map<IndexEntry, Revision> item, List<IndexEntry> entries)
  {
    return (difference(item, entries) == 0);
  }

  public List<Map<IndexEntry, Revision>> createCombinations(Map<IndexEntry, Collection<Revision>> moduleBranches)
  {
    
    if (moduleBranches.keySet().size() == 0) return new ArrayList<Map<IndexEntry, Revision>>();

    // Get an entry:
    List<Map<IndexEntry, Revision>> thisLevel = new ArrayList<Map<IndexEntry, Revision>>();

    IndexEntry e = moduleBranches.keySet().iterator().next();

    for (Revision b : moduleBranches.remove(e))
    {
      Map<IndexEntry, Revision> result = new HashMap<IndexEntry, Revision>();

      result.put(e, b);
      thisLevel.add(result);
    }

    List<Map<IndexEntry, Revision>> children = createCombinations(moduleBranches);
    if (children.size() == 0) return thisLevel;
    
    // Merge the two together
    List<Map<IndexEntry, Revision>> result = new ArrayList<Map<IndexEntry, Revision>>();

    for (Map<IndexEntry, Revision> thisLevelEntry : thisLevel)
    {
      
      for (Map<IndexEntry, Revision> childLevelEntry : children)
      {
        HashMap<IndexEntry, Revision> r = new HashMap<IndexEntry, Revision>();
        r.putAll(thisLevelEntry);
        r.putAll(childLevelEntry);
        result.add(r);
      }
      
    }

    return result;
  }
}
