package io.anyway.puzzle.bridge.hibernate;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.orm.hibernate4.support.OpenSessionInViewFilter;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class FrameworkSessionInViewFilter extends OpenSessionInViewFilter{

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if( WebApplicationContextUtils.getWebApplicationContext(getServletContext())==null){
			filterChain.doFilter(request, response);
			return;
		}
		super.doFilterInternal(request, response, filterChain);
	}
}
