<ul class="nav">
  <li><a href="<@url value="/"/>" class="ajax view">${getText("index")}</a></li>
  <@authorize ifAnyGranted="ROLE_ADMINISTRATOR">
  <li><a href="<@url value="/user"/>" class="ajax view">${getText("user")}</a></li>
  <li><a href="<@url value="/bpmn/processDefinition"/>" class="ajax view">流程部署</a></li>
  <li><a href="<@url value="/bpmn/historicProcessInstance/tabs"/>" class="ajax view">所有流程</a></li>
  <li><a href="<@url value="/bpmn/task"/>" class="ajax view">所有任务</a></li>
  <li><a href="<@url value="/bpmn/processInstance"/>" class="ajax view">运行中的流程</a></li>
  </@authorize>
  <@authorize ifNotGranted="ROLE_ADMINISTRATOR">
  <li class="dropdown">
 	<a href="#" class="dropdown-toggle" data-toggle="dropdown">
      	我的流程
    </a>
    <ul class="dropdown-menu">
        <li><a href="<@url value="/bpmn/historicProcessInstance/tabs?involved"/>" class="ajax view">经办的流程</a></li>
        <li><a href="<@url value="/bpmn/historicProcessInstance/tabs?involved&startedBy=true"/>" class="ajax view">发起的流程</a></li>
    </ul>
  </li>
  <li><a href="<@url value="/bpmn/task/todotabs"/>" class="ajax view">我的任务</a></li>
  <#assign startableProcessMap=beans['processHelper'].findStartableProcessMap(authentication('principal').username)>
  <#if !startableProcessMap.empty>
  <li class="dropdown">
 	<a href="#" class="dropdown-toggle" data-toggle="dropdown">
      	发起流程
    </a>
    <ul class="dropdown-menu">
    	<#list startableProcessMap.entrySet() as entry>
        <li><a href="<@url value="/bpmn/task/form?processDefinitionKey=${entry.key}"/>" class="ajax view">${entry.value}</a></li>
        </#list>
    </ul>
  </li>
  </#if>
  <@authorize ifAnyGranted="employee">
  <li><a href="<@url value="/leave"/>" class="ajax view">我的请假</a></li>
  </@authorize>
  </@authorize>
</ul>