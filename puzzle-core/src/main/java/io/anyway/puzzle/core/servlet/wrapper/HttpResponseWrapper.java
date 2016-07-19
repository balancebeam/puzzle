package io.anyway.puzzle.core.servlet.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class HttpResponseWrapper extends
		HttpServletResponseWrapper implements HttpServletResponse {

	private ComponentServletOutputStream out = new ComponentServletOutputStream();
	
	public HttpResponseWrapper(HttpServletResponse delegate) {
		super(delegate);
	}

	public ServletOutputStream getOutputStream() throws IOException {
		return out;
	}

	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(out);
	}
	
	public byte[] getResponseBody(){
		return out.getResponseBody();
	}

	final private static class ComponentServletOutputStream extends
			ServletOutputStream {

		private ByteArrayOutputStream buf= new ByteArrayOutputStream();
		
		@Override
		public void write(int b) throws IOException {
			buf.write(b);
		}

		public byte[] getResponseBody() {
			return buf.toByteArray();
		}
	}
}
