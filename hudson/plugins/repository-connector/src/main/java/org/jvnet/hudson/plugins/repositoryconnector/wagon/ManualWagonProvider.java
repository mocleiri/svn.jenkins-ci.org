package org.jvnet.hudson.plugins.repositoryconnector.wagon;

/*
 * Copyright (c) 2010 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0, 
 * and you may not use this file except in compliance with the Apache License Version 2.0. 
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the Apache License Version 2.0 is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.sonatype.aether.connector.wagon.WagonProvider;

/**
 * Supports file, http and https
 */
public class ManualWagonProvider implements WagonProvider {

	public Wagon lookup(String roleHint) throws Exception {
		if ("file".equals(roleHint)) {
			return new FileWagon();
		} else if ("http".equals(roleHint)) {
			return new LightweightHttpWagon();
		} else if ("https".equals(roleHint)) {
			return new LightweightHttpsWagon();
		}
		return null;
	}

	public void release(Wagon wagon) {

	}

}
