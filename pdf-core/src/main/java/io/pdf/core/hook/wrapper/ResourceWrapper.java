package io.pdf.core.hook.wrapper;

import java.net.URL;

import javax.servlet.ServletContext;

import io.pdf.core.PluginContext;

public class ResourceWrapper {
	
	private String resource;
	
	private URL url;
	
	private PluginContext pluginContext;
	
	public ResourceWrapper(PluginContext pluginContext,String resource){
		this(pluginContext,resource,pluginContext.findLocalResource(resource));
	}
	
	public ResourceWrapper(PluginContext pluginContext,String resource,URL url){
		this.pluginContext= pluginContext;
		this.resource= resource;
		this.url= url;
	}

	public String getResourcePath(){
		return resource;
	}

	public String getAbsoluteResourcePath(){
		return "/".concat(pluginContext.getName()).concat(resource);
	}

	public URL getResource(){
		return url;
	}

	public PluginContext getPluginContext(){
		return pluginContext;
	}

	public ServletContext getServletContext(){
		return pluginContext.getServletContext();
	}
	
	public String toString(){
		StringBuffer buf= new StringBuffer();
		buf.append("{pluginName= ")
			.append(pluginContext.getName())
			.append(",resource= ")
			.append(getResourcePath())
			.append(",absoluteResourcePath= ")
			.append(getAbsoluteResourcePath())
			.append("}");
		return buf.toString();
	}
}
