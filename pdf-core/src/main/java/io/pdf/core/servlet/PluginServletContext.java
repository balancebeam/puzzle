package io.pdf.core.servlet;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import io.pdf.bridge.plugin.PluginFramework;
import io.pdf.core.PluginContext;

public class PluginServletContext implements ServletContext{

	private ServletContext delegate;
	
	private Hashtable<String, String>contextparams;
	
	private Hashtable<String, Object> attributes = new Hashtable<String, Object>();
	
	private PluginContext pluginContext;
	
	public PluginServletContext(PluginContext pluginContext) {
		this(pluginContext,new Hashtable<String, String>());
	}
	
	public PluginServletContext(PluginContext pluginContext,Hashtable<String, String>contextparams){
		this.pluginContext= pluginContext;
		this.contextparams= contextparams;
		this.delegate= PluginFramework.getFrameworkServletContext();
	}
	
	public String getContextPath() {
		return delegate.getContextPath();
	}
	
	public ServletContext getContext(String uripath) {
		return delegate.getContext(uripath);
	}

	public int getMajorVersion() {
		return delegate.getMajorVersion();
	}
	
	public int getMinorVersion() {
		return delegate.getMinorVersion();
	}
	
	public String getMimeType(String file) {
		return delegate.getMimeType(file);
	}
	
	public Set<String> getResourcePaths(String path) {
		Collection<URL> resourcePaths= pluginContext.getResourcePaths(path);
		if(resourcePaths!= null){
			Set<String> result= new LinkedHashSet<String>();
			for(URL each: resourcePaths){
				result.add(each.getFile());
			}
			return result;
		}
		return Collections.<String>emptySet();
	}
	
	public URL getResource(String path) throws MalformedURLException {
		return pluginContext.findLocalResource(path);
	}
	
	public InputStream getResourceAsStream(String path) {
		try {
			URL url = getResource(path);
			if(url!=null){
				return url.openStream();
			}
		} catch (Exception e) {}
		return null;
	}
	
	public ServletContext getDelegate(){
		return delegate;
	}
	
	public RequestDispatcher getRequestDispatcher(String path) {
		return delegate.getRequestDispatcher(path);
	}
	
	public RequestDispatcher getNamedDispatcher(String name) {
		return delegate.getNamedDispatcher(name);
	}

	@SuppressWarnings("deprecation")
	public Servlet getServlet(String name) throws ServletException {
		return  delegate.getServlet(name);
	}
	
	@SuppressWarnings("deprecation")
	public Enumeration<?> getServlets() {
		return delegate.getServlets();
	}
	
	@SuppressWarnings("deprecation")
	public Enumeration<?> getServletNames() {
		return delegate.getServletNames();
	}

	public void log(String msg) {
		delegate.log(msg);
	}

	@SuppressWarnings("deprecation")
	public void log(Exception exception, String msg) {
		delegate.log(exception, msg);
	}

	public void log(String message, Throwable throwable) {
		delegate.log(message, throwable);
	}

	public String getRealPath(String path) {
		return delegate.getRealPath(path);
	}

	public String getServerInfo() {
		return delegate.getServerInfo();
	}
	
	public String getInitParameter(String name) {
		String value= contextparams.get(name);
		if(null==value){
			value = delegate.getInitParameter(name);
		}
		return value;
	}

	
	public Enumeration<String> getInitParameterNames() {
		Hashtable<String,String> tmp = new Hashtable<String,String>();
		@SuppressWarnings("unchecked")
		Enumeration<String>  enumeration=delegate.getInitParameterNames();
		if(null!=enumeration){
			for(;enumeration.hasMoreElements();){
				tmp.put(enumeration.nextElement(),"");
			}
		}
		tmp.putAll(contextparams);
		return tmp.keys();
	}

	public Object getAttribute(String name) {
		Object object = attributes.get(name);
		return object;
	}

	public Enumeration<String> getAttributeNames() {
		return attributes.keys();
	}

	public void setAttribute(String name, Object object) {
		attributes.put(name, object);
	}

	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	public String getServletContextName() {
		return delegate.getServletContextName();
	}
	
	public PluginContext getPluginContext(){
		return pluginContext;
	}
	
	public String toString(){
		StringBuffer buf= new StringBuffer();
		buf.append("{")
			.append("pluginName= ")
			.append(pluginContext.getName())
			.append(",context-param= ")
			.append(contextparams)
			.append(",attributes= ")
			.append(attributes)
			.append("}");
		return buf.toString();
	}
}
