<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title><#if request.requestURI?ends_with('/tabs2')><#if startedBy??&&startedBy>发起的流程<#else>经办的流程</#if><#else>流程列表</#if></title>
</head>
<body>

<#assign dataurl=actionBaseUrl/>
<#if Parameters.involved??>
<#assign dataurl+='/involved'/>
</#if>
<#if request.queryString??>
<#assign dataurl+='?'+request.queryString>
</#if>
<ul class="nav nav-tabs">
	<li class="active"><a href="#all_historicProcessInstances" data-toggle="tab">${getText('all')}</a></li>
	<li><a href="#unfinished_historicProcessInstances" data-toggle="tab">${getText('unfinished')}</a></li>
	<li><a href="#finished_historicProcessInstances" data-toggle="tab">${getText('finished')}</a></li>
</ul>
<div class="tab-content">
	<div id="all_historicProcessInstances" class="tab-pane ajaxpanel active" data-url="${dataurl}">
	</div>
	<div id="unfinished_historicProcessInstances" class="tab-pane ajaxpanel manual" data-url="${dataurl+dataurl?contains('?')?then('&','?')}&finished=false">
	</div>
	<div id="finished_historicProcessInstances" class="tab-pane ajaxpanel manual" data-url="${dataurl+dataurl?contains('?')?then('&','?')}&finished=true">
	</div>
</div>
</body>
</html>