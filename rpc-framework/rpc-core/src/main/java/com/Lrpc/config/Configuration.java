package com.Lrpc.config;

import com.Lrpc.compress.Compressor;
import com.Lrpc.compress.Impl.GzipCompressor;
import com.Lrpc.discovery.RegistryConfig;
import com.Lrpc.loadbalancer.LoadBalancer;
import com.Lrpc.loadbalancer.RoundRobinLoadBalancer;
import com.Lrpc.serialize.Serialize;
import com.Lrpc.utils.IdGenerator;
import com.caucho.hessian.io.Serializer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

@Data
@Slf4j
public class Configuration {
    // 配置信息-->端口号
    private int port = 8081;

    // 配置信息-->应用程序的名字
    private String appName = "rpc";

    // 分组信息
    private String group = "default";

    // 配置信息-->注册中心
    private RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");

    // 配置信息-->序列化协议
    private String serializeType = "jdk";

    // 配置信息-->压缩使用的协议
    private String compressType = "gzip";
    private Compressor  compressor = new GzipCompressor();

    // 配置信息-->id发射器
    public IdGenerator idGenerator = new IdGenerator(1, 2);

    // 配置信息-->负载均衡策略
    private LoadBalancer loadBalancer = new RoundRobinLoadBalancer();

    // 读取xml,dom4j
    public Configuration() {
        // 读取xml获得配置信息
        loadFromXml(this);
    }

    /**
     *  从配置文件读取xml  可以用dom4j，我们这里使用jdk自带的
     * @param configuration
     */
    private void loadFromXml(Configuration configuration) {
        try {
            // 获取一个document
            DocumentBuilderFactory  documentBuilderFactory = DocumentBuilderFactory.newInstance();
            //  关闭dtd校验
            documentBuilderFactory.setValidating(false);
            documentBuilderFactory.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream("lrpc.xml");
            Document doc = documentBuilder.parse(stream);

            // 获取一个xpath解析器
            XPathFactory  xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();


            // 解析一个表达式
            // 3、解析所有的标签
            configuration.setPort(resolvePort(doc, xpath));
            configuration.setAppName(resolveAppName(doc, xpath));
            configuration.setIdGenerator(resolveIdGenerator(doc, xpath));
            configuration.setRegistryConfig(resolveRegistryConfig(doc, xpath));


            // 处理使用的压缩方式和序列化方式
            configuration.setCompressType(resolveCompressType(doc, xpath));
            configuration.setSerializeType(resolveSerializeType(doc, xpath));

            // 配置新的压缩方式和序列化方式，并将其纳入工厂中
            ObjectWrapper<Compressor> compressorObjectWrapper = resolveCompressCompressor(doc, xpath);


            ObjectWrapper<Serialize> serializerObjectWrapper = resolveSerializer(doc, xpath);


            //  处理负载均衡器
            configuration.setLoadBalancer(resolveLoadBalancer(doc, xpath));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("未发现相关配置文件或解析配置文件异常将选用默认配置",e);
        }
    }

    /**
     * 解析端口号
     *
     * @param doc   文档
     * @param xpath xpath解析器
     * @return 端口号
     */
    private int resolvePort(Document doc, XPath xpath) {
        String expression = "/configuration/port";
        String portString = parseString(doc, xpath, expression);
        return Integer.parseInt(portString);
    }

    /**
     * 解析应用名称
     *
     * @param doc   文档
     * @param xpath xpath解析器
     * @return 应用名
     */
    private String resolveAppName(Document doc, XPath xpath) {
        String expression = "/configuration/appName";
        return parseString(doc, xpath, expression);
    }

    /**
     * 解析负载均衡器
     *
     * @param doc   文档
     * @param xpath xpath解析器
     * @return 负载均衡器实例
     */
    private LoadBalancer resolveLoadBalancer(Document doc, XPath xpath) {
        String expression = "/configuration/loadBalancer";
        return parseObject(doc, xpath, expression, null);
    }

    /**
     * 解析id发号器
     *
     * @param doc   文档
     * @param xpath xpath解析器
     * @return id发号器实例
     */
    private IdGenerator resolveIdGenerator(Document doc, XPath xpath) {
        String expression = "/configuration/idGenerator";
        String aClass = parseString(doc, xpath, expression, "class");
        String dataCenterId = parseString(doc, xpath, expression, "dataCenterId");
        String machineId = parseString(doc, xpath, expression, "MachineId");

        try {
            Class<?> clazz = Class.forName(aClass);
            Object instance = clazz.getConstructor(new Class[]{long.class, long.class})
                    .newInstance(Long.parseLong(dataCenterId), Long.parseLong(machineId));
            return (IdGenerator) instance;
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析注册中心
     *
     * @param doc   文档
     * @param xpath xpath解析器
     * @return RegistryConfig
     */
    private RegistryConfig resolveRegistryConfig(Document doc, XPath xpath) {
        String expression = "/configuration/registry";
        String url = parseString(doc, xpath, expression, "url");
        return new RegistryConfig(url);
    }

    /**
     * 解析压缩的具体实现
     *
     * @param doc   文档
     * @param xpath xpath解析器
     * @return ObjectWrapper<Compressor>
     */
    private ObjectWrapper<Compressor> resolveCompressCompressor(Document doc, XPath xpath) {
        String expression = "/configuration/compressor";
        Compressor compressor = parseObject(doc, xpath, expression, null);
        Byte code = Byte.valueOf(Objects.requireNonNull(parseString(doc, xpath, expression, "code")));
        String name = parseString(doc, xpath, expression, "name");
        return new ObjectWrapper<>(code,name,compressor);
    }




    /**
     * 解析压缩的算法名称
     * @param doc   文档
     * @param xpath xpath解析器
     * @return 压缩算法名称
     */
    private String resolveCompressType(Document doc, XPath xpath) {
        String expression = "/configuration/compressType";
        return parseString(doc, xpath, expression, "type");
    }




    /**
     * 解析序列化器
     * @param doc   文档
     * @param xpath xpath解析器
     * @return 序列化器
     */
    private ObjectWrapper<Serialize> resolveSerializer(Document doc, XPath xpath) {
        String expression = "/configuration/serializer";
        Serialize serializer = parseObject(doc, xpath, expression, null);
        Byte code = Byte.valueOf(Objects.requireNonNull(parseString(doc, xpath, expression, "code")));
        String name = parseString(doc, xpath, expression, "name");
        return new ObjectWrapper<>(code,name,serializer);
    }




    /**
     * 解析序列化的方式
     * @param doc   文档
     * @param xpath xpath解析器
     * @return 序列化的方式
     */
    private String resolveSerializeType(Document doc, XPath xpath) {
        String expression = "/configuration/serializeType";
        return parseString(doc, xpath, expression, "type");
    }



    /**
     * 获得一个节点文本值   <port>7777</>
     * @param doc        文档对象
     * @param xpath      xpath解析器
     * @param expression xpath表达式
     * @return 节点的值
     */
    private String parseString(Document doc,XPath xpath,String expression) {
        try {
            XPathExpression expr = xpath.compile(expression);
            // 我们的表达式可以帮我们获取节点
            Node targetNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
            return targetNode.getTextContent();
        } catch (XPathExpressionException e) {
            log.error("An exception occurred while parsing the expression.", e);
        }
        return null;
    }



    /**
     * 获得一个节点属性的值   <port num="7777"></>
     * @param document           文档对象
     * @param xPath         xpath解析器
     * @param expression    xpath表达式
     * @param attributeName 节点名称
     * @return 节点的值
     */
    private String parseString(Document document, XPath xPath,String  expression,String attributeName) {


        try {
            XPathExpression expr = xPath.compile(expression);
            // 我们的表达式可以帮助我们获取节点
            Node targetNode = (Node) expr.evaluate(document, XPathConstants.NODE);
            return targetNode.getAttributes().getNamedItem(attributeName).getNodeValue();
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }


    }


        /**
     * 解析一个节点，返回一个实例
     * @param document        文档对象
     * @param xPath      xpath解析器
     * @param expression xpath表达式
     * @param paramType  参数列表
     * @param param      参数
     * @param <T>        泛型
     * @return 配置的实例
     */
    private <T> T parseObject(Document document,XPath xPath,String  expression,Class<?>[] paramType,Object...  param) {
        try {
            XPathExpression expr = xPath.compile(expression);
            // 我们的表达式可以帮助我们获取节点
            Node targetNode = (Node)expr.evaluate(document, XPathConstants.NODE);
            String className = targetNode.getAttributes().getNamedItem("class").getNodeValue();//获取的东西 ：com.Lrpc.serialize.impl.HessianSerialize
            Class<?> aClass = Class.forName(className);
            Object instant = null;
            if( paramType == null){
                instant = aClass.getConstructor().newInstance();// 无参构造
            }else{
                instant = aClass.getConstructor(paramType).newInstance(param);//  有参构造
            }
            return  (T)instant;
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException |XPathExpressionException e) {
            log.error("解析表达式时发生异常", e);
        }
        return  null;
    }

    public static void main(String[] args) {
        Configuration  configuration = new Configuration();
    }
}
