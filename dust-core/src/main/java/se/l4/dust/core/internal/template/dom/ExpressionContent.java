package se.l4.dust.core.internal.template.dom;

import se.l4.dust.api.expression.Expression;
import se.l4.dust.api.template.RenderingContext;
import se.l4.dust.api.template.dom.Content;
import se.l4.dust.api.template.dom.DynamicContent;

public class ExpressionContent
	extends DynamicContent
{
	private final Expression expr;

	public ExpressionContent(Expression expr)
	{
		this.expr = expr;
	}

	@Override
	public Content copy()
	{
		return new ExpressionContent(expr);
	}

	@Override
	public Object getValue(RenderingContext ctx, Object root)
	{
		return expr.execute(ctx, root);
	}

	@Override
	public void setValue(RenderingContext ctx, Object root, Object data)
	{
		// TODO Auto-generated method stub
		
	}

}
