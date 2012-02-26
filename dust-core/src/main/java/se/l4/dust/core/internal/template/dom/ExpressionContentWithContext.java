package se.l4.dust.core.internal.template.dom;

import se.l4.dust.api.expression.Expression;
import se.l4.dust.api.template.RenderingContext;
import se.l4.dust.api.template.dom.Content;
import se.l4.dust.api.template.dom.DynamicContent;
import se.l4.dust.api.template.dom.Element.Attribute;

/**
 * Content that will use another {@link Content} to fetch its data.
 * 
 * @author Andreas Holstenson
 *
 */
public class ExpressionContentWithContext
	extends DynamicContent
{
	private final Expression expr;
	private final Object context;

	public ExpressionContentWithContext(Expression expr, Object context)
	{
		this.expr = expr;
		this.context = context;
	}

	@Override
	public Content copy()
	{
		return new ExpressionContentWithContext(expr, context);
	}
	
	private Object getActualContext(RenderingContext ctx, Object root)
	{
		Object context = this.context;
		if(context instanceof Attribute)
		{
			context = ((Attribute) context).getValue(ctx, root);
		}
		
		// TODO: Support for more types?
		return context;
	}

	@Override
	public Object getValue(RenderingContext ctx, Object root)
	{
		return expr.get(ctx, getActualContext(ctx, root));
	}

	@Override
	public void setValue(RenderingContext ctx, Object root, Object data)
	{
		expr.set(ctx, getActualContext(ctx, root), data);
	}

}