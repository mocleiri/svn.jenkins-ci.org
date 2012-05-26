package org.jvnet.hudson.plugins.fortify360;

import hudson.FilePath;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

/** A special ClassLoader for loading the fortifyclient.jar which is a sub-module of Fortify360 hudson plugin 
 * <p>
 * When it loads, it will first try to locate wsclient.jar and wsobjects.jar, and then deduce the F360 server version
 * by checking the md5 of the jars. And then load the corresponding fortifyclient.jar.
 * <p>
 * Note the fortifyclient.jar is located in Fortify360Plugin inside the resources folder, not inside the lib, which will be 
 * automatically loaded
 * <p>
 * 
 * @author sng
 *
 */
public class FortifyClientClassLoader extends URLClassLoader {

	private static FortifyClientClassLoader _instance;
	private ClassLoader currentThreadLoader;
	
	private static Map<String, String> wsclientMd5 = null;
	private static Map<String, String> wsobjectsMd5 = null;
	
	public FortifyClientClassLoader(URL[] urls, ClassLoader parent) { 
		super(urls, parent);
		currentThreadLoader = null;
	}
	
	public static synchronized void initMd5Map() {
		if ( null == wsclientMd5 ) {
			wsclientMd5 = new HashMap<String, String>();
			wsclientMd5.put("d370675a710931bc83364cadf6adc226", "2.1");
			wsclientMd5.put("166485667af7440754e99300df39375f", "2.5");
			wsclientMd5.put("fef8b31efc1488c15ac138e4021c5c18", "2.6");  
			wsclientMd5.put("71e5f5726daa71aa01c80b9b56bebe73", "2.6.1");  
			wsclientMd5.put("d3e748409496f0b7da32c65572576b88", "2.6.5");  
			wsclientMd5.put("1a5c0281ccac18ac2b2d618cf2538591", "3.0.0");
			wsclientMd5.put("8dca96072c49f8ab2de000574760b9dd", "3.1.0");
			wsclientMd5.put("00d01db3c35f28adca7a3efb4c89bd34", "3.20");
			wsclientMd5.put("e435cf228483466d22abd491f9eaddb9", "3.30");
			wsclientMd5.put("10083805168cf5fb8ff3122f56a7884e", "3.40");
			wsclientMd5.put("51299a19a83fe85b2e0f753ea8f31dd1", "3.50");
		}
		
		if ( null == wsobjectsMd5 ) {
			wsobjectsMd5 = new HashMap<String, String>();
			wsobjectsMd5.put("220a2991ac70948c72e9ca8ef9411d40", "2.1");
			wsobjectsMd5.put("dbc1060536612c5e9903c408ce16ca2a", "2.5");
			wsobjectsMd5.put("9446eead6124a69e093844d757395adc", "2.6"); 
			wsobjectsMd5.put("69bc42ae7246ee91b67817760a21b03c", "2.6.1"); 
			wsobjectsMd5.put("82ac603b97c135134ecc56df40e9e894", "2.6.5"); 
			wsobjectsMd5.put("70da5cb9e309898784eeb54d11fbe9df", "3.0.0");
			wsobjectsMd5.put("841eabd671fc1ecb78c9a7123db0d558", "3.1.0");
			wsobjectsMd5.put("d1c1beb9f92ab01a7431b1a0b554da4e", "3.20");
			wsobjectsMd5.put("f5df453aae5c6bcb67ae16cff1b2a089", "3.30");
			wsobjectsMd5.put("ccd27276a65a1abb80c71959b4431659", "3.40");
			wsobjectsMd5.put("c8ce5499a6aa3d4a57ce095ee3e0219c", "3.50");
		}
	}
	
	public static synchronized FortifyClientClassLoader getInstance(String jarsPath, String version, PrintStream log) throws Exception { 
		
		initMd5Map();
		
		if ( null == _instance ) {
		
			ClassLoader parent = FortifyClientClassLoader.class.getClassLoader();
			List<URL> urls = new ArrayList<URL>();
			// get the wsclient.jar and wsobject.jar
			if ( StringUtils.isBlank(jarsPath) ) jarsPath = findWSClientPath(); 
			log.println("####################################################################");
			log.println("FortifyClientClassLoader: JarsPath = " + jarsPath);
			File wsclient = findFileByJarBasename(new File(jarsPath), "wsclient");
			File wsobjects = findFileByJarBasename(new File(jarsPath), "wsobjects");
			if ( !wsclient.exists() || !wsobjects.exists() ) {
				throw new RuntimeException("Invalid JarsPath: " + jarsPath + ", can't locate wsclient.jar or wsobjects.jar");
			}
			urls.add(wsclient.toURI().toURL());
			urls.add(wsobjects.toURI().toURL());

			// now, I need to deduce the F360 server version, this is a bit tricky...
			// 1. I will check the md5 of the wsclient.jar and wsobject.jar
			// 2. if md5 is not found, I will check the path name directly
			String wsclientVer = wsclientMd5.get(new FilePath(wsclient).digest());
			String wsobjectsVer = wsobjectsMd5.get(new FilePath(wsobjects).digest());
			
			if ( StringUtils.isBlank(version) ) {
				// the two versions need to be consistent
				if ( null != wsclientVer && wsclientVer.equals(wsobjectsVer) ) {
					version = wsclientVer;
					log.println("FortifyClientClassLoader: version = " + version + " (by checking md5 of wsclient.jar and wsobjects.jar)");
				} else {
					// check the path name, the checking is pretty simple
					if ( -1 != jarsPath.indexOf("3.50") ) version = "3.50";
					else if ( -1 != jarsPath.indexOf("3.40") ) version = "3.40";
					else if ( -1 != jarsPath.indexOf("3.30") ) version = "3.30";
					else if ( -1 != jarsPath.indexOf("3.20") ) version = "3.20";
					else if ( -1 != jarsPath.indexOf("3.1.0") ) version = "3.1.0";
					else if ( -1 != jarsPath.indexOf("3.0.0") ) version = "3.0.0";
					else if ( -1 != jarsPath.indexOf("2.6.5") ) version = "2.6.5";
					else if ( -1 != jarsPath.indexOf("2.6.1") ) version = "2.6.1";
					else if ( -1 != jarsPath.indexOf("2.6") ) version = "2.6";
					else if ( -1 != jarsPath.indexOf("2.5") ) version = "2.5";
					log.println("FortifyClientClassLoader: version = " + version + " (by checking the path of jarsPath)");
				}
			} else {
				log.println("FortifyClientClassLoader: version = " + version);				
			}

			if ( StringUtils.isBlank(version) ) {
				throw new RuntimeException("Can't determine Fortify 360 Server version");
			}
			
			if ( null != version && version.startsWith("3") ) {
				File fortifyCommon = new File(jarsPath, "fortify-common-" + version + ".jar");
				if ( !fortifyCommon.exists() ) {
					throw new RuntimeException("Invalid JarsPath: " + jarsPath + ", can't locate fortify-common-" + version + ".jar");
				}
				urls.add(fortifyCommon.toURI().toURL());
				
			} else if ( !version.startsWith("2.5") ) {
				// if not 3.0.0, then we will need the common.jar and common13.jar unless for v2.5
				// for 2.6, we need to include common.jar and common13.jar as well
				File common = new File(jarsPath, "common.jar");
				File common13 = new File(jarsPath, "common13.jar");
				if ( !common.exists() || !common13.exists() ) {
					throw new RuntimeException("Invalid JarsPath: " + jarsPath + ", can't locate common.jar or common13.jar");
				}
				urls.add(common.toURI().toURL());
				urls.add(common13.toURI().toURL());
			}
			
			// ok, load the correct version of fortifyclient
			urls.add(parent.getResource("fortifyclient-" + version + ".jar"));

			for(URL x : urls) {
				log.println("FortifyClientClassLoader URL: " + x.toString());
			}
			log.println("####################################################################");
			_instance = new FortifyClientClassLoader(urls.toArray(new URL[0]), parent);
		}
		
		return _instance;
	}
	
	public static void reset() {
		_instance = null;
	}
	
	/** A special hack for Hudson
	 * <p>
	 * When Hudson loads Fortify360Plugin, it use a special ClassLoader, let's call it PluginClassLoader.
	 * But when a Hudson job runs, it will run as a normal worker thread, and this worker thread is not PluginClassLoader.
	 * Therefore, it won't not found the fortifyclient.jar which is stored in Fortify360plugin as a resources.
	 * <p>
	 * Therefore, the solution is when we need to run FortifyClient, we will need to change the worker thread ClassLoader 
	 * to *this* ClassLoader (which is the PluginClassLoader)
	 * </p> 
	 * 
	 */
	public void bindCurrentThread() {
		if ( null != currentThreadLoader ) {
			throw new IllegalStateException(this.getClass().getName() + ".bindCurrentThread(), rebinding is not allowed. Probably previously binded without unbinding. currentThreadLoader is " + currentThreadLoader);
		}
		currentThreadLoader =  Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(this);
	}
	
	/** A special hack for Hudson
	 * <p>
	 * When Hudson loads Fortify360Plugin, it use a special ClassLoader, let's call it PluginClassLoader.
	 * But when a Hudson job runs, it will run as a normal worker thread, and this worker thread is not PluginClassLoader.
	 * Therefore, it won't not found the fortifyclient.jar which is stored in Fortify360plugin as a resources.
	 * <p>
	 * Therefore, the solution is when we need to run FortifyClient, we will need to change the worker thread ClassLoader 
	 * to *this* ClassLoader (which is the PluginClassLoader)
	 * </p> 
	 * 
	 */
	public void unbindCurrentThread() {
		if ( null != currentThreadLoader ) {
			Thread.currentThread().setContextClassLoader(currentThreadLoader);
			currentThreadLoader = null;
		}
	}
	
	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		try {
			return super.findClass(className);
		} catch ( ClassNotFoundException e ) {
			if ( null != currentThreadLoader ) {
				return currentThreadLoader.loadClass(className);
			} else {
				throw e;
			}
		}
	}
	
	@Override
	public URL getResource(String name) {
		URL url = findResource(name);
		if ( null == url ) {
			if ( null != currentThreadLoader ) {
				url = currentThreadLoader.getResource(name);
			}
		}
		return url;
	}	
	
	public static String findWSClientPath() {
		File[] list = PathUtils.locateBaesnameInPath("fortifyclient");
		if ( null != list && list.length > 0 ) {
			File f = list[0];
			// e.g. f is "C:\\Program Files\\Fortify Software\\Fortify 360 v2.6.5\\bin\\fortifyclient.bat"
			// we need to change this to "C:\\Program Files\\Fortify Software\\Fortify 360 v2.6.5\\Core\\lib
			File core = new File(f.getParentFile().getParentFile(), "Core");
			File lib = new File(core, "lib");
			File wsclient = findFileByJarBasename(lib, "wsclient");
			if ( null != wsclient ) return lib.toString();
		}
		return null;
	}
	
	/** Find the file by using the jar basename
	 * <p>E.g. you can find wsclient in a given path, then we will try to find
	 * wsclient.jar, wsclient-x.x.x.jar in the path
	 * <p>If we can find such a jar, the full name of the jar will be returned
	 * 
	 * @param path e.g. a library path
	 * @param basename e.g. wsclient
	 * @return the jar file with the given basename
	 */
	public static File findFileByJarBasename(File path, String basename) {
		if ( !path.exists() || !path.isDirectory() ) return null;
		
		// find by exact basename.jar
		File exactFile = new File(path, basename + ".jar");
		if ( exactFile.exists() ) return exactFile;;
		
		// find by basename-x.y.z.jar
		File[] list = path.listFiles();
		Pattern p = Pattern.compile(basename + "\\-\\d(\\.\\d+)+\\.jar");
		for(File file : list) {
			if (p.matcher(file.getName()).matches()) return file;
		}
		return null;
	}
	
//	/** By using the search paths, retrieve the path that contains the wsclient.jar
//	 * 
//	 * @return the path that contains the wsclient.jar, null if not found
//	 */
//	public static String findWSClientPath() {
//		Map<String, String> env = System.getenv();
//		String path = null;
//		for(String key : env.keySet()) {
//			// the key may be case sensitive on Unix
//			if ( "PATH".equalsIgnoreCase(key) ) {
//				path = env.get(key);
//				break;
//			}
//		}
//		String pathSep = System.getProperty("path.separator"); // ":" on Unix, ";" on Win
//		if ( null != path ) {
//			String[] array = path.split(Pattern.quote(pathSep)); // need to quote the pathSep coz it is metachar
//			for(String s : array ) {
//				String p = fortifyclientPath(new File(s));
//				if ( null != p ) return p;
//			}
//		}
//		return null;
//	}
//	
//	/** Check if the path is a Fortify Product path, and if yes, return the path that contains wsclient.jar
//	 * 
//	 * @param path a segment of the search PATH
//	 * @return the path that contains the wsclient.jar, null if it the path is not a F360 product path
//	 */
//	public static String fortifyclientPath(File path) {
//		if ( path.exists() && path.isDirectory() ) {
//			FilenameFilter filter = new FilenameFilter() {
//				@Override
//				public boolean accept(File dir, String name) {
//					int x = name.lastIndexOf(".");
//					String basename = name;
//					if ( x >= 0 ) {
//						basename = name.substring(0, x);
//					}
//					if ( basename.equalsIgnoreCase("fortifyclient") ) return true;
//					else return false;
//				}
//			}; 
//			String[] files = path.list(filter);
//			if ( null != files && files.length > 0 ) {
//				// ok, this path contains "fortifyclient"
//				// there can be two possibilities
//				// 1. the machine installed SCA, <SCA>\bin\fortifyclient, <SCA>\Core\lib\wsclient.jar
//				// 2. the machine installed F360 server Demo Suite and changed the search path
//				// <F360Demo>\Tools\fortifyclient\bin\fortifyclient, <F360Demo>\Tools\fortifyclient\Core\lib\wsclient.jar
//				String s = System.getProperty("file.separator");
//				File jar = new File(path, ".." + s + "Core" + s + "lib" + s + "wsclient.jar");
//				if ( jar.exists() ) return jar.getParentFile().getAbsolutePath();
//			}
//		}
//		return null;
//	}
}
