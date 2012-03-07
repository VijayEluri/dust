package se.l4.dust.core.internal.expression.invoke;

import se.l4.dust.api.Context;
import se.l4.dust.core.internal.expression.ErrorHandler;
import se.l4.dust.core.internal.expression.ExpressionCompiler;
import se.l4.dust.core.internal.expression.ast.Node;

/**
 * Invoker for OR operations.
 * 
 * @author Andreas Holstenson
 *
 */
public class OrInvoker
	extends AbstractBooleanInvoker
{

	public OrInvoker(Node node, Invoker left, Invoker right)
	{
		super(node, left, right);
	}

	@Override
	protected boolean check(ErrorHandler errors, Object left, Object right)
	{
		Boolean lb = castLeftNotNull(errors, left);
		Boolean rb = castLeftNotNull(errors, right);
		return lb || rb;
	}
	
	@Override
	public Object get(ErrorHandler errors, Context context, Object root, Object instance)
	{
		try
		{
			Object lv = left.get(errors, context, root, instance);
			Boolean lb = castLeftNotNull(errors, lv);
			if(lb)
			{
				return true;
			}
			
			Object rv = right.get(errors, context, root, instance);
			return check(errors, lv, rv);
		}
		catch(Throwable t)
		{
			throw errors.error(node, t);
		}
	}
	
	@Override
	public String toJavaGetter(ErrorHandler errors, ExpressionCompiler compiler, String context)
	{
		String lj = left.toJavaGetter(errors, compiler, context);
		String rj = right.toJavaGetter(errors, compiler, context);
		
		return "(" + compiler.unwrap(left.getReturnClass(), lj) 
			+ " || " 
			+ compiler.unwrap(right.getReturnClass(), rj) + ")";
	}
}
