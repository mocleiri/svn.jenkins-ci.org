/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Alan Harder
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
package hudson.plugins.sidebar_link;

import hudson.Plugin;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import java.io.IOException;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;

/**
 * Simply add a link in the main page sidepanel.
 * @author Alan.Harder@sun.com
 */
public class SidebarLinkPlugin extends Plugin {
    private String url = "", text = "", icon = null;

    @Override public void start() throws Exception {
	load();
	Hudson.getInstance().getActions().add(new LinkAction(this));
    }

    public String getUrl() { return url; }
    public String getText() { return text; }
    public String getIcon() { return icon; }

    @Override public void configure(JSONObject formData)
	    throws IOException, ServletException, FormException {
	url = formData.optString("url");
	text = formData.optString("text");
	icon = formData.optString("icon");
	save();
    }
}
