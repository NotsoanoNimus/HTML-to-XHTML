# Java HTML to XHTML Converter
A Java-based pretty-printer and HTML-to-XHTML converter. Sanitizes documents to the best of its ability.

Simply feed this application an input file and it will generate beautified console output (or a file if you modify it), as you wish.


## Example
Consider this nice, but slightly malformed HTML file.
```
<html>
    <head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
        <meta name="theme-color" content="#135C39">
<link rel="manifest" href="%PUBLIC_URL%/manifest.json"><title>The Romulus Go-Green Group</title>
</head>
<body>
        <noscript>
    <p>You need to enable <b><i>JavaScript</b></i> to view this site. &reg;</p>
        <p>Please update your browser, and ensure that you don't have any plugins disabling your browser's JavaScript!</p>
        </noscript>
<div id=root></div><div id="volunteer-portal"></div>
    </body>
</html>
```

![An example of the conversion results from the above file.](https://raw.githubusercontent.com/NotsoanoNimus/Pretty-Cool-XHTML/master/docs/conversionResults.png)

_Notice the few tags that have adjusted and become nested properly now according to the XHTML standard, as well as the corrected document formatting._


## Important Note
The generated XHTML from this utility has two output options:
- Pretty Printed (neat formatting).
- Minimized.

This is important because the pretty-printed version tends to interfere with _document spacing_. For example, consider the following HTML:
```
<b>Item One</b> and a half<b>, Item Two</b>
```

When this is "pretty-printed", it ends up rendering like so in the XHTML document: `Item One and a half  , Item Two` because the pretty-print feature is separating the line at the boundary and adding the desired spacing.
