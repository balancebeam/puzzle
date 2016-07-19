package io.anyway.puzzle.jasper.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.ServletContext;

import org.apache.jasper.Constants;
import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.compiler.TldLocationsCache;
import org.apache.jasper.xmlparser.ParserUtils;
import org.apache.jasper.xmlparser.TreeNode;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.storage.url.BundleURLConnection;
import org.xml.sax.InputSource;

import io.anyway.puzzle.bridge.plugin.PluginFramework;
import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.internal.PluginRepository;


/**
 * 为支持bundle内部定义taglib定制此类，配合PluginServletOptions使用。
 * 扩展了init方法，增加了scanPlugins方法，扫描bundle内的taglib描述文件
 * 修改init方法，支持组件的动态化，即能够伴随着组件的新增、更新以及移除，变化已有的Taglib定义
 * @author zhaob
 * @author zhangwb
 */
@SuppressWarnings("unchecked")
public class PluginTldLocationsCache extends TldLocationsCache {

	// Logger
    private Log logger = LogFactory.getLog(PluginTldLocationsCache.class);

    /**
     * The types of URI one may specify for a tag library
     */
    public static final int ABS_URI = 0;
    public static final int ROOT_REL_URI = 1;
    public static final int NOROOT_REL_URI = 2;

    private static final String WEB_XML = "/WEB-INF/web.xml";
    private static final String FILE_PROTOCOL = "file:";
    private static final String JAR_FILE_SUFFIX = ".jar";

    // Names of JARs that are known not to contain any TLDs
    private static HashSet<String> noTldJars;

    /**
     * The mapping of the 'global' tag library URI to the location (resource
     * path) of the TLD associated with that tag library. The location is
     * returned as a String array:
     *    [0] The location
     *    [1] If the location is a jar file, this is the location of the tld.
     */
    private Hashtable<String, String[]> mappings;
    
    private boolean initialized;
    private ServletContext ctxt;
    private boolean redeployMode;

    //*********************************************************************
    // Constructor and Initilizations

    /*
     * Initializes the set of JARs that are known not to contain any TLDs
     */
    static {
        noTldJars = new HashSet<String>();
        // Bootstrap JARs
        noTldJars.add("bootstrap.jar");
        noTldJars.add("commons-daemon.jar");
        noTldJars.add("tomcat-juli.jar");
        // Main JARs
        noTldJars.add("annotations-api.jar");
        noTldJars.add("catalina.jar");
        noTldJars.add("catalina-ant.jar");
        noTldJars.add("catalina-ha.jar");
        noTldJars.add("catalina-tribes.jar");
        noTldJars.add("el-api.jar");
        noTldJars.add("jasper.jar");
        noTldJars.add("jasper-el.jar");
        noTldJars.add("ecj-3.7.jar");
        noTldJars.add("jsp-api.jar");
        noTldJars.add("servlet-api.jar");
        noTldJars.add("tomcat-coyote.jar");
        noTldJars.add("tomcat-dbcp.jar");
        // i18n JARs
        noTldJars.add("tomcat-i18n-en.jar");
        noTldJars.add("tomcat-i18n-es.jar");
        noTldJars.add("tomcat-i18n-fr.jar");
        noTldJars.add("tomcat-i18n-ja.jar");
        // Misc JARs not included with Tomcat
        noTldJars.add("ant.jar");
        noTldJars.add("commons-dbcp.jar");
        noTldJars.add("commons-beanutils.jar");
        noTldJars.add("commons-fileupload-1.0.jar");
        noTldJars.add("commons-pool.jar");
        noTldJars.add("commons-digester.jar");
        noTldJars.add("commons-loggerging.jar");
        noTldJars.add("commons-collections.jar");
        noTldJars.add("jmx.jar");
        noTldJars.add("jmx-tools.jar");
        noTldJars.add("xercesImpl.jar");
        noTldJars.add("xmlParserAPIs.jar");
        noTldJars.add("xml-apis.jar");
        // JARs from J2SE runtime
        noTldJars.add("sunjce_provider.jar");
        noTldJars.add("ldapsec.jar");
        noTldJars.add("localedata.jar");
        noTldJars.add("dnsns.jar");
        noTldJars.add("tools.jar");
        noTldJars.add("sunpkcs11.jar");
    }
    
    public PluginTldLocationsCache(ServletContext ctxt) {
        this(ctxt, true);
    }

    /** Constructor. 
     *
     * @param ctxt the servlet context of the web application in which Jasper 
     * is running
     * @param redeployMode if true, then the compiler will allow redeploying 
     * a tag library from the same jar, at the expense of slowing down the
     * server a bit. Note that this may only work on JDK 1.3.1_01a and later,
     * because of JDK bug 4211817 fixed in this release.
     * If redeployMode is false, a faster but less capable mode will be used.
     */
    public PluginTldLocationsCache(ServletContext ctxt, boolean redeployMode) {
    	super(ctxt, redeployMode);
    	this.ctxt = ctxt;
        this.redeployMode = redeployMode;
        mappings = new Hashtable<String, String[]>();
        initialized = false;
    }

    /**
     * Sets the list of JARs that are known not to contain any TLDs.
     *
     * @param jarNames List of comma-separated names of JAR files that are 
     * known not to contain any TLDs 
     */
    public static void setNoTldJars(String jarNames) {
        if (jarNames != null) {
            noTldJars.clear();
            StringTokenizer tokenizer = new StringTokenizer(jarNames, ",");
            while (tokenizer.hasMoreElements()) {
                noTldJars.add(tokenizer.nextToken());
            }
        }
    }

    /**
     * Gets the 'location' of the TLD associated with the given taglib 'uri'.
     *
     * Returns null if the uri is not associated with any tag library 'exposed'
     * in the web application. A tag library is 'exposed' either explicitly in
     * web.xml or implicitly via the uri tag in the TLD of a taglib deployed
     * in a jar file (WEB-INF/lib).
     * 
     * @param uri The taglib uri
     *
     * @return An array of two Strings: The first element denotes the real
     * path to the TLD. If the path to the TLD points to a jar file, then the
     * second element denotes the name of the TLD entry in the jar file.
     * Returns null if the uri is not associated with any tag library 'exposed'
     * in the web application.
     */
    public String[] getLocation(String uri) throws JasperException {
        if (!initialized) {
            init();//如果没有初始化的话，需要进行初始化
        }
        
        //查找组件的tld
        Collection<PluginContext> list= PluginRepository.getInstance().getAllPluginContext();
        try {
	        for(PluginContext each: list){
				Hashtable<String, String[]>  tldMapping= (Hashtable<String, String[]>)each.getAttribute("tld-mapping");
				//证明没有收集组件tld文件
				if(tldMapping==null){
					//初始化一次组件的tld文件放到自定义属性tld-mapping对应的对象里
					processPluginTld(each);
					tldMapping= (Hashtable<String, String[]>)each.getAttribute("tld-mapping");
				}			
	        	if(tldMapping.containsKey(uri)){
	 	        	return tldMapping.get(uri);
	 	        }
	        }
        } catch (Exception e) {
        	logger.error(e.getMessage(),e);
        }
        
        return (String[]) mappings.get(uri);
    }

    /** 
     * Returns the type of a URI:
     *     ABS_URI
     *     ROOT_REL_URI
     *     NOROOT_REL_URI
     */
    public static int uriType(String uri) {
        if (uri.indexOf(':') != -1) {
            return ABS_URI;
        } else if (uri.startsWith("/")) {
            return ROOT_REL_URI;
        } else {
            return NOROOT_REL_URI;
        }
    }

    private void init() throws JasperException {
    	
        if (initialized) return;
        try {
            processWebDotXml();
            scanJars();
            processTldsInFileSystem("/WEB-INF/");
            initialized = true;
        } catch (Exception ex) {
            throw new JasperException(Localizer.getMessage(
                    "jsp.error.internal.tldinit", ex.getMessage()));
        }
    }
    
   // private void 
    
    
    /*
     * Populates taglib map described in web.xml.
     */    
    private void processWebDotXml() throws Exception {
        InputStream is = null;
        try {
            // Acquire input stream to web application deployment descriptor
            String altDDName = (String)ctxt.getAttribute(Constants.ALT_DD_ATTR);
            URL uri = null;
            if (altDDName != null) {
                try {
                    uri = new URL(FILE_PROTOCOL+altDDName.replace('\\', '/'));
                } catch (MalformedURLException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn(Localizer.getMessage(
                                            "jsp.error.internal.filenotfound",
                                            altDDName));
                    }
                }
            } else {
                uri = ctxt.getResource(WEB_XML);
                if (uri == null && logger.isWarnEnabled()) {
                    logger.warn(Localizer.getMessage(
                                            "jsp.error.internal.filenotfound",
                                            WEB_XML));
                }
            }

            if (uri == null) {
                return;
            }
            is = uri.openStream();
            InputSource ip = new InputSource(is);
            ip.setSystemId(uri.toExternalForm()); 

            // Parse the web application deployment descriptor
            TreeNode webtld = null;
            // altDDName is the absolute path of the DD
            if (altDDName != null) {
                webtld = new ParserUtils().parseXMLDocument(altDDName, ip);
            } else {
                webtld = new ParserUtils().parseXMLDocument(WEB_XML, ip);
            }

            // Allow taglib to be an element of the root or jsp-config (JSP2.0)
            TreeNode jspConfig = webtld.findChild("jsp-config");
            if (jspConfig != null) {
                webtld = jspConfig;
            }
            Iterator<?> taglibs = webtld.findChildren("taglib");
            while (taglibs.hasNext()) {

                // Parse the next <taglib> element
                TreeNode taglib = (TreeNode) taglibs.next();
                String tagUri = null;
                String tagLoc = null;
                TreeNode child = taglib.findChild("taglib-uri");
                if (child != null)
                    tagUri = child.getBody();
                child = taglib.findChild("taglib-location");
                if (child != null)
                    tagLoc = child.getBody();

                // Save this location if appropriate
                if (tagLoc == null)
                    continue;
                if (uriType(tagLoc) == NOROOT_REL_URI)
                    tagLoc = "/WEB-INF/" + tagLoc;
                String tagLoc2 = null;
                if (tagLoc.endsWith(JAR_FILE_SUFFIX)) {
                    tagLoc = ctxt.getResource(tagLoc).toString();
                    tagLoc2 = "META-INF/taglib.tld";
                }
                mappings.put(tagUri, new String[] { tagLoc, tagLoc2 });
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable t) {}
            }
        }
    }

    /**
     * Scans the given JarURLConnection for TLD files located in META-INF
     * (or a subdirectory of it), adding an implicit map entry to the taglib
     * map for any TLD that has a <uri> element.
     *
     * @param conn The JarURLConnection to the JAR file to scan
     * @param ignore true if any exceptions raised when processing the given
     * JAR should be ignored, false otherwise
     */
	private void scanJar(JarURLConnection conn, boolean ignore,PluginContext ctx)
                throws JasperException {

        JarFile jarFile = null;
        String resourcePath = conn.getJarFileURL().toString();
        try {
            if (redeployMode) {
                conn.setUseCaches(false);
            }
            jarFile = conn.getJarFile();
            Enumeration<?> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = (JarEntry) entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("META-INF/")){
                	continue;
                }
                if (!name.endsWith(".tld")) continue;
                InputStream stream = jarFile.getInputStream(entry);
                try {
                    String uri = getUriFromTld(resourcePath, stream);
                    // Add implicit map entry only if its uri is not already
                    // present in the map
                    if (uri != null&&ctx!=null) {
                    	Map<String,String[]> tldMapping= (Map<String,String[]>)ctx.getAttribute("tld-mapping");
                    	tldMapping.put(uri, new String[] {resourcePath, name});
                    }else if (uri != null && mappings.get(uri) == null) {
                        mappings.put(uri, new String[]{ resourcePath, name });
                    }
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (Throwable t) {
                            // do nothing
                        }
                    }
                }
            }
        } catch (Exception ex) {
            if (!redeployMode) {
                // if not in redeploy mode, close the jar in case of an error
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (Throwable t) {
                        // ignore
                    }
                }
            }
            if (!ignore) {
                throw new JasperException(ex);
            }
        } finally {
            if (redeployMode) {
                // if in redeploy mode, always close the jar
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (Throwable t) {
                        // ignore
                    }
                }
            }
        }
    }

    /*
     * Searches the filesystem under /WEB-INF for any TLD files, and adds
     * an implicit map entry to the taglib map for any TLD that has a <uri>
     * element.
     */
    private void processTldsInFileSystem(String startPath)
            throws Exception {
    	if (startPath.startsWith("/WEB-INF/framework")|| startPath.startsWith("/WEB-INF/components"))
    		return;
        Set<?> dirList = ctxt.getResourcePaths(startPath);
        if (dirList != null) {
            Iterator<?> it = dirList.iterator();
            while (it.hasNext()) {
                String path = (String) it.next();
                if (path.endsWith("/")) {
                    processTldsInFileSystem(path);
                }
                if (!path.endsWith(".tld")) {
                    continue;
                }
                InputStream stream = ctxt.getResourceAsStream(path);
                String uri = null;
                try {
                    uri = getUriFromTld(path, stream);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (Throwable t) {
                            // do nothing
                        }
                    }
                }
                // Add implicit map entry only if its uri is not already
                // present in the map
                if (uri != null && mappings.get(uri) == null) {
                    mappings.put(uri, new String[] { path, null });
                }
            }
        }
    }

    /*
     * Returns the value of the uri element of the given TLD, or null if the
     * given TLD does not contain any such element.
     */
    private String getUriFromTld(String resourcePath, InputStream in) 
        throws JasperException
    {
        // Parse the tag library descriptor at the specified resource path
        TreeNode tld = new ParserUtils().parseXMLDocument(resourcePath, in);
        TreeNode uri = tld.findChild("uri");
        if (uri != null) {
            String body = uri.getBody();
            if (body != null)
                return body;
        }

        return null;
    }

    /*
     * Scans all JARs accessible to the webapp's classloader and its
     * parent classloaders for TLDs.
     * 
     * The list of JARs always includes the JARs under WEB-INF/lib, as well as
     * all shared JARs in the classloader delegation chain of the webapp's
     * classloader.
     *
     * Considering JARs in the classloader delegation chain constitutes a
     * Tomcat-specific extension to the TLD search
     * order defined in the JSP spec. It allows tag libraries packaged as JAR
     * files to be shared by web applications by simply dropping them in a 
     * location that all web applications have access to (e.g.,
     * <CATALINA_HOME>/common/lib).
     *
     * The set of shared JARs to be scanned for TLDs is narrowed down by
     * the <tt>noTldJars</tt> class variable, which contains the names of JARs
     * that are known not to contain any TLDs.
     */
    private void scanJars() throws Exception {

        ClassLoader webappLoader = PluginFramework.class.getClassLoader() ;
        ClassLoader loader = webappLoader;
        
        Method m = ClassLoader.class.getDeclaredMethod("findResources", new Class[]{String.class});
        m.setAccessible(true);
		Enumeration<URL> e =(Enumeration<URL>)m.invoke(loader, new Object[]{"META-INF"});
        
        for(;e.hasMoreElements();){
        	URL url = e.nextElement();
        	String filename = url.getFile();
        	if(filename.endsWith("!/META-INF")){
        		JarURLConnection conn = (JarURLConnection)new URL("jar:"+(filename.startsWith("file")?"":"file:")+filename.replace("META-INF", "")).openConnection();
        		if (needScanJar(loader, webappLoader,
        				((JarURLConnection) conn).getJarFile().getName())) {
        			scanJar((JarURLConnection) conn, true,null);
        		}
        	}
        }
    }
    
    /*
     * Determines if the JAR file with the given <tt>jarPath</tt> needs to be
     * scanned for TLDs.
     *
     * @param loader The current classloader in the parent chain
     * @param webappLoader The webapp classloader
     * @param jarPath The JAR file path
     *
     * @return TRUE if the JAR file identified by <tt>jarPath</tt> needs to be
     * scanned for TLDs, FALSE otherwise
     */
    private boolean needScanJar(ClassLoader loader, ClassLoader webappLoader,
                                String jarPath) {
        if (loader == webappLoader) {
            // JARs under WEB-INF/lib must be scanned unconditionally according
            // to the spec.
            return true;
        } else {
            String jarName = jarPath;
            int slash = jarPath.lastIndexOf('/');
            if (slash >= 0) {
                jarName = jarPath.substring(slash + 1);
            }
            return (!noTldJars.contains(jarName));
        }
    }
    
   
    /*
     * 扫描组件中tld定义
     */
    private void processPluginTld(PluginContext ctx) throws Exception {
    	//为每个组件都创建一个tldmapping对象，存放tld的定义
    	ctx.setAttribute("tld-mapping", new HashMap<String,String[]>());
    	//解析组件的web.xml的tld配置
    	processPluginWebDotXml(ctx);
    	String location= ((EquinoxBundle)ctx.getBundle()).getModule().getLocation();
    	if (location.endsWith(".jar")) {
    		URL url = ctx.getClassLoader().getResource("META-INF");
    		BundleURLConnection conn = (BundleURLConnection) url.openConnection();
    		JarURLConnection jarUrlConn = (JarURLConnection) conn.getLocalURL().openConnection();
    		JarFile jarFile = jarUrlConn.getJarFile();
    		String resourcePath = jarUrlConn.getJarFileURL().toString();
            Enumeration<?> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = (JarEntry) entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("META-INF/")){
                	continue;
                }
                if (!name.endsWith(".tld")) continue;
                InputStream stream = jarFile.getInputStream(entry);
                try {
                    String uri = getUriFromTld(resourcePath, stream);
                    if (uri != null) {
                    	Map<String,String[]> tldMapping= (Map<String,String[]>)ctx.getAttribute("tld-mapping");
                    	tldMapping.put(uri, new String[] {resourcePath, name});
                    }
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (Throwable t) {
                            // do nothing
                        }
                    }
                }
            }
    	} else {
    		String metainfo = "META-INF";
    		File file= new File(location.substring(location.lastIndexOf(":")+1));
    		visitEachFile(new File(file, metainfo),ctx);
    	}
    	ModuleClassLoader cl = (ModuleClassLoader)ctx.getClassLoader();
		for(ClasspathEntry each : cl.getClasspathManager().getHostClasspathEntries()){
			File f = each.getBundleFile().getBaseFile();
			if(f.isFile()){
				URL url = new URL("jar:file://"+f.toURI().getPath().replace("\\", "/")+"!/");
				scanJar((JarURLConnection) url.openConnection(),true,ctx);
			}
		}
		if(logger.isDebugEnabled()){
      		logger.debug("完成解析插件"+ctx.getName()+"classpath中的tld配置");
      	}
    }
    //处理组件中web.xml中tld定义
    private void  processPluginWebDotXml(PluginContext ctx) throws Exception{
  	  InputStream is = null;
        try {
      	  URL urlDefault = ctx.findLocalResource("WEB-INF/web.xml");
      	  if (urlDefault == null) {
                return;
            }
            is = urlDefault.openStream();
            InputSource ip = new InputSource(is);
            ip.setSystemId(urlDefault.toExternalForm()); 
            // Parse the web application deployment descriptor
            TreeNode webtld = null;
            webtld = new ParserUtils().parseXMLDocument(WEB_XML, ip);
            // Allow taglib to be an element of the root or jsp-config (JSP2.0)
            TreeNode jspConfig = webtld.findChild("jsp-config");
            if (jspConfig != null) {
                webtld = jspConfig;
            }
            Iterator<?> taglibs = webtld.findChildren("taglib");
            while (taglibs.hasNext()) {
                // Parse the next <taglib> element
                TreeNode taglib = (TreeNode) taglibs.next();
                String tagUri = null;
                String tagLoc = null;
                TreeNode child = taglib.findChild("taglib-uri");
                if (child != null)
                    tagUri = child.getBody();
                child = taglib.findChild("taglib-location");
                if (child != null)
                  tagLoc = child.getBody();
                  String resourcePath ="/"+ctx.getName()+tagLoc;
                  Map<String,String[]> tldMapping= (Map<String,String[]>)ctx.getAttribute("tld-mapping");
              	  tldMapping.put(tagUri, new String[] {resourcePath, tagLoc});
              	if(logger.isDebugEnabled()){
              		logger.debug("完成解析组件"+ctx.getName()+"web.xml中的tld配置");
              	}
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable t) {}
            }
        }
  }
    //递归遍历每个文件
    private void visitEachFile(File f,PluginContext ctx) throws JasperException, IOException{
    	File[] files = f.listFiles();
    	if(null== files){
    		return;
    	}
    	for(File each : files){
    		if (each.getName().endsWith(".tld")){
    			String name = each.getName();
    			String relativeName = each.getPath();
    			relativeName = relativeName.substring(relativeName.indexOf("META-INF")).replace("\\", "/");
    			URL url = ctx.findLocalResource(relativeName);
    			String resourcePath ="/"+ ctx.getName()+relativeName;
    			String uri = getUriFromTld(resourcePath, url.openStream());
                if (uri != null) {
                	Map<String,String[]> tldMapping= (Map<String,String[]>)ctx.getAttribute("tld-mapping");
                	tldMapping.put(uri, new String[] {resourcePath, name});
                }
    			continue;
    		}
    		if(each.isDirectory()){
    			visitEachFile(each,ctx);
    		}
    	}
    }
    
}
