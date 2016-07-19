package io.anyway.puzzle.core.hook.delegate;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import io.anyway.puzzle.core.aware.ElementPostfixAware;
import io.anyway.puzzle.core.servlet.PluginWebInstanceRepository;
import io.anyway.puzzle.core.servlet.wrapper.HttpRequestWrapper;
import io.anyway.puzzle.core.servlet.wrapper.HttpServletWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.anyway.puzzle.bridge.plugin.PluginFramework;
import io.anyway.puzzle.core.common.PluginRequestUtil;
import io.anyway.puzzle.core.exception.PuzzleException;

/**
 * Global Sevlet Filter
 * @author yangzz
 *
 */
public class ServletDispatcherFilter implements Filter,ElementPostfixAware {
	
	private Log logger = LogFactory.getLog(ServletDispatcherFilter.class);

	public void init(FilterConfig filterConfig) throws ServletException {
	}

	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse,
			FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		String requestPath= PluginRequestUtil.getRequestPath(request);
		
		List<HttpServletWrapper> httpServlets=  PluginWebInstanceRepository.getHttpServlets();
		for(HttpServletWrapper wrapper: httpServlets){
			List<String> patterns= wrapper.getPatterns();
			for (String pattern: patterns) {
				if (PluginRequestUtil.match(pattern, requestPath)) {
					if(logger.isDebugEnabled()){
						logger.debug("Request Servlet: "+wrapper);
					}
					HttpRequestWrapper requestWrapper= null;
					if(request instanceof HttpRequestWrapper){
						requestWrapper= (HttpRequestWrapper)request;
						requestWrapper.setServletContext(wrapper.getServletContext());
					}
					else{
						requestWrapper= new HttpRequestWrapper(request,wrapper.getServletContext());
					}
					//amend request.getServletPath
					if(pattern.endsWith("/*")){
						pattern = pattern.replace("/*", "");
						if(requestPath.startsWith(pattern)){
							String newpathInfo = requestPath.substring(pattern.length());					
							requestWrapper.setPathInfo(newpathInfo);
							requestWrapper.setServletPath(pattern);
						}
					}
					if("".equals(requestWrapper.getServletPath()) && request.getPathInfo()!=null){
						requestWrapper.setServletPath(request.getPathInfo());
						requestWrapper.setPathInfo("");
					}
					
					ClassLoader original = Thread.currentThread().getContextClassLoader();
					try{
						Thread.currentThread().setContextClassLoader(PluginFramework.getFrameworkContextClassLoader());
						wrapper.getHttpServlet().service(requestWrapper, servletResponse);
						if(logger.isDebugEnabled()){
							logger.debug("Success："+wrapper);
						}
						
					}catch(Throwable e){
						if(e instanceof PuzzleException){
							throw (PuzzleException)e;
						}
						if(e.getCause()!=null && e.getCause() instanceof PuzzleException){
							throw (PuzzleException)e.getCause();
						}
						throw new PuzzleException("Request Servlet failure: "+wrapper,e);
					}					
					finally{
						Thread.currentThread().setContextClassLoader(original);
					}
					return;
				}
			}
		}
		chain.doFilter(servletRequest, servletResponse);
	}

	public void destroy() {
	}

}
