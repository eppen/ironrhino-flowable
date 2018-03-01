<#ftl output_format='HTML'>
<#assign templateName="form/"+processDefinitionKey/>
<#if formKey?has_content>
	<#assign templateName+="_"+formKey/>
</#if>
<#assign templateName+=".comments.ftl"/>
<#assign template=.get_optional_template(templateName)>
<#if template.exists>
	<@template.include/>
<#elseif comments?? && !comments.empty>
	<table class="table">
			<caption style="background-color: #bebec5;"><h5>${getText('comment')}</h5></caption>
			<thead>
			<tr>
				<th style="width:80px;">${getText('owner')}</th>
				<th style="width:130px;">${getText('date')}</th>
				<th>${getText('comment')}</th>
				<th style="width:80px;"></th>
			</tr>
			</thead>
			<tbody>
			<#list comments as comment>
			<tr>
				<td>
				<#if comment.userId??>
				<span class="user" data-username="${comment.userId}">${(beans['userDetailsService'].loadUserByUsername(comment.userId,true))!}</span>
				</#if>
				</td>
				<td>${comment.time?datetime}</td>
				<td style="white-space:pre-wrap;word-break:break-all;">${comment.fullMessage!}</td>
				<td>
				<#if comment.userId?? && comment.userId==authentication("principal").username>
				<a href="${actionBaseUrl}/deleteComment/${comment.id}" class="btn ajax deleteRow">${getText('delete')}</a>
				</#if>
				</td>
			</tr>
			</#list>
			</tbody>
	</table>	
</#if>