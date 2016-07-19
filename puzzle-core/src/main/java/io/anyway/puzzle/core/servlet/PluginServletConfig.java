package io.anyway.puzzle.core.servlet;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class PluginServletConfig implements ServletConfig{
	
	private String servletName;
	
	private ServletContext servletContext;
	
	private Hashtable<String,String> initParameters;
	
	public PluginServletConfig(String servletName,ServletContext servletContext,Hashtable<String,String> initParameters){
		this.servletName= servletName;
		this.servletContext= servletContext;
		this.initParameters= initParameters;
	}
	
	public String getServletName() {
		return servletName;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public String getInitParameter(String name) {
		return initParameters.get(name);
	}

	public Enumeration<String> getInitParameterNames() {
		return initParameters.keys();
	}
	
	public String toString(){
		StringBuffer buf= new StringBuffer();
		buf.append("{")
			.append("servlet-name= ")
			.append(servletName)
			.append(",init-param= ")
			.append(initParameters)
			.append("}");
		return buf.toString();
	}
	
}
