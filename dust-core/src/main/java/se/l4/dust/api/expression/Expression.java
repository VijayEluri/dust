package se.l4.dust.api.expression;

import se.l4.dust.api.Context;

/**
 * Expression as retrieved from {@link Expressions}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface Expression
{
	/**
	 * Execute the expression using the given context and instance.
	 * 
	 * @param context
	 * 		context (such as rendering, etc)
	 * @param instance
	 * 		instance to execute on
	 * @return
	 */
	Object get(Context context, Object instance);
	
	/**
	 * Set the value of this expression (if possible).
	 * 
	 * @param context
	 * @param instance
	 * @param value
	 */
	void set(Context context, Object instance, Object value);
}