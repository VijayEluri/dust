package se.l4.dust.api.template.mixin;

import java.io.IOException;


/**
 * Wrapper that can be applied by a mixin. The wrapper will execute before
 * and after an element has been emitted.
 *
 * @author Andreas Holstenson
 *
 */
public interface ElementWrapper
{
	void beforeElement(ElementEncounter encounter)
		throws IOException;

	void afterElement(ElementEncounter encounter)
		throws IOException;
}
