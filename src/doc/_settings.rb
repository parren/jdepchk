
@head_nodes << '<link rel="stylesheet" type="text/css" media="screen" href="' + root_path + 'style_screen.css" />'
@head_nodes << '<link rel="stylesheet" type="text/css" media="print" href="' + root_path + 'style_print.css" />'

@rootcrumb = '<a href="http://arrenbrecht.ch/">arrenbrecht.ch</a>'
@crumbs << 'JDepChk'

# Redirect to output path.
@html_name = '../../temp/doc/' + html_name
