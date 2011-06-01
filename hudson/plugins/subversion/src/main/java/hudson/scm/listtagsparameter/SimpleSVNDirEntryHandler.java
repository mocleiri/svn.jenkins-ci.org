/*
 * The MIT License
 *
 * Copyright (c) 2010-2011, Manufacture Francaise des Pneumatiques Michelin,
 * Romain Seguy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.scm.listtagsparameter;

import hudson.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;

/**
 * Simple {@link ISVNDirEntryHandler} used to get a list containing all the
 * directories in a given Subversion repository.
 *
 * @author Romain Seguy (http://openromain.blogspot.com)
 */
public class SimpleSVNDirEntryHandler implements ISVNDirEntryHandler {

  private List<String> dirs = new ArrayList<String>();
  private Pattern filterPattern = null;

  public SimpleSVNDirEntryHandler(String filter) {
    if(StringUtils.isNotBlank(filter)) {
      filterPattern = Pattern.compile(filter);
    }
  }

  public List<String> getDirs() {
    return getDirs(false);
  }

  public List<String> getDirs(boolean reverse) {
    Collections.sort(dirs);
    if(reverse) {
        Collections.reverse(dirs);
    }
    return dirs;
  }

  @Override
  public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
    if(filterPattern != null && filterPattern.matcher(dirEntry.getName()).matches() || filterPattern == null) {
      dirs.add(Util.removeTrailingSlash(dirEntry.getName()));
    }
  }

}
