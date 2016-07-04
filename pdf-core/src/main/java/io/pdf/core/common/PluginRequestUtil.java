package io.pdf.core.common;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import io.pdf.core.PluginContext;
import io.pdf.core.hook.wrapper.ResourceWrapper;
import io.pdf.core.internal.PluginRepository;

public class PluginRequestUtil {
	/**
	 * get request path, exclude application context path
	 * @param request HttpServletRequest
	 * @return String
	 */
	public static String getRequestPath(HttpServletRequest request){
		/**
		 * http://localhost:8080/framework/security/get.do?personid=xxx
		 * result: /security/get.do
		 */
		//获取请求路径
		String requestPath=request.getServletPath();
		String pathInfo= request.getPathInfo();
		if (pathInfo!= null) {
			requestPath= requestPath + pathInfo;
		}
		//处理http://localhost:8080/framework/servlet/tester;jsessionid=xxxxxxxxxxxxxxx情况，去掉;号后边东西
		int index= requestPath.indexOf(";");
		if(index>0){
			requestPath= requestPath.substring(0,index);
		}
		return requestPath;
	}
	/**
	 * get component ResourceWrapper by request path
	 * @param requestPath
	 * @return
	 */
	public static ResourceWrapper getPluginResourceWrapper(String requestPath){
		String path= (requestPath.startsWith("/")? requestPath.substring(1): requestPath);
		int pos= -1;
		if((pos=path.indexOf("/"))>0){
			String firstcharactor= path.substring(0,pos);
			PluginContext ctx= PluginRepository.getInstance().getPluginContextByAlias(firstcharactor);
			if(null== ctx){
				ctx= PluginRepository.getInstance().getPluginContext(firstcharactor);
			}
			if(null!= ctx){
				String resourcePath= path.substring(pos+1);
				URL resourceURL= ctx.findLocalResource(resourcePath);
				if(null!= resourceURL){
					return new ResourceWrapper(ctx,resourcePath,resourceURL);
				}
			}
		}
		return null;
	}
	public static ResourceWrapper getPluginResourceWrapper(HttpServletRequest request){
		return getPluginResourceWrapper(getRequestPath(request));
	}
	
	/**
	 * match URL regex
	 * @param pattern
	 * @param servletPath
	 * @return
	 */
	public final static boolean match(String pattern, String servletPath) {
		if (pattern == null)
			return (false);

		// Case 1 - Exact Match
		if (pattern.equals(servletPath))
			return (true);

		// Case 2 - Path Match ("/.../*")
		if (pattern.equals("/*") || pattern.equals("/"))
			return (true);
		if (pattern.endsWith("/*")) {
			if (pattern
					.regionMatches(0, servletPath, 0, pattern.length() - 2)) {
				if (servletPath.length() == (pattern.length() - 2)) {
					return (true);
				} else if ('/' == servletPath.charAt(pattern.length() - 2)) {
					return (true);
				}
			}
			return (false);
		}

		// Case 3 - Extension Match
		if (pattern.startsWith("*.")) {
			int slash = servletPath.lastIndexOf('/');
			int period = servletPath.lastIndexOf('.');
			if ((slash >= 0)
					&& (period > slash)
					&& (period != servletPath.length() - 1)
					&& ((servletPath.length() - period) == (pattern.length() - 1))) {
				return (pattern.regionMatches(2, servletPath, period + 1,
						pattern.length() - 2));
			}
		}
		//Case 4 -Extension Matching "/Report-*.do" or "/report/*.do"
		else if(pattern.contains("*.")){
			int period = pattern.indexOf("*.");
			String prevPattern = pattern.substring(0,period);
			String lastPattern = pattern.substring(period+2);
			if(servletPath.startsWith(prevPattern) 
					&& !"".equals(lastPattern) 
					&& servletPath.endsWith(lastPattern)){
				return true;
			}
		}
		return false;
	}
}
