package io.pdf.core.servlet.wrapper;

import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import io.pdf.core.servlet.metadata.FilterMetadata;

public class FilterWrapper {
	
	private Filter filter;
	
	private int priority= -1; 
	
	private FilterConfig filterConfig;
	
	private FilterMetadata metadata;
	
	public FilterWrapper(Filter filter,FilterConfig filterConfig,FilterMetadata metadata){
		this.filter= filter;
		this.filterConfig= filterConfig;
		this.metadata= metadata;
		String priority= filterConfig.getInitParameter("priority");
		this.priority= null!= priority?Integer.parseInt(priority): -1;
	}
	
	public Filter getFilter(){
		return filter;
	}
	
	public List<String> getPatterns(){
		return metadata.getPatterns();
	}
	
	public int getPriority(){
		return priority;
	}
	
	public ServletContext getServletContext(){
		return filterConfig.getServletContext();
	}
	
	public String toString(){
		return metadata.toString();
	}
}
