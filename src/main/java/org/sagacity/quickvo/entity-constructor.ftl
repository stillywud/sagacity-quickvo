<#if (quickVO.type=="TABLE")>
/*---begin-constructor-area---don't-update-this-area--*/
<#if (quickVO.singlePk=='1'||quickVO.singlePk=='0')>
<#assign paramCnt="0"/> 
	/** pk constructor */
	public ${quickVO.entityName}(<#list quickVO.columns as column><#if (column.pkFlag=='1')><#if (paramCnt=='1')>,</#if><#assign paramCnt='1'/>${column.resultType} ${column.colJavaName?uncap_first}</#if></#list>)
	{
		<#list quickVO.columns as column>
		<#if (column.pkFlag=='1')>
		this.${column.colJavaName?uncap_first}=${column.colJavaName?uncap_first};
		</#if>
		</#list>
	}
</#if>

<#if (quickVO.exportTables?exists)>
<#list quickVO.exportTables as exportTable>
    /**
	 * mapping ${exportTable.pkRefTableName} data to ${quickVO.tableName} oneToMany List
	 */
	public void mapping${exportTable.pkRefTableJavaName?cap_first}<#if exportTable.pkRefTableJavaName?ends_with("s")>e</#if>s(List<${quickVO.entityName}> mainSet,List<${exportTable.pkRefTableJavaName?cap_first}> itemSet)
    {
    	if(mainSet==null || mainSet.isEmpty() || itemSet==null||itemSet.isEmpty())
    		return;
    	${quickVO.entityName} main;
    	${exportTable.pkRefTableJavaName?cap_first} item;
    	for(int i=0;i<mainSet.size();i++){
    		main=mainSet.get(i);
    		if(itemSet.size()==0)
    			break;
    		for(int j=0;j<itemSet.size();j++){
    			item=itemSet.get(j);
    			if(${exportTable.pkEqualsFkStr}){
    			   main.${exportTable.pkRefTableJavaName?uncap_first}<#if exportTable.pkRefTableJavaName?ends_with("s")>e</#if>s.add(item);
    			   itemSet.remove(j);
    			   j--;
    			}
    		}
    	}
    }
</#list>
</#if>
	/*---end-constructor-area---don't-update-this-area--*/
</#if>