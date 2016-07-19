package io.anyway.puzzle.core.servlet;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

public class PluginFilterConfig implements FilterConfig{

	private String filterName;
	
	private ServletContext servletContext;
	
	private Hashtable<String, String> initParams;
	
	public PluginFilterConfig(String filterName,ServletContext servletContext,Hashtable<String, String> initParams){
		this.filterName= filterName;
		this.servletContext= servletContext;
		this.initParams= initParams;
	}
	
	public String getFilterName() {
		return filterName;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public String getInitParameter(String name) {
		return initParams.get(name);
	}

	public Enumeration<String> getInitParameterNames() {
		return initParams.keys();
	}
	
	public String toString(){
		StringBuffer buf= new StringBuffer();
		buf.append("{")
			.append("filter-name= ")
			.append(filterName)
			.append(",init-param= ")
			.append(initParams)
			.append("}");
		return buf.toString();
	}

}
