package io.anyway.puzzle.core.servlet.metadata;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import io.anyway.puzzle.core.PluginContext;

public class ServletMetadata {

	private String servletName;

	private String servletClass;

	private int startup= -1;

	private Hashtable<String, String> initParams = new Hashtable<String, String>();

	private List<String> urlPatterns= new ArrayList<String>();
	
	private PluginContext pluginContext;
	
	public ServletMetadata(String servletName,String servletClass,PluginContext pluginContext){
		this.servletName= servletName;
		this.servletClass= servletClass;
		this.pluginContext= pluginContext;
	}
	
	public void setStartup(int startup){
		this.startup= startup;
	}
	
	public void addInitParam(String name,String value){
		initParams.put(name, value);
	}
	
	public void addUrlPattern(String pattern){
		urlPatterns.add(pattern);
	}
	
	public String getServletName(){
		return servletName;
	}
	
	public String getServletClass(){
		return servletClass;
	}
	
	public int getStartup(){
		return startup;
	}
	
	public Hashtable<String,String> getInitParams(){
		return initParams;
	}
	
	public List<String> getPatterns(){
		return urlPatterns;
	}
	
	public PluginContext getPluginContext(){
		return pluginContext;
	}
	
	public String toString(){
		StringBuffer buf= new StringBuffer();
		buf.append("{")
			.append("pluginName= ")
			.append(pluginContext.getName())
			.append(",servlet-name= ")
			.append(servletName)
			.append(",servlet-class= ")
			.append(servletClass)
			.append(",load-on-startup= ")
			.append(startup)
			.append(",init-param= ")
			.append(initParams)
			.append(",servlet-mapping= ")
			.append(urlPatterns)
			.append("}");
		return buf.toString();
	}
}
