package io.anyway.puzzle.core.servlet.metadata;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import io.anyway.puzzle.core.PluginContext;

public class FilterMetadata {
	
	private String filterName;
	
	private String filterClass;
	
	private Hashtable<String, String> initParams = new Hashtable<String, String>();
	
	private List<String> urlPatterns= new ArrayList<String>();
	
	private PluginContext pluginContext;

	public FilterMetadata(String filterName,String filterClass,PluginContext pluginContext){
		this.filterName= filterName;
		this.filterClass= filterClass;
		this.pluginContext= pluginContext;
	}
	
	public void addInitParam(String name,String value){
		initParams.put(name, value);
	}
	public void addUrlPattern(String pattern){
		urlPatterns.add(pattern);
	}
	
	public String getFilterName(){
		return filterName;
	}
	
	public String getFilterClass(){
		return filterClass;
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
			.append(",filter-name= ")
			.append(filterName)
			.append(",filter-class= ")
			.append(filterClass)
			.append(",init-param= ")
			.append(initParams)
			.append(",url-pattern= ")
			.append(urlPatterns)
			.append("}");
		return buf.toString();
	}
}
