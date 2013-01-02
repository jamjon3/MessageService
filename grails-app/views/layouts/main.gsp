<!doctype html>
<!--[if lt IE 7 ]> <html lang="en" class="no-js ie6"> <![endif]-->
<!--[if IE 7 ]>    <html lang="en" class="no-js ie7"> <![endif]-->
<!--[if IE 8 ]>    <html lang="en" class="no-js ie8"> <![endif]-->
<!--[if IE 9 ]>    <html lang="en" class="no-js ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="en" class="no-js"><!--<![endif]-->
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
		<title><g:layoutTitle default="Grails"/></title>
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
                <r:require modules="core"/>
		<link rel="shortcut icon" href="${resource(dir: 'images', file: 'usf-favicon.ico')}" type="image/x-icon">
		<link rel="apple-touch-icon" href="${resource(dir: 'images', file: 'apple-touch-icon.png')}">
		<link rel="apple-touch-icon" sizes="114x114" href="${resource(dir: 'images', file: 'apple-touch-icon-retina.png')}">
        <link type="text/css" href="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.13/themes/flick/jquery-ui.css" rel="stylesheet" />
        <link type="text/css" href="https://ajax.aspnetcdn.com/ajax/jquery.dataTables/1.9.1/css/jquery.dataTables_themeroller.css" rel="stylesheet" /> 
		<link rel="stylesheet" href="${resource(dir: 'js/jQuery-Timepicker-Addon', file: 'jquery-ui-timepicker-addon.css')}" type="text/css">
        <link rel="stylesheet" href="${resource(dir: 'css', file: 'MessageService.css')}" type="text/css">
		<g:layoutHead/>
        <r:layoutResources />
	</head>
	<body>               
          <g:layoutBody/>
          <!-- <div id="footer" class="ui-widget-header ui-corner-all" role="contentinfo"></div> -->
          <div id="spinner" class="spinner" style="display:none;"><g:message code="spinner.alt" default="Loading&hellip;"/></div>
          <g:javascript library="application"/>
          <r:layoutResources />
	</body>
</html>