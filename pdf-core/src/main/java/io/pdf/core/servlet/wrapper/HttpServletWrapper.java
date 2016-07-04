package io.pdf.core.servlet.wrapper;

import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import io.pdf.core.servlet.metadata.ServletMetadata;

public class HttpServletWrapper {
	
	private HttpServlet servlet;
	
	private ServletConfig config;
	
	private boolean started= false;
	
	private ServletMetadata metadata;
	
	public HttpServletWrapper(HttpServlet servlet,ServletConfig config,ServletMetadata metadata){
		this.servlet= servlet;
		this.config= config;
		this.metadata= metadata;
	}
	
	public HttpServlet getHttpServlet() throws ServletException{
		if(!started && (started= true)){
			servlet.init(config);
		}
		return servlet;
	}

	public List<String> getPatterns(){
		return metadata.getPatterns();
	}
	
	public boolean started(){
		return started;
	}
	
	public ServletContext getServletContext(){
		return config.getServletContext();
	}
}
