/**
 * 
 */
package org.sagacity.quickvo.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.sagacity.quickvo.Constants;
import org.sagacity.quickvo.model.BusinessIdConfig;
import org.sagacity.quickvo.model.CascadeModel;
import org.sagacity.quickvo.model.ColumnTypeMapping;
import org.sagacity.quickvo.model.ConfigModel;
import org.sagacity.quickvo.model.PrimaryKeyStrategy;
import org.sagacity.quickvo.model.QuickModel;
import org.sagacity.quickvo.utils.DBHelper;
import org.sagacity.quickvo.utils.FileUtil;
import org.sagacity.quickvo.utils.LoggerUtil;
import org.sagacity.quickvo.utils.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @project sagacity-quickvo
 * @description 解析xml配置文件
 * @author chenrenfei <a href="mailto:zhongxuchen@hotmail.com">联系作者</a>
 * @version id:XMLConfigLoader.java,Revision:v1.0,Date:2010-7-21
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class XMLConfigLoader {
	/**
	 * 定义全局日志
	 */
	private static Logger logger = LoggerUtil.getLogger();

	/**
	 * @todo 解析配置文件
	 * @return
	 * @throws Exception
	 */
	public static ConfigModel parse() throws Exception {
		File xmlFile = new File(FileUtil.linkPath(Constants.BASE_LOCATE, Constants.QUICK_CONFIG_FILE));
		// 文件不存在则忽视相对路径进行重试
		if (!xmlFile.exists()) {
			xmlFile = new File(Constants.QUICK_CONFIG_FILE);
			if (!xmlFile.exists()) {
				logger.info("相对路径:" + Constants.BASE_LOCATE + ",配置文件:[" + Constants.QUICK_CONFIG_FILE + "]不存在,请正确配置!");
				throw new Exception("配置文件:" + xmlFile.getAbsolutePath() + " 不存在,请正确配置!");
			}
		}
		ConfigModel configModel = new ConfigModel();
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
		Document doc = domBuilder.parse(xmlFile);
		Element root = doc.getDocumentElement();
		// 加载常量信息
		Constants.loadProperties(root.getElementsByTagName("property"));

		// 任务设置
		Element tasks = (Element) root.getElementsByTagName("tasks").item(0);

		// 输出路径
		if (tasks.hasAttribute("dist")) {
			configModel.setTargetDir(Constants.replaceConstants(tasks.getAttribute("dist")));
		} else {
			configModel.setTargetDir(Constants.BASE_LOCATE);
		}
		// 判断指定的目标路径是否是根路径
		if (!FileUtil.isRootPath(configModel.getTargetDir())) {
			configModel.setTargetDir(FileUtil.skipPath(Constants.BASE_LOCATE, configModel.getTargetDir()));
		}

		// 设置抽象类路径
		if (tasks.hasAttribute("abstractPath")) {
			configModel.setAbstractPath(Constants.replaceConstants(tasks.getAttribute("abstractPath")));
		} else if (tasks.hasAttribute("abstract-path")) {
			configModel.setAbstractPath(Constants.replaceConstants(tasks.getAttribute("abstract-path")));
		}

		// 设置编码格式
		if (tasks.hasAttribute("encoding")) {
			configModel.setEncoding(Constants.replaceConstants(tasks.getAttribute("encoding")));
		}

		NodeList quickVOs = tasks.getElementsByTagName("task");
		if (quickVOs == null || quickVOs.getLength() == 0) {
			return null;
		}
		// 解析task 任务配置信息
		List quickModels = new ArrayList();
		Element quickvo;
		Element vo;
		Element entity;
		boolean active = true;
		NodeList nodeList;
		for (int i = 0; i < quickVOs.getLength(); i++) {
			quickvo = (Element) quickVOs.item(i);
			active = true;
			if (quickvo.hasAttribute("active")) {
				active = Boolean.parseBoolean(quickvo.getAttribute("active"));
			}
			// 生效的任务
			if (active) {
				QuickModel quickModel = new QuickModel();
				if (quickvo.hasAttribute("author")) {
					quickModel.setAuthor(quickvo.getAttribute("author"));
				}
				if (quickvo.hasAttribute("datasource")) {
					quickModel.setDataSource(quickvo.getAttribute("datasource"));
				} else if (quickvo.hasAttribute("dataSource")) {
					quickModel.setDataSource(quickvo.getAttribute("dataSource"));
				}
				if (quickvo.hasAttribute("swagger-model")) {
					quickModel.setSwaggerApi(Boolean.parseBoolean(quickvo.getAttribute("swagger-model")));
				}
				if (quickvo.hasAttribute("include")) {
					// *表示全部,等同于没有include配置
					if (!quickvo.getAttribute("include").equals("*")) {
						quickModel.setIncludeTables(Constants.replaceConstants(quickvo.getAttribute("include")));
					}
				}
				// 排除的表
				if (quickvo.hasAttribute("exclude")) {
					quickModel.setExcludeTables(Constants.replaceConstants(quickvo.getAttribute("exclude")));
				}

				// 实体bean
				nodeList = quickvo.getElementsByTagName("entity");
				if (nodeList.getLength() > 0) {
					entity = (Element) nodeList.item(0);
					quickModel.setEntityPackage(Constants.replaceConstants(entity.getAttribute("package")));
					if (entity.hasAttribute("substr")) {
						quickModel.setEntitySubstr(Constants.replaceConstants(entity.getAttribute("substr")));
					}
					// 是否包含抽象类
					if (entity.hasAttribute("has-abstract")) {
						quickModel.setHasAbstractEntity(Boolean.parseBoolean(entity.getAttribute("has-abstract")));
					}
					if (entity.hasAttribute("name")) {
						quickModel.setEntityName(Constants.replaceConstants(entity.getAttribute("name")));
					} else {
						quickModel.setEntityName("#{subName}");
					}
					quickModel.setHasEntity(true);
				}
				// vo(update 2020-10-15 单独vo时意义转为entity兼容旧模式)，如跟entity一起使用等于dto的概念
				nodeList = quickvo.getElementsByTagName("vo");
				if (nodeList.getLength() > 0) {
					vo = (Element) nodeList.item(0);
					// 存在entity和dto 形式，这时的vo等同于dto
					if (quickModel.isHasEntity()) {
						quickModel.setVoPackage(Constants.replaceConstants(vo.getAttribute("package")));
						if (vo.hasAttribute("substr")) {
							quickModel.setVoSubstr(Constants.replaceConstants(vo.getAttribute("substr")));
						}

						if (vo.hasAttribute("lombok")) {
							quickModel.setLombok(Boolean.parseBoolean(vo.getAttribute("lombok")));
						}

						if (vo.hasAttribute("lombok-chain")) {
							quickModel.setLombokChain(Boolean.parseBoolean(vo.getAttribute("lombok-chain")));
						}
						if (vo.hasAttribute("name")) {
							quickModel.setVoName(Constants.replaceConstants(vo.getAttribute("name")));
						} else {
							quickModel.setVoName("#{subName}");
						}
						quickModel.setHasVO(true);
					} // 只有vo，则用entity来代替
					else {
						quickModel.setEntityPackage(Constants.replaceConstants(vo.getAttribute("package")));
						if (vo.hasAttribute("substr")) {
							quickModel.setEntitySubstr(Constants.replaceConstants(vo.getAttribute("substr")));
						}

						if (vo.hasAttribute("name")) {
							quickModel.setEntityName(Constants.replaceConstants(vo.getAttribute("name")));
						} else {
							quickModel.setEntityName("#{subName}");
						}
						quickModel.setHasEntity(true);
					}
				}

				quickModels.add(quickModel);
			}
		}
		if (quickModels.isEmpty()) {
			logger.info("没有激活的任务可以执行!");
			return null;
		}
		configModel.setTasks(quickModels);
		nodeList = root.getElementsByTagName("primary-key");
		// 主键设置
		if (nodeList.getLength() > 0) {
			NodeList tables = ((Element) nodeList.item(0)).getElementsByTagName("table");
			Element table;
			String name;
			String sequence;
			// 默认为赋值模式
			String strategy;
			String generator;
			for (int i = 0; i < tables.getLength(); i++) {
				table = (Element) tables.item(i);
				name = table.getAttribute("name");
				sequence = null;
				strategy = "assign";
				generator = null;
				if (table.hasAttribute("strategy")) {
					strategy = table.getAttribute("strategy");
				}
				if (table.hasAttribute("sequence")) {
					sequence = table.getAttribute("sequence");
					strategy = "sequence";
				}

				// 生成器模式优先
				if (table.hasAttribute("generator")) {
					generator = table.getAttribute("generator");
					strategy = "generator";
					sequence = null;
					if (generator.equalsIgnoreCase("default")) {
						generator = Constants.PK_DEFAULT_GENERATOR;
					}
				}

				PrimaryKeyStrategy primaryKeyStrategy = new PrimaryKeyStrategy(name, strategy, sequence, generator);
				// force没有必要
				if (table.hasAttribute("force")) {
					primaryKeyStrategy.setForce(Boolean.parseBoolean(table.getAttribute("force")));
				}
				configModel.addPkGeneratorStrategy(primaryKeyStrategy);
			}
		}
		// 自定义数据匹配类型
		List<ColumnTypeMapping> typeMapping = new ArrayList<ColumnTypeMapping>();
		Constants.addDefaultTypeMapping(typeMapping);
		nodeList = root.getElementsByTagName("type-mapping");
		if (nodeList.getLength() > 0) {
			NodeList jdbcTypes = ((Element) nodeList.item(0)).getElementsByTagName("sql-type");
			if (jdbcTypes.getLength() > 0) {
				Element type;
				String javaType;
				String[] precision;
				String[] scale;
				for (int i = 0; i < jdbcTypes.getLength(); i++) {
					type = (Element) jdbcTypes.item(i);
					ColumnTypeMapping colTypeMapping = new ColumnTypeMapping();
					// 兼容老版本
					colTypeMapping.putNativeTypes(type.getAttribute("native-types").split("\\,"));

					if (type.hasAttribute("jdbc-type")) {
						colTypeMapping.setJdbcType(type.getAttribute("jdbc-type"));
					}

					if (type.hasAttribute("precision")) {
						if (StringUtil.isNotBlank(type.getAttribute("precision"))) {
							precision = type.getAttribute("precision").split("\\..");
							if (precision.length >= 2) {
								colTypeMapping.setPrecisionMin(Integer.valueOf(precision[0].trim()));
								colTypeMapping.setPrecisionMax(Integer.valueOf(precision[1].trim()));
							} else {
								colTypeMapping.setPrecisionMin(Integer.valueOf(precision[0].trim()));
								colTypeMapping.setPrecisionMax(Integer.valueOf(precision[0].trim()));
							}
						}
					}

					// 小数位
					if (type.hasAttribute("scale")) {
						if (StringUtil.isNotBlank(type.getAttribute("scale"))) {
							scale = type.getAttribute("scale").split("\\..");
							if (scale.length >= 2) {
								colTypeMapping.setScaleMin(Integer.valueOf(scale[0].trim()));
								colTypeMapping.setScaleMax(Integer.valueOf(scale[1].trim()));
							} else {
								colTypeMapping.setScaleMin(Integer.valueOf(scale[0].trim()));
								colTypeMapping.setScaleMax(Integer.valueOf(scale[0].trim()));
							}
						}
					}
					// 对应的java类型(对常规类型进行容错性处理)
					javaType = type.getAttribute("java-type");
					if (javaType.equalsIgnoreCase("BigDecimal")) {
						javaType = "java.math.BigDecimal";
					} else if (javaType.equalsIgnoreCase("BigInteger")) {
						javaType = "java.math.BigInteger";
					} else if (javaType.equalsIgnoreCase("LocalDate")) {
						javaType = "java.time.LocalDate";
					} else if (javaType.equalsIgnoreCase("LocalDateTime")) {
						javaType = "java.time.LocalDateTime";
					} else if (javaType.equalsIgnoreCase("LocalTime")) {
						javaType = "java.time.LocalTime";
					} else if (javaType.equalsIgnoreCase("Timestamp")) {
						javaType = "java.sql.Timestamp";
					} else if (javaType.equalsIgnoreCase("Date")) {
						javaType = "java.util.Date";
					}
					colTypeMapping.setJavaType(javaType);
					if (javaType.indexOf(".") != -1) {
						colTypeMapping.setResultType(javaType.substring(javaType.lastIndexOf(".") + 1));
					} else {
						colTypeMapping.setResultType(javaType);
					}
					typeMapping.add(colTypeMapping);
				}
			}
		}
		configModel.setTypeMapping(typeMapping);
		nodeList = root.getElementsByTagName("cascade");
		// 级联操作设置
		if (nodeList.getLength() > 0) {
			NodeList mainCascades = nodeList;
			String mainTable;
			Element mainCasade;
			for (int m = 0; m < mainCascades.getLength(); m++) {
				mainCasade = (Element) mainCascades.item(m);
				if (mainCasade.hasAttribute("main-table")) {
					mainTable = mainCasade.getAttribute("main-table");
					NodeList cascades = mainCasade.getElementsByTagName("table");
					Element cascadeElt;
					List cascadeModelList = new ArrayList<CascadeModel>();
					for (int i = 0; i < cascades.getLength(); i++) {
						cascadeElt = (Element) cascades.item(i);
						CascadeModel cascade = new CascadeModel();
						cascade.setTableName(cascadeElt.getAttribute("name"));
						if (cascadeElt.hasAttribute("delete")) {
							cascade.setDelete(Boolean.parseBoolean(cascadeElt.getAttribute("delete")));
						}
						// lazy load 的特定sql，可以自行定义,如:enabled=1附加条件
						if (cascadeElt.hasAttribute("load")) {
							cascade.setLoad(cascadeElt.getAttribute("load"));
						}
						// 新的配置模式
						if (cascadeElt.hasAttribute("update-cascade")) {
							cascade.setUpdateSql(cascadeElt.getAttribute("update-cascade"));
						}
						cascadeModelList.add(cascade);
					}
					configModel.addCascadeConfig(mainTable, cascadeModelList);
				}
			}
		}

		// 数据库
		DBHelper.loadDatasource(root.getElementsByTagName("datasource"));
		nodeList = root.getElementsByTagName("business-primary-key");
		// 表业务主键配置
		if (nodeList.getLength() > 0) {
			NodeList tables = ((Element) nodeList.item(0)).getElementsByTagName("table");
			Element tableElt;
			for (int m = 0; m < tables.getLength(); m++) {
				tableElt = (Element) tables.item(m);
				BusinessIdConfig bizIdConfig = new BusinessIdConfig();
				bizIdConfig.setTableName(tableElt.getAttribute("name"));
				bizIdConfig.setColumn(tableElt.getAttribute("column"));
				bizIdConfig.setGenerator(tableElt.getAttribute("generator"));
				if (bizIdConfig.getGenerator() != null && bizIdConfig.getGenerator().equalsIgnoreCase("redis")) {
					bizIdConfig.setGenerator(Constants.PK_REDIS_ID_GENERATOR);
				}
				if (tableElt.hasAttribute("related-column")) {
					String relatedColumn = tableElt.getAttribute("related-column");
					// 统一分割符
					bizIdConfig.setRelatedColumns(
							relatedColumn.replaceAll("\\;", ",").replaceAll("\\，", ",").split("\\,"));
				} else if (tableElt.hasAttribute("related-columns")) {
					String relatedColumns = tableElt.getAttribute("related-columns");
					// 统一分割符
					bizIdConfig.setRelatedColumns(
							relatedColumns.replaceAll("\\;", ",").replaceAll("\\，", ",").split("\\,"));
				}
				if (tableElt.hasAttribute("signature")) {
					bizIdConfig.setSignature(tableElt.getAttribute("signature"));
				}
				if (tableElt.hasAttribute("length")) {
					bizIdConfig.setLength(Integer.parseInt(tableElt.getAttribute("length")));
				}
				if (tableElt.hasAttribute("sequence-size")) {
					bizIdConfig.setSequenceSize(Integer.parseInt(tableElt.getAttribute("sequence-size")));
				}
				configModel.addBusinessId(bizIdConfig);
			}
		}
		return configModel;
	}
}
