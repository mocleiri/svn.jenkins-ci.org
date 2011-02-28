/*Copyright (c) 2010, Parallels-NSU lab. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided 
that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions 
    * and the following disclaimer.
    
    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
    * and the following disclaimer in the documentation and/or other materials provided with 
    * the distribution.
    
    * Neither the name of the Parallels-NSU lab nor the names of its contributors may be used to endorse 
    * or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED 
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE 
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package hpi;

import java.util.LinkedList;
import java.util.List;

public class Directory {
	private String name;
	protected List<Directory> nested;

	public Directory() {
		this.nested = new LinkedList<Directory>();
	}

	public Directory(String name) {
		this.nested = new LinkedList<Directory>();
		this.name = name;
	}

	public long getId() {
		return hashCode();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Directory> getNested() {
		return nested;
	}

	public void setNested(List<Directory> nested) {
		this.nested = nested;
	}

	public boolean addDirectory(Directory dir) {
		return nested.add(dir);
	}

	public void deleteDirectory(Directory dir) {
		nested.remove(dir);
	}
}