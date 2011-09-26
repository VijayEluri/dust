package se.l4.dust.core.internal.expression.invoke;

import com.fasterxml.classmate.ResolvedType;

import se.l4.dust.core.internal.expression.ErrorHandler;
import se.l4.dust.core.internal.expression.ExpressionCompiler;
import se.l4.dust.core.internal.expression.ast.Node;

/**
 * Abstract base class for boolean operations.
 * 
 * @author Andreas Holstenson
 *
 */
public abstract class AbstractBooleanInvoker
	implements Invoker
{
	protected final Node node;
	protected final Invoker left;
	protected final Invoker right;

	public AbstractBooleanInvoker(Node node, Invoker left, Invoker right)
	{
		this.node = node;
		this.left = left;
		this.right = right;
	}
	
	@Override
	public Node getNode()
	{
		return node;
	}
	
	@Override
	public Class<?> getReturnClass()
	{
		return boolean.class;
	}
	
	@Override
	public ResolvedType getReturnType()
	{
		return null;
	}
	
	protected <T> T castLeftNotNull(ErrorHandler errors, Object value)
	{
		return castNotNull(errors, left.getNode(), value);
	}
	
	protected <T> T castRightNotNull(ErrorHandler errors, Object value)
	{
		return castNotNull(errors, right.getNode(), value);
	}
	
	protected <T> T castNotNull(ErrorHandler errors, Node node, Object value)
	{
		if(value == null)
		{
			throw errors.error(node, "Result of expression was null");
		}
		
		return (T) value;
	}
	
	@Override
	public Object interpret(ErrorHandler errors, Object root, Object instance)
	{
		Object lv = left.interpret(errors, root, instance);
		Object rv = right.interpret(errors, root, instance);
		return check(errors, lv, rv);
	}
	
	@Override
	public void set(ErrorHandler errors, Object root, Object instance,
			Object value)
	{
		throw errors.error(node, "Can not set value of this expression");
	}
	
	@Override
	public String toJavaSetter(ErrorHandler errors, ExpressionCompiler compiler, String context)
	{
		return null;
	}
	
	protected abstract boolean check(ErrorHandler errors, Object left, Object right);
}
