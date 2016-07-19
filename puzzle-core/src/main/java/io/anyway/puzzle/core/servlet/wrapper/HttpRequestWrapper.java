package io.anyway.puzzle.core.servlet.wrapper;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import io.anyway.puzzle.bridge.plugin.PluginFramework;

public class HttpRequestWrapper extends HttpServletRequestWrapper implements HttpServletRequest {
	
	private ServletContext servletContext= PluginFramework.getFrameworkServletContext();
	
	private String pathInfo;
	
	private String servletPath;
	
	public HttpRequestWrapper(HttpServletRequest delegate,ServletContext servletContext){
		super(delegate);
		this.servletContext= servletContext;
	}
	
	public HttpRequestWrapper(HttpServletRequest delegate){
		super(delegate);
	}
	
	public void setServletContext(ServletContext servletContext){
		this.servletContext= servletContext;
	}
	
	public HttpSession getSession(boolean create) {
		HttpSession session =  ((HttpServletRequest)getRequest()).getSession(create);
		if(session==null){
			return null;
		}
		return new HttpSessionWrapper(session,servletContext);
	}
	
	public HttpSession getSession() {
		return new HttpSessionWrapper(((HttpServletRequest)getRequest()).getSession(),servletContext);
	}
	
	public String getPathInfo(){
		if(pathInfo!=null){
			if("".equals(pathInfo)){
				return null;
			}
			return pathInfo;
		}
		return super.getPathInfo();
	}
	
	public void setPathInfo(String pathInfo){
		this.pathInfo = pathInfo;
	}
	
	public String getServletPath(){
		if(servletPath!=null){
			return servletPath;
		}
		return super.getServletPath();
	}
	
	public void setServletPath(String servletPath){
		this.servletPath = servletPath;
	}
	
}
