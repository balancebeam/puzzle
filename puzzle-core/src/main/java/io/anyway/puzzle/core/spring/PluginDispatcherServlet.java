package io.anyway.puzzle.core.spring;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.anyway.puzzle.core.aware.ServletMetadataAware;
import io.anyway.puzzle.core.servlet.metadata.ServletMetadata;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;

import io.anyway.puzzle.core.exception.PuzzleException;

public final class PluginDispatcherServlet extends DispatcherServlet implements ServletMetadataAware {
	
	private static final long serialVersionUID = 1L;
	
	protected ServletMetadata metadata;
	
	@Override
	public void init(ServletConfig config) throws ServletException{
		if(null!= metadata){
			for(String pattern: metadata.getPatterns()){
				if(!pattern.matches("/\\w+/.*")){
					throw new PuzzleException(metadata.getServletName()+" path of "+pattern+" is invalid，must be the pattern of /**/*");
				}
			}
		}
		super.init(config);
	}
	
	@Override
	public void setServletMetadata(ServletMetadata metadata) {
		this.metadata= metadata;
	}
	
	@Override
	protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) throws Exception {
		logger.error(ex.getMessage(), ex);
		return super.processHandlerException(request,response,handler,ex);
	}
	
	@Override
	protected LocaleContext buildLocaleContext(HttpServletRequest request) {
		return new SimpleLocaleContext(LocaleContextHolder.getLocale());
	}
	
}
