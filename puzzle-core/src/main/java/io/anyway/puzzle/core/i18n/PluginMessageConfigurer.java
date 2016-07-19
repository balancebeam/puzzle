package io.anyway.puzzle.core.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.util.ObjectUtils;

public class PluginMessageConfigurer extends ResourceBundleMessageSource{
	/**
	 * get local component i18n content
	 * @param code
	 * @param args
	 * @param locale
	 * @return
	 * @throws NoSuchMessageException
	 */
	public String getLocalMessage(String code, Object[] args, Locale locale)throws NoSuchMessageException{
		if (code == null) {
			return null;
		}
		if (locale == null) {
			locale = Locale.getDefault();
		}
		Object[] argsToUse = args;

		if (!isAlwaysUseMessageFormat() && ObjectUtils.isEmpty(args)) {
			// Optimized resolution: no arguments to apply,
			// therefore no MessageFormat needs to be involved.
			// Note that the default implementation still uses MessageFormat;
			// this can be overridden in specific subclasses.
			String message = resolveCodeWithoutArguments(code, locale);
			if (message != null) {
				return message;
			}
		}

		else {
			// Resolve arguments eagerly, for the case where the message
			// is defined in a parent MessageSource but resolvable arguments
			// are defined in the child MessageSource.
			argsToUse = resolveArguments(args, locale);

			MessageFormat messageFormat = resolveCode(code, locale);
			if (messageFormat != null) {
				synchronized (messageFormat) {
					return messageFormat.format(argsToUse);
				}
			}
		}
		// Check locale-independent common messages for the given message code.
		Properties commonMessages = getCommonMessages();
		if (commonMessages != null) {
			String commonMessage = commonMessages.getProperty(code);
			if (commonMessage != null) {
				return formatMessage(commonMessage, args, locale);
			}
		}
		throw new NoSuchMessageException(code,locale);
	}
}
