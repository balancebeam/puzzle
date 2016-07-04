package io.pdf.core.hook.delegate;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.pdf.bridge.plugin.PluginFramework;
import io.pdf.core.aware.ElementPostfixAware;
import io.pdf.core.common.PluginRequestUtil;
import io.pdf.core.hook.wrapper.ResourceWrapper;
/**
 * process component static resource request, such as js、css、png and so on
 * 
 * @author yangzz
 *
 */
public class DefaultDispatcherFilter implements Filter,ElementPostfixAware{

	private Log logger = LogFactory.getLog(DefaultDispatcherFilter.class);
	
	private static final String LAST_MODIFIED = "Last-Modified";
	private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	private static final String IF_NONE_MATCH = "If-None-Match";
	private static final String ETAG = "ETag";
	
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
			FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response= (HttpServletResponse)servletResponse;
		String requestPath= PluginRequestUtil.getRequestPath(request);
		// it's not a framework static resource
		URL url= PluginFramework.getFrameworkServletContext().getResource(requestPath);
		if(url== null){
			ResourceWrapper wrapper= PluginRequestUtil.getPluginResourceWrapper(requestPath);
			if(null!= wrapper){
				String method = request.getMethod();
				if (method.equals("GET") || method.equals("POST") || method.equals("HEAD")) {
					if(logger.isDebugEnabled()){
						logger.debug("begin to read plugin ["+wrapper.getPluginContext().getName()+"] resource "+wrapper.getResourcePath());
					}
					URL resourceURL=  wrapper.getResource();
					ServletContext servletContext= wrapper.getPluginContext().getServletContext();
					writeResource(request, response, requestPath, resourceURL,servletContext);
					if(logger.isDebugEnabled()){
						logger.debug("success to read plugin ["+wrapper.getPluginContext().getName()+"] resouce "+wrapper.getResourcePath());
					}
				} else {
					if(logger.isWarnEnabled()){
						logger.warn("not support method: "+method);
					}
					response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				}
				return;
			}
		}
		
		chain.doFilter(servletRequest, servletResponse);
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void writeResource(final HttpServletRequest req, final HttpServletResponse resp, final String resourcePath, final URL resourceURL,final ServletContext servletContext) throws IOException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {

				public Object run() throws Exception {
					URLConnection connection = resourceURL.openConnection();
					long lastModified = connection.getLastModified();
					int contentLength = connection.getContentLength();

					String etag = null;
					if (lastModified != -1 && contentLength != -1)
						etag = "W/\"" + contentLength + "-" + lastModified + "\""; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

					// Check for cache revalidation.
					// We should prefer ETag validation as the guarantees are stronger and all HTTP 1.1 clients should be using it
					String ifNoneMatch = req.getHeader(IF_NONE_MATCH);
					if (ifNoneMatch != null && etag != null && ifNoneMatch.indexOf(etag) != -1) {
						resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						return Boolean.TRUE;
					}

					long ifModifiedSince = req.getDateHeader(IF_MODIFIED_SINCE);
					// for purposes of comparison we add 999 to ifModifiedSince since the fidelity
					// of the IMS header generally doesn't include milli-seconds
					if (ifModifiedSince > -1 && lastModified > 0 && lastModified <= (ifModifiedSince + 999)) {
						resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						return Boolean.TRUE;
					}

					// return the full contents regularly
					if (contentLength != -1)
						resp.setContentLength(contentLength);

					String contentType = servletContext.getMimeType(resourcePath);

					if (contentType != null)
						resp.setContentType(contentType);

					if (lastModified > 0)
						resp.setDateHeader(LAST_MODIFIED, lastModified);

					if (etag != null)
						resp.setHeader(ETAG, etag);

					if (contentLength != 0) {
						// open the input stream
						InputStream is = null;
						try {
							is = connection.getInputStream();
							// write the resource
							try {
								OutputStream os = resp.getOutputStream();
								int writtenContentLength = writeResourceToOutputStream(is, os);
								if (contentLength == -1 || contentLength != writtenContentLength)
									resp.setContentLength(writtenContentLength);
							} catch (IllegalStateException e) { // can occur if the response output is already open as a Writer
								Writer writer = resp.getWriter();
								writeResourceToWriter(is, writer);
								// Since ContentLength is a measure of the number of bytes contained in the body
								// of a message when we use a Writer we lose control of the exact byte count and
								// defer the problem to the Servlet Engine's Writer implementation.
							}
						} catch (FileNotFoundException e) {
							// FileNotFoundException may indicate the following scenarios
							// - url is a directory
							// - url is not accessible
							sendError(resp, HttpServletResponse.SC_FORBIDDEN);
						} catch (SecurityException e) {
							// SecurityException may indicate the following scenarios
							// - url is not accessible
							sendError(resp, HttpServletResponse.SC_FORBIDDEN);
						} finally {
							if (is != null)
								try {
									is.close();
								} catch (IOException e) {
									// ignore
								}
						}
					}
					return Boolean.TRUE;
				}
			}, AccessController.getContext());
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
	}

	void sendError(final HttpServletResponse resp, int sc) throws IOException {

		try {
			// we need to reset headers for 302 and 403
			resp.reset();
			resp.sendError(sc);
		} catch (IllegalStateException e) {
			// this could happen if the response has already been committed
		}
	}

	int writeResourceToOutputStream(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[8192];
		int bytesRead = is.read(buffer);
		int writtenContentLength = 0;
		while (bytesRead != -1) {
			os.write(buffer, 0, bytesRead);
			writtenContentLength += bytesRead;
			bytesRead = is.read(buffer);
		}
		return writtenContentLength;
	}

	void writeResourceToWriter(InputStream is, Writer writer) throws IOException {
		Reader reader = new InputStreamReader(is);
		try {
			char[] buffer = new char[8192];
			int charsRead = reader.read(buffer);
			while (charsRead != -1) {
				writer.write(buffer, 0, charsRead);
				charsRead = reader.read(buffer);
			}
		} finally {
			if (reader != null) {
				reader.close(); // will also close input stream
			}
		}
	}

	public void destroy() {
	}

}
