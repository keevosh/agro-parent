<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head profile="http://gmpg.org/xfn/11">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title>AgroKnow API &raquo; Requests</title>
        <meta name="viewport" content="width=device-width">
    </head>
    <body>
        <div id="viewport" style="width:960px;margin:0 auto;">
            <h1>Agroknow Search API</h1>
            <div>
                <!-- DESCRIPTION AND REQUEST ENTRYPOINTS -->
                <h2>Requests</h2>
                <p><a name="url"></a>The base URL of the API is:</p>
                <pre style="padding-left: 30px;"><code>http://&lt;DOMAIN&gt;/v1</code></pre> (it should change to api.agroknow.com later)

                <table style="margin: 1em; border: 2px dashed #e7a500; background-color: #ffffcc;">
                    <tr>
                        <td style="padding: 1em;">
                            You <em>must</em> enter your 32-character API key after the <code>&amp;api_key=</code> parameter
                            in every request you make, including the examples below. <a href="register">Learn how to request one</a>.
                        </td>
                    </tr>
                </table>

                <h2><a name="types"></a>Resource Types</h2>
                <p>When you formulate a REST query, you have to decide which resource type you want. The resource type is the first word in your query
                    and determines the format of the response data. The resource types currently offered are described below.</p>

                <h3><code>akif</code></h3>
                <p>AgroKnow <code>akif</code> type is a data model to represent agricultural data.</p>
                <p>The RESTful URL to request data from the akif resource begins:</p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif</pre>

                <h3><code>agrif</code></h3>
                <p>AgroKnow <code>agrif</code> type is a data model to represent bibliographical data.</p>
                <p>The RESTful URL to request data from the agrif resource begins:</p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/agrif</pre>

                <!-- API FEATURES -->
                <h2>Feature Outline</h2>
                <p>Query features can be broken into two sections: <em>What do you want?</em> and <em>How do you want it?</em></p>
                <h3>What do you want?</h3>
                <ul>
                    <li><a href="#simple">Simple search</a></li>
                    <li><a href="#search-specific-fields">Searching within specific fields</a></li>
                    <li><a href="#temporal">Temporal</a></li>
                    <li><a href="#search-specific-items">Fetching specific items</a></li>
                </ul>
                <h3>How do you want it?</h3>
                <ul>
                    <li><a href="#sorting">Sorting results</a></li>
                    <li><a href="#faceting">Faceting results</a></li>
                    <li><a href="#pagination">Pagination</a></li>
                </ul>
                <h3>Putting it all together</h3>
                <ul>
                    <li><a href="#complex">Complex queries</a></li>
                </ul>
                <p>&nbsp;</p>

                <h2>Features</h2>

                <h3 id="simple">Simple search</h3>
                <p>This is just a keyword search against all the text fields. If don't know anything about the fields, but you know you
                    want <code>fossil</code>, use the <code>q</code> parameter.</p>
                <p><strong>Example:</strong></p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?q=fossil&amp;api_key=</pre>
                <p>(items with <code>fossil</code> anywhere in any field)</p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?q=*&amp;api_key=</pre>
                <p>(all items)</p>

                <h3 id="search-specific-fields">Searching within specific fields</h3>
                <p>You don't know what you want, but you know it has to have <code>oil</code> in the english title. It doesn't just have to be the title, though. You can search
                    for terms within any textual field <em>except</em>:</p>
                <p>You do not need the <code>q</code> parameter when searching within a specific field. Use the field's name as a URL query string parameter instead.</p>
                <p>You can combine specific field searches with simple search. You can also combine multiple field-specific searches. When combining field <code>q</code>
                    parameters, separate them with an ampersand (<code>&amp;</code>). These combinations are exclusive, meaning that result items fulfill all simple and
                    field-specific criteria.</p>
                <p><strong>Examples:</strong></p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?set=KEEVOSH&amp;api_key=</pre>
                <p>(items with <code>KEEVOSH</code> anywhere in the <code>set</code> field)</p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?languageBlocks.en.title=oil&amp;api_key=</pre>
                <p>(items with <code>oil</code> anywhere in the <code>languageBlocks.en.title</code> field)</p>
                <p>Multiple:</p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?expressions.language=en&amp;languageBlocks.en.description=sea&amp;api_key=</pre>
                <p>(items with <code>en</code> in the <code>expressions.languages</a></code> and <code>sea</code> in the <code>languageBlocks.en.description</code>)</p>
                <p>Field &amp; simple:</p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?set=ASETVALUE&amp;q=zebra&amp;api_key=</pre>
                <p>(items with <code>ASETVALUE</code> in the <code>set</code> and <code>zebra</code> in any field.)</p>
                <p>Boolean:</p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?status=pending+OR+active&amp;api_key=</pre>
                <p>(items with <code>active</code> or <code>pending</code> in the <code>status</code>.)</p>

                <h3 id="temporal">Temporal searching</h3>
                <p>The Agroknow Search API understands dates. Combine that with the fielded search above, and you've got a pretty spiffy way of finding records that have fields
                    that fall before, after, and between dates.</p>
                <p>The following examples search the <code>creationDate</code> field which is the time an akif or agrif document was created.</p>
                <p><strong>Examples:</strong></p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?creationDate=1910-01-01&amp;api_key=</pre>
                <p>(items that were created on <code>1910</code>)</p>

                <h3 id="search-specific-items">Fetching specific <code>items</code></h3>
                <p>Say you already have the <code>set</code> and <code>id</code> for the <code>item</code> you want, and
                    you're just looking to get the rest of the metadata for that item. Simply add the <code>set/id</code> to the end of the <code>items</code> request. Bonus:
                    You can search for multiple <code>id</code>'s in the given <code>set</code> by separating the ids with a comma (,).</p>
                <p><strong>Example:</strong></p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif/SECRMICRO/20296?api_key=</pre>
                <p>(item with the given <code>set</code> and <code>id</code>)</p>

                <h3 id="sorting">Sorting results</h3>
                <p>Sort stuff using the <code>sort_by</code> parameter. We&#8217;ll sort stuff in ascending order by default, but if you'd like to flip things,
                    set the <code>sort_order</code> parameter to <code>desc</code>.</p>
                <p><strong>Examples:</strong></p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?q=fossil&amp;sort_by=languageBlocks.en.title&amp;api_key=</pre>
                <p>(items with <code>fossil</code> in any field, sorted by the <code>languageBlocks.en.title</code>)</p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?q=fossil&amp;sort_by=creationDate&amp;sort_order=desc&amp;api_key=</pre>
                <p>(items with <code>fossil</code> in any field, sorted by <code>creationDate</code>)</p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?q=sea&amp;sort_by=languageBlocks.en.title&amp;api_key=</pre>
                <p>(items with <code>sea</code> in any field, sorted by their english title)</p>

                <h3 id="faceting">Faceting results</h3>
                <p>Facets tell you the most common values for certain fields in a collection of items. We return a couple different types of facets depending upon
                    the field you're looking for. For date fields, we'll send back facets of type <code>date_histogram</code> (which is what it sounds like).
                    For complex text fields, we'll break it down for you into a <code>terms</code> type. For simple text fields, we'll also send back a
                    <code>terms</code> type but with unadulterated values. And for geographic types, we'll give you a <code>geo_distance</code> type.
                <p><strong>Examples:</strong></p>
                <p>Basic:</p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?facets=set&amp;api_key=</pre>
                <p>Multiple:</p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?facets=languageBlocks.en.title,set&amp;api_key=</pre>
                <p>No Docs:</p>
                <pre style="padding-left: 30px;">http://api.dp.la/dev/items?facets=set&amp;page_size=0&amp;api_key=</pre>
                <p>Facet Limit:</p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?facets=set&amp;facet_size=3&amp;api_key=</pre>
                <p>&nbsp;</p>

                <h3 id="pagination">Pagination</h3>
                <p>By default, we'll give you 10 items. If that's not enough, you can get the next ten items incrementing the <code>page</code> parameter (it's one-indexed).
                    If that's still not enough, you can pull more items per page by using the <code>page_size</code> parameter (we'll limit you to 100 items per page because
                    greediness is a vice).</p>
                <p><strong>Examples:</strong></p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?q=fossil&amp;page_size=2&amp;api_key=</pre>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?q=sea&amp;page_size=25&amp;api_key=</pre>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?q=sea&amp;page_size=25&amp;page=3&amp;api_key=</pre>
                <p>&nbsp;</p>

                <h3 id="complex">Complex Queries</h3>
                <p>Let's get this APIarty started.</p>
                <p><strong>Examples:</strong></p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?q=sea&amp;languageBlocks.en.title=africa&amp;api_key=</pre>
                <p><span>(items with both <code>sea</code> in any field and <code>africa</code> in the <code>languageBlocks.en.title</code>)</span></p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/akif?languageBlocks.en.description=sea+OR+africa&amp;page_size=25&amp;api_key=</pre>
                <p><span>(the first 25 items with either <code>sea</code> or <code>africa</code> in the <code>languageBlocks.en.description</code>)</span></p>
                <p>&nbsp;</p>

                <h3 id="autocompletion">Suggestion/Autocomplete Queries</h3>
                <p>The last but not least aspect of the API is its suggestion/autocompletion capabilities. You can provide the user's input to the API and
                    get back suggestion from popular searches the user could use and bring him faster to results.</p>
                <p><strong>Examples:</strong></p>
                <pre style="padding-left: 30px;">http://&lt;DOMAIN&gt;/v1/_ac?q=pot&amp;api_key=</pre>
                <p><span>(if previous search queries contain <code>potato</code> or <code>potatoes AND onions</code> or <code>potamology</code> those queries will
                        be in the result and should be should as suggestions to the user)</span></p>
                <p>&nbsp;</p>
            </div>
        </div>
    </body>
</html>
