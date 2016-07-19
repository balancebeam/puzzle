package io.anyway.puzzle.core.internal;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import io.anyway.puzzle.core.common.Constants;
import io.anyway.puzzle.core.spring.PluginSpringManager;
import org.eclipse.osgi.container.ModuleLoader;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.osgi.framework.Bundle;
import org.springframework.context.ApplicationContext;

import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.servlet.PluginServletManager;

public class PluginContextSupport implements PluginContext{
	
	private Bundle bundle;
	
	private int state;
	
	private ApplicationContext applicationContext;
	
	private PluginServletManager servletManager;
	
	private PluginSpringManager springManager;
	
	private Map<String,Object> attributes= Collections.<String,Object>emptyMap();
	
	public PluginContextSupport(Bundle bundle){
		this.bundle= bundle;
	}

	@Override
	public String getName() {
		return bundle.getSymbolicName();
	}

	@Override
	public String getVersion() {
		return bundle.getVersion().toString();
	}

	@Override
	public String getUnique() {
		return getName()+ Constants.SEMICOLIN_VERSION+getVersion();
	}

	@Override
	public String getAlias() {
		return getManifest().get(Constants.BUNDLE_ALIAS);
	}

	@Override
	public Dictionary<String, String> getManifest() {
		return bundle.getHeaders();
	}

	@Override
	public int getState() {
		return state;
	}
	
	void setState(int state){
		this.state= state;
	}

	@Override
	public ModuleClassLoader getClassLoader() {
		return AccessController.doPrivileged(new PrivilegedAction<ModuleClassLoader>() {
			@Override
			public ModuleClassLoader run() {
				ModuleWiring wiring = ((EquinoxBundle)bundle).getModule().getCurrentRevision().getWiring();
				if (wiring != null) {
					ModuleLoader moduleLoader = wiring.getModuleLoader();
					if (moduleLoader instanceof BundleLoader) {
						return ((BundleLoader) moduleLoader).getModuleClassLoader();
					}
				}
				return null;
			}
		});
	}

	@Override
	public Class<?> findLocalClass(String name) throws ClassNotFoundException {
		return getClassLoader()!=null? getClassLoader().findLocalClass(name): null;
	}

	@Override
	public URL findLocalResource(String resource) {
		return getClassLoader()!=null?getClassLoader().findLocalResource(resource): null;
	}

	@Override
	public Collection<URL> getResourcePaths(String path) {
		if(getClassLoader()!=null){
			Enumeration<URL> enumeration= getClassLoader().findLocalResources(path);
			if(enumeration!= null){
				List<URL> result= new LinkedList<URL>();
				for(;enumeration.hasMoreElements();){
					result.add(enumeration.nextElement());
				}
				return result;
			}
		}
		return Collections.<URL>emptyList();
	}

	@Override
	public ServletConfig getServletConfig() {
		return servletManager.getServletConfig();
	}
	
	@Override
	public ServletContext getServletContext() {
		return servletManager.getServletContext();
	}
	
	@Override
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
	
	public void setApplicationContext(ApplicationContext applicationContext){
		this.applicationContext= applicationContext;
	}

	@Override
	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	@Override
	public void setAttribute(String key, Object value) {
		if(attributes==Collections.<String,Object>emptyMap()){
			attributes= new HashMap<String,Object>();
		}
	}

	@Override
	public void removeAttribute(String key) {
		attributes.remove(key);
	}

	@Override
	public Collection<String> getAttributeKeys() {
		return attributes.keySet();
	}

	@Override
	public boolean containAttributeKey(String key) {
		return attributes.containsKey(key);
	}
	
	@Override
	public Bundle getBundle(){
		return bundle;
	}
	
	public void setServletManager(PluginServletManager servletManager){
		this.servletManager= servletManager;
	}
	
	public PluginServletManager getServletManager(){
		return servletManager;
	}
	
	public void setSpringManager(PluginSpringManager springManager){
		this.springManager= springManager;
	}
	
	public PluginSpringManager getSpringManager(){
		return springManager;
	}
	
	public void destroy(){
		if(springManager!=null){
			springManager.removeServiceListener();
		}
		if(servletManager!= null){
			servletManager.destroy();
		}
		if(springManager!=null){
			springManager.destroy();
		}
	}
	@Override
	public String toString(){
		return "Plugin["+getName()+"_"+getVersion()+"]";
	}
	
}
