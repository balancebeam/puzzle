package io.pdf.core.aware;

import org.springframework.beans.factory.Aware;

import io.pdf.core.servlet.metadata.ServletMetadata;

public interface ServletMetadataAware extends Aware {
	
	void setServletMetadata(ServletMetadata metadata);
}
