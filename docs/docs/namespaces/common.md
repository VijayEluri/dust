---
layout: page
title: dust:common

nav: docs
---
{::options parse_block_html="true" /}

This is the common namespace, where the default components reside.

## Table of Contents
{:.no_toc}

* toc
{:toc max_level=2}

## Components

All of these components are located in the namespace `dust:common`. All of these examples bind `dust:common` to `d` and `dust:parameters` to `p`.

<div class="padding-bottom">

### `if`

Component that will output its content if a certain test passes. Will optionally output some content if the test does not match.

#### Example
{:.no_toc}

{% highlight xml %}
<d:if test="${test}">
	This will be emitted if the test passes.
	<p:else>
		This is an optional parameter that is rendered if the test does not pass.
	</p:else>
</d:if>
{% endhighlight %}

#### Attributes and parameters
{:.no_toc}

| Name      | Required | Type       | Description |
|:----------|:---------|:-----------|:------------|
| `test`    | Yes      | Boolean    | The expression to test against. |
| `else`    | No       | Parameter  | Optional markup block to render if the test fails |

</div>

<div class="padding-bottom">

### `loop`

Loop over certain values, outputting its content for every value.

#### Example
{:.no_toc}

{% highlight xml %}
<d:loop source="${values}" value="${value}">
	<div>${value}>/div>
</d:loop>
{% endhighlight %}

#### Attributes and parameters
{:.no_toc}

| Name      | Required | Type       | Description |
|:----------|:---------|:-----------|:------------|
| `source`  | Yes      | Array, Iterable or Iterator | The source of values to loop over. |
| `value`   | Yes      | * | The expression that is set for every value looped over |

</div>

<div class="padding-bottom">

### `body`

Output content passed into the body of a component. Can optionally output the contents of a parameter.

#### Example
{:.no_toc}

{% highlight xml %}
<d:body />

<d:body id="param" />
{% endhighlight %}

#### Attributes and parameters
{:.no_toc}

| Name      | Required | Type       | Description |
|:----------|:---------|:-----------|:------------|
| `id`      | No       | String     | Optional identifier of a parameter to output |

</div>

<div class="padding-bottom">

### `raw`

Output some content without escaping it.

#### Example
{:.no_toc}

{% highlight xml %}
<d:raw value="${someHtml}">
{% endhighlight %}

#### Attributes and parameters
{:.no_toc}

| Name      | Required | Type       | Description |
|:----------|:---------|:-----------|:------------|
| `value`   | Yes      | *          | Value to output |

</div>

<div class="padding-bottom">

### `holder`

Wrap other markup and output it. Used to support components that do not have a single root element.

#### Example
{:.no_toc}

{% highlight xml %}
<d:holder>
	<td>Hello</td>
	<td>Cookie</td>
</d:holder>
{% endhighlight %}

</div>

<div class="padding-bottom">
### `render`

Render an object assuming it has a template or it is an instance of `Emittable`.

#### Example
{:.no_toc}

{% highlight xml %}
<d:render value="${object}" />
{% endhighlight %}

### Attributes and parameters
{:.no_toc}

| Name      | Required | Type       | Description |
|:----------|:---------|:-----------|:------------|
| `value`   | Yes      | *          | Value to render |

</div>

## Mixins

### `if`

Only output the element if the test passes.

{% highlight xml %}
<div d:if="${test}">
	Hello!
</div>
{% endhighlight %}

### `attributes`

Mixin attributes used when calling the current component. 

{% highlight xml %}
<button class="btn-lg" d:attributes="merge">${title}</button>
{% endhighlight %}

## Expressions

### `urlencode(String)`

Encode the given string for inclusion in a URL.

{% highlight xml %}
${d:urlencode(name)}

${name.d:urlencode()}
{% endhighlight %}

### `emit` and `skip`

These two properties are used together with attributes to output an empty attribute or skip an attribute.

{% highlight xml %}
<input type="checkbox" ... checked="${checked ? d:emit : d:skip}" />
{% endhighlight %}
