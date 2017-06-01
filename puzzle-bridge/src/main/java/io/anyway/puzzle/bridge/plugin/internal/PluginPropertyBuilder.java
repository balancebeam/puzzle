package io.anyway.puzzle.bridge.plugin.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Framework Property Builder 
 * @author yangzz
 *
 */
public class PluginPropertyBuilder {
	
	private Log logger = LogFactory.getLog(PluginPropertyBuilder.class);
	
	private ServletContext context;
	
	private String webrootpath;
	
	private Map<String,String> properties= new HashMap<String, String>(); 
	
	private List<URL> frameworkURLs = new ArrayList<URL>();
	
	private PluginFrameworkClassLoader frameworkClassLoader;
	
	public PluginPropertyBuilder(ServletContext context){
		this.context= context;
		webrootpath= context.getRealPath("/");
		if(webrootpath.endsWith(File.separator)){
			webrootpath= webrootpath.substring(0,webrootpath.length()-1);
		}
		logger.info("Framework Webroot path: "+webrootpath);
		initFrameworkProperties();
	}
	
	/**
	 * initialize system property
	 * @return java.util.Map<String,String>
	 */
	private void initFrameworkProperties(){
		Properties launchProperties= new Properties();
		InputStream in= context.getResourceAsStream("/WEB-INF/plugins/launch.ini");
		if(null== in){
			throw new IllegalArgumentException("OSGi Engin config property is not exists, please check /WEB-INF/plugins/launch.ini");
		}
		try {
			launchProperties.load(in);
		} catch (IOException e) {
			throw new IllegalArgumentException("/WEB-INF/plugins/launch.ini parsing error");
		}
		finally{
			if(null!= in){
				try {
					in.close();
				} catch (IOException e) {
					logger.error("/WEB-INF/plugins/launch.ini closing error");
				}
			}
		}
		for (Entry<Object, Object> each: launchProperties.entrySet()) {
			String key= String.valueOf(each.getKey());
			String value= String.valueOf(each.getValue());
			if(value.startsWith("/WEB-INF/")){
				value= webrootpath+ value;
			}
			properties.put(key,value);
		}
		
		
		//if no configuration dictionary, mkdir it
		File tmp= new File(properties.get("osgi.configuration.area"));
		if(!tmp.exists()){
			tmp.mkdir();
			logger.info("mkdir \"/WEB-INF/configuartion/\" dictionary: "+tmp.getAbsolutePath());
		}
		//get OSGi framework file
		List<String> bundles= new LinkedList<String>();
		List<String> debugs= new LinkedList<String>();
		File[] listFiles= new File(properties.get("osgi.install.area")).listFiles();
		if(null!= listFiles){
			for (File each : listFiles) {
				if(each.isDirectory() && !new File(each,"META-INF/MANIFEST.MF").exists()){
					continue;
				}
				String filename = each.getName();
				//找到OSGi包
				if (filename.startsWith("org.eclipse.osgi_")){
					try {
						frameworkURLs.add(each.toURI().toURL());
					} catch (MalformedURLException e) {
						throw new IllegalArgumentException(e.getMessage(),e);
					}
					properties.put("osgi.framework", "file:"+each.getAbsolutePath());
					continue;
				}
				//添加扩展包
				if(filename.startsWith("io.anayway.puzzle.core_")) {
					if(!properties.containsKey("osgi.framework.extensions")){
						properties.put("osgi.framework.extensions", "reference:file:"+each.getName());
					}
					continue;
				}
				if(each.isFile()){
					//添加调试插件
					if(each.getName().equals("debug.cfg")){
						Properties debugPlugins= new LinkedProperties();
						InputStream fin= null;
						try {
							fin = new FileInputStream(each);
							debugPlugins.load(fin);
						} catch (Exception e) {
							logger.warn("load debug.cfg error", e);
							continue;
						}
						finally{
							if(fin!= null){
								try {
									fin.close();
								} catch (IOException e) {
								}
							}
						}
						for(Enumeration<Object> it= debugPlugins.keys();it.hasMoreElements();){
							String key = (String)it.nextElement();
							String fname= debugPlugins.getProperty(key);
							if(new File(fname).exists()){
								debugs.add(fname);
							}
							else{
								logger.warn("File \""+fname+"\" is not exist");
							}
						}
						logger.info("debug.bundles: "+debugs);
						continue;
					}
					if(!each.getName().endsWith(".jar")){
						continue;
					}
				}
				bundles.add(each.getName());
			}
		}
		
		if(!properties.containsKey("osgi.framework")){
			throw new IllegalArgumentException("Miss osgi.framework");
		}
		if(!properties.containsKey("osgi.framework.extensions")){
			throw new IllegalArgumentException("Miss osgi.framework.extensions");
		}
		bundles.addAll(debugs);
		
		logger.info("osgi.bundles: "+bundles);
		StringBuilder builder= new StringBuilder();
		builder.append("reference:file:")
			.append(bundles.remove(0))
			.append("@start");
		
		for(String each: bundles){
			builder.append(",reference:file:")
			.append(each)
			.append("@start");
		}
		properties.put("osgi.bundles", builder.toString());
	}
	
	public Map<String,String> getFrameworkProperties(){
		return properties;
	}
	
	public synchronized PluginFrameworkClassLoader getFrameworkClassLoader(){
		if(null==frameworkClassLoader){
			frameworkClassLoader= new PluginFrameworkClassLoader(frameworkURLs.toArray(new URL[frameworkURLs.size()]), getClass().getClassLoader());
		}
		return frameworkClassLoader;
	}

	private static final String WS_DELIM = " \t\n\r\f";
	/**
	 * buildCommandLineArguments parses the commandline config parameter into a set of arguments 
	 * @return an array of String containing the commandline arguments
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String[] buildCommandArguments() {
		List args = new ArrayList();
		String command= null;
		if(command==null || "".equals(command)){
			command= properties.get("osgi.command");
		}
		if (command!= null) {
			StringTokenizer tokenizer = new StringTokenizer(command, WS_DELIM);
			while (tokenizer.hasMoreTokens()) {
				String arg = tokenizer.nextToken();
				if (arg.startsWith("\"")) { //$NON-NLS-1$
					if (arg.endsWith("\"")) { //$NON-NLS-1$ 
						if (arg.length() >= 2) {
							// strip the beginning and ending quotes 
							arg = arg.substring(1, arg.length() - 1);
						}
					} else {
						String remainingArg = tokenizer.nextToken("\""); //$NON-NLS-1$
						arg = arg.substring(1) + remainingArg;
						// skip to next whitespace separated token
						tokenizer.nextToken(WS_DELIM);
					}
				} else if (arg.startsWith("'")) { //$NON-NLS-1$
					if (arg.endsWith("'")) { //$NON-NLS-1$ 
						if (arg.length() >= 2) {
							// strip the beginning and ending quotes 
							arg = arg.substring(1, arg.length() - 1);
						}
					} else {
						String remainingArg = tokenizer.nextToken("'"); //$NON-NLS-1$
						arg = arg.substring(1) + remainingArg;
						// skip to next whitespace separated token
						tokenizer.nextToken(WS_DELIM);
					}
				} else if (arg.startsWith("-D")) { //$NON-NLS-1$
					int matchIndex = arg.indexOf("=\""); //$NON-NLS-1$
					if (matchIndex != -1) {
						if (!arg.substring(matchIndex + 2).endsWith("\"") && tokenizer.hasMoreTokens()) { //$NON-NLS-1$
							arg += tokenizer.nextToken("\"") + "\""; //$NON-NLS-1$ //$NON-NLS-2$
							// skip to next whitespace separated token
							tokenizer.nextToken(WS_DELIM);
						}
					}
				}
				args.add(arg);
			}
		}
		return (String[]) args.toArray(new String[] {});
	}
	public static void main(String[] args){
		String file= "file:/Users/yangzz/dump.rdb";
		File f= new File(file);
		System.out.println(f.exists());
	}
}
