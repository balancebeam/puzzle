package io.pdf.core.servlet.wrapper;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;


@SuppressWarnings("deprecation")
public class HttpSessionWrapper implements HttpSession{
	
	private HttpSession delegate;
	
	private ServletContext servletContext;
	
	public HttpSessionWrapper(HttpSession delegate,ServletContext servletContext){
		this.delegate= delegate;
		this.servletContext= servletContext;
	}
	
	@Override
	public long getCreationTime() {
		return delegate.getCreationTime();
	}
	
	@Override
	public String getId() {
		return delegate.getId();
	}
	
	@Override
	public long getLastAccessedTime() {
		return delegate.getLastAccessedTime();
	}
	
	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}
	
	@Override
	public void setMaxInactiveInterval(int interval) {
		delegate.setMaxInactiveInterval(interval);
	}
	
	@Override
	public int getMaxInactiveInterval() {
		return delegate.getMaxInactiveInterval();
	}
	
	@Override
	public HttpSessionContext getSessionContext() {
		return delegate.getSessionContext();
	}
	
	@Override
	public Object getAttribute(String name) {
		return delegate.getAttribute(name);
	}
	
	@Override
	public Object getValue(String name) {
		return delegate.getValue(name);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<String> getAttributeNames() {
		return delegate.getAttributeNames();
	}
	
	@Override
	public String[] getValueNames() {
		return delegate.getValueNames();
	}
	
	@Override
	public void setAttribute(String name, Object value) {
		delegate.setAttribute(name, value);	
	}
	
	@Override
	public void putValue(String name, Object value) {
		delegate.putValue(name, value)	;	
	}
	
	@Override
	public void removeAttribute(String name) {
		delegate.removeAttribute(name);
	}
	
	@Override
	public void removeValue(String name) {
		delegate.removeValue(name);
	}
	
	@Override
	public void invalidate() {
		delegate.invalidate();
	}
	
	@Override
	public boolean isNew() {
		return delegate.isNew();
	}
}

