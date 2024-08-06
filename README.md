## SpringBoot3-SpringSecurity6.2.5-jwt认证和授权代码教程

> jdk17
> 博客：https://blog.zukedog.cn/archives/1722941548098

### 认证

1. pom.xml文件引入依赖，在引入SpringSecurity依赖，没做任何配置前，此时访问所有接口都要登录，登录页面是SpringSecurity自带的。有一个默认的用户，用户名user，密码会在项目启动时在控制台打印。

   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
       <modelVersion>4.0.0</modelVersion>
       <parent>
           <groupId>org.springframework.boot</groupId>
           <artifactId>spring-boot-starter-parent</artifactId>
           <version>3.2.8</version>
           <relativePath/> <!-- lookup parent from repository -->
       </parent>
   
       <groupId>com.zuke</groupId>
       <artifactId>demo</artifactId>
       <version>0.0.1-SNAPSHOT</version>
       <name>demo</name>
       <description>demo</description>
   
       <properties>
           <java.version>17</java.version>
       </properties>
       <dependencies>
           <!-- jdk1.8以上需要加入依赖 -->
           <dependency>
               <groupId>javax.xml.bind</groupId>
               <artifactId>jaxb-api</artifactId>
               <version>2.3.0</version>
           </dependency>
           <dependency>
               <groupId>com.sun.xml.bind</groupId>
               <artifactId>jaxb-impl</artifactId>
               <version>2.3.0</version>
           </dependency>
           <dependency>
               <groupId>com.sun.xml.bind</groupId>
               <artifactId>jaxb-core</artifactId>
               <version>2.3.0</version>
           </dependency>
           <dependency>
               <groupId>javax.activation</groupId>
               <artifactId>activation</artifactId>
               <version>1.1.1</version>
           </dependency>
           <!-- mybatis-plus依赖 -->
           <dependency>
               <groupId>com.baomidou</groupId>
               <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
               <version>3.5.7</version>
           </dependency>
           <!-- mysql依赖 -->
           <dependency>
               <groupId>mysql</groupId>
               <artifactId>mysql-connector-java</artifactId>
               <version>8.0.33</version>
           </dependency>
           <!-- jwt依赖 -->
           <dependency>
               <groupId>io.jsonwebtoken</groupId>
               <artifactId>jjwt</artifactId>
               <version>0.9.0</version>
           </dependency>
           <!-- fastjson依赖 -->
           <dependency>
               <groupId>com.alibaba</groupId>
               <artifactId>fastjson</artifactId>
               <version>2.0.52</version>
           </dependency>
           <!-- redis依赖 -->
           <dependency>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-starter-data-redis</artifactId>
           </dependency>
           <!-- SpringSecurity依赖 -->
           <dependency>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-starter-security</artifactId>
           </dependency>
           <!-- web开发依赖 -->
           <dependency>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-starter-web</artifactId>
           </dependency>
           <!-- lombok依赖 -->
           <dependency>
               <groupId>org.projectlombok</groupId>
               <artifactId>lombok</artifactId>
               <optional>true</optional>
           </dependency>
           <!-- SpringBoot测试依赖 -->
           <dependency>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-starter-test</artifactId>
               <scope>test</scope>
           </dependency>
       </dependencies>
   
       <build>
           <plugins>
               <plugin>
                   <groupId>org.springframework.boot</groupId>
                   <artifactId>spring-boot-maven-plugin</artifactId>
                   <configuration>
                       <excludes>
                           <exclude>
                               <groupId>org.projectlombok</groupId>
                               <artifactId>lombok</artifactId>
                           </exclude>
                       </excludes>
                   </configuration>
               </plugin>
           </plugins>
       </build>
   
   </project>
   ```



2. 定义端口、mysql数据源（此处是mysql8）、redis数据源、MybatisPlus配置

   src/main/resources/application.yml

   ```yaml
   server:
     port: 8800
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/spring-security?characterEncoding=utf8&serverTimezone=UTC
       username: root
       password: GEfSuDyTeu7MvZyc
       driver-class-name: com.mysql.cj.jdbc.Driver
     data:
       redis:
         #Redis服务器地址
         host: localhost
         #Redis服务器连接端口
         port: 6379
         #Redis如果有密码,需要配置, 没有密码就不要写
         password: 123456
         #Redis数据库索引（默认为0）
         database: 0
         #连接超时时间（毫秒）
         timeout: 1800000
         jedis:
           pool:
             #连接池最大连接数（使用负值表示没有限制）
             max-active: 20
             #最大阻塞等待时间(负数表示没限制)
             max-wait: -1
             #连接池中的最大空闲连接
             max-idle: 5
             #连接池中的最小空闲连接
             min-idle: 0
   mybatis-plus:
     mapper-locations: classpath:/mapper/*.xml
   ```



3. redis相关配置和工具类

   src/main/java/com/example/demo/utils/FastJsonRedisSerializer.java

   ```java
   import com.alibaba.fastjson.JSON;
   import com.alibaba.fastjson.parser.ParserConfig;
   import com.alibaba.fastjson.serializer.SerializerFeature;
   import com.fasterxml.jackson.databind.JavaType;
   import com.fasterxml.jackson.databind.type.TypeFactory;
   import org.springframework.data.redis.serializer.RedisSerializer;
   import org.springframework.data.redis.serializer.SerializationException;
   
   import java.nio.charset.Charset;
   
   
   public class FastJsonRedisSerializer<T> implements RedisSerializer<T> {
       public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
       private Class<T> clazz;
   
       static {
           ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
       }
   
       public FastJsonRedisSerializer(Class<T> clazz) {
           super();
           this.clazz = clazz;
       }
   
       @Override
       public byte[] serialize(T t) throws SerializationException {
           if (t == null) {
               return new byte[0];
           }
           return JSON.toJSONString(t,
                   SerializerFeature.WriteClassName).getBytes(DEFAULT_CHARSET);
       }
   
       @Override
       public T deserialize(byte[] bytes) throws SerializationException {
           if (bytes == null || bytes.length <= 0) {
               return null;
           }
           String str = new String(bytes, DEFAULT_CHARSET);
           return JSON.parseObject(str, clazz);
       }
   
       protected JavaType getJavaType(Class<?> clazz) {
           return TypeFactory.defaultInstance().constructType(clazz);
       }
   }
   ```

   src/main/java/com/example/demo/utils/RedisCache.java

   ```java
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.data.redis.core.BoundSetOperations;
   import org.springframework.data.redis.core.HashOperations;
   import org.springframework.data.redis.core.RedisTemplate;
   import org.springframework.data.redis.core.ValueOperations;
   import org.springframework.stereotype.Component;
   
   import java.util.*;
   import java.util.concurrent.TimeUnit;
   
   //redis工具类
   
   @SuppressWarnings(value = {"unchecked", "rawtypes"})
   @Component
   public class RedisCache {
       @Autowired
       public RedisTemplate redisTemplate;
   
       /**
        * 缓存基本的对象，Integer、String、实体类等
        *
        * @param key   缓存的键值
        * @param value 缓存的值
        */
       public <T> void setCacheObject(final String key, final T value) {
           redisTemplate.opsForValue().set(key, value);
       }
   
       /**
        * 缓存基本的对象，Integer、String、实体类等
        *
        * @param key      缓存的键值
        * @param value    缓存的值
        * @param timeout  时间
        * @param timeUnit 时间颗粒度
        */
       public <T> void setCacheObject(final String key, final T value, final
       Integer timeout, final TimeUnit timeUnit) {
           redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
       }
   
       /**
        * 设置有效时间
        *
        * @param key     Redis键
        * @param timeout 超时时间
        * @return true=设置成功；false=设置失败
        */
       public boolean expire(final String key, final long timeout) {
           return expire(key, timeout, TimeUnit.SECONDS);
       }
   
       /**
        * 设置有效时间
        *
        * @param key     Redis键
        * @param timeout 超时时间
        * @param unit    时间单位
        * @return true=设置成功；false=设置失败
        */
       public boolean expire(final String key, final long timeout, final TimeUnit
               unit) {
           return redisTemplate.expire(key, timeout, unit);
       }
   
       /**
        * 获得缓存的基本对象。
        *
        * @param key 缓存键值
        * @return 缓存键值对应的数据
        */
       public <T> T getCacheObject(final String key) {
           ValueOperations<String, T> operation = redisTemplate.opsForValue();
           return operation.get(key);
       }
   
       /**
        * 删除单个对象
        *
        * @param key
        */
       public boolean deleteObject(final String key) {
           return redisTemplate.delete(key);
       }
   
       /**
        * 删除集合对象
        *
        * @param collection 多个对象
        * @return
        */
       public long deleteObject(final Collection collection) {
           return redisTemplate.delete(collection);
       }
   
       /**
        * 缓存List数据
        *
        * @param key      缓存的键值
        * @param dataList 待缓存的List数据
        * @return 缓存的对象
        */
       public <T> long setCacheList(final String key, final List<T> dataList) {
           Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
           return count == null ? 0 : count;
       }
   
       /**
        * 获得缓存的list对象
        *
        * @param key 缓存的键值
        * @return 缓存键值对应的数据
        */
       public <T> List<T> getCacheList(final String key) {
           return redisTemplate.opsForList().range(key, 0, -1);
       }
   
       /**
        * 缓存Set
        *
        * @param key     缓存键值
        * @param dataSet 缓存的数据
        * @return 缓存数据的对象
        */
       public <T> BoundSetOperations<String, T> setCacheSet(final String key, final
       Set<T> dataSet) {
           BoundSetOperations<String, T> setOperation =
                   redisTemplate.boundSetOps(key);
           Iterator<T> it = dataSet.iterator();
           while (it.hasNext()) {
               setOperation.add(it.next());
           }
           return setOperation;
       }
   
       /**
        * 获得缓存的set
        *
        * @param key
        * @return
        */
       public <T> Set<T> getCacheSet(final String key) {
           return redisTemplate.opsForSet().members(key);
       }
   
       /**
        * 缓存Map
        *
        * @param key
        * @param dataMap
        */
       public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
           if (dataMap != null) {
               redisTemplate.opsForHash().putAll(key, dataMap);
           }
       }
   
       /**
        * 获得缓存的Map
        *
        * @param key
        * @return
        */
       public <T> Map<String, T> getCacheMap(final String key) {
           return redisTemplate.opsForHash().entries(key);
       }
   
       /**
        * 往Hash中存入数据
        *
        * @param key   Redis键
        * @param hKey  Hash键
        * @param value 值
        */
       public <T> void setCacheMapValue(final String key, final String hKey, final
       T value) {
           redisTemplate.opsForHash().put(key, hKey, value);
       }
   
       /**
        * 获取Hash中的数据
        *
        * @param key  Redis键
        * @param hKey Hash键
        * @return Hash中的对象
        */
       public <T> T getCacheMapValue(final String key, final String hKey) {
           HashOperations<String, String, T> opsForHash =
                   redisTemplate.opsForHash();
           return opsForHash.get(key, hKey);
       }
   
       /**
        * 删除Hash中的数据
        *
        * @param key
        * @param hkey
        */
       public void delCacheMapValue(final String key, final String hkey) {
           HashOperations hashOperations = redisTemplate.opsForHash();
           hashOperations.delete(key, hkey);
       }
   
       /**
        * 获取多个Hash中的数据
        *
        * @param key   Redis键
        * @param hKeys Hash键集合
        * @return Hash对象集合
        */
       public <T> List<T> getMultiCacheMapValue(final String key, final
       Collection<Object> hKeys) {
           return redisTemplate.opsForHash().multiGet(key, hKeys);
       }
   
       /**
        * 获得缓存的基本对象列表
        *
        * @param pattern 字符串前缀
        * @return 对象列表
        */
       public Collection<String> keys(final String pattern) {
           return redisTemplate.keys(pattern);
       }
   }
   ```

   src/main/java/com/example/demo/config/RedisConfig.java

   ```java
   import com.example.demo.utils.FastJsonRedisSerializer;
   import org.springframework.context.annotation.Bean;
   import org.springframework.context.annotation.Configuration;
   import org.springframework.data.redis.connection.RedisConnectionFactory;
   import org.springframework.data.redis.core.RedisTemplate;
   import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
   import org.springframework.data.redis.serializer.StringRedisSerializer;
   
   
   @Configuration
   public class RedisConfig {
       @Bean
       @SuppressWarnings(value = { "unchecked", "rawtypes" })
       public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory
                                                                  connectionFactory)
       {
           RedisTemplate<Object, Object> template = new RedisTemplate<>();
           template.setConnectionFactory(connectionFactory);
           FastJsonRedisSerializer serializer = new
                   FastJsonRedisSerializer(Object.class);
           // 使用StringRedisSerializer来序列化和反序列化redis的key值
           template.setKeySerializer(new StringRedisSerializer());
           template.setValueSerializer(serializer);
           // Hash的key也采用StringRedisSerializer的序列化方式
           template.setHashKeySerializer(new StringRedisSerializer());
           template.setHashValueSerializer(serializer);
           template.afterPropertiesSet();
           return template;
       }
   }
   ```



4. jwt工具类和其他工具类

   src/main/java/com/example/demo/utils/JwtUtil.java

   ```java
   import io.jsonwebtoken.Claims;
   import io.jsonwebtoken.JwtBuilder;
   import io.jsonwebtoken.Jwts;
   import io.jsonwebtoken.SignatureAlgorithm;
   
   import javax.crypto.SecretKey;
   import javax.crypto.spec.SecretKeySpec;
   import java.util.Base64;
   import java.util.Date;
   import java.util.UUID;
   
   public class JwtUtil {
       //有效期为
       public static final Long JWT_TTL = 60 * 60 * 1000L;// 60 * 60 *1000 一个小时
       //设置秘钥明文
       public static final String JWT_KEY = "zuke";
   
       public static String getUUID() {
           String token = UUID.randomUUID().toString().replaceAll("-", "");
           return token;
       }
   
       /**
        * 生成jtw
        *
        * @param subject token中要存放的数据（json格式）
        * @return
        */
       public static String createJWT(String subject) {
           JwtBuilder builder = getJwtBuilder(subject, null, getUUID());// 设置过期时间
           return builder.compact();
       }
   
       /**
        * 生成自定义过期时间的jtw
        *
        * @param subject   token中要存放的数据（json格式）
        * @param ttlMillis token超时时间
        * @return
        */
       public static String createJWT(String subject, Long ttlMillis) {
           JwtBuilder builder = getJwtBuilder(subject, ttlMillis, getUUID());// 设置过期时间
           return builder.compact();
       }
   
       /**
        * 创建token
        *
        * @param id
        * @param subject
        * @param ttlMillis
        * @return
        */
       public static String createJWT(String id, String subject, Long ttlMillis) {
           JwtBuilder builder = getJwtBuilder(subject, ttlMillis, id);// 设置过期时间
           return builder.compact();
       }
   
   
       /**
        * 生成加密后的秘钥 secretKey
        *
        * @return
        */
       public static SecretKey generalKey() {
           byte[] encodedKey = Base64.getDecoder().decode(JwtUtil.JWT_KEY);
           SecretKey key = new SecretKeySpec(encodedKey, 0, encodedKey.length,
                   "AES");
           return key;
       }
   
       /**
        * 解析jwt
        *
        * @param jwt
        * @return
        * @throws Exception
        */
       public static Claims parseJWT(String jwt) throws Exception {
           SecretKey secretKey = generalKey();
           return Jwts.parser()
                   .setSigningKey(secretKey)
                   .parseClaimsJws(jwt)
                   .getBody();
       }
   
       private static JwtBuilder getJwtBuilder(String subject, Long ttlMillis,
                                               String uuid) {
           SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
           SecretKey secretKey = generalKey();
           long nowMillis = System.currentTimeMillis();
           Date now = new Date(nowMillis);
           if (ttlMillis == null) {
               ttlMillis = JwtUtil.JWT_TTL;
           }
           long expMillis = nowMillis + ttlMillis;
           Date expDate = new Date(expMillis);
           return Jwts.builder()
                   .setId(uuid) //唯一的ID
                   .setSubject(subject) // 主题 可以是JSON数据
                   .setIssuer("zuke") // 签发者
                   .setIssuedAt(now) // 签发时间
                   .signWith(signatureAlgorithm, secretKey) //使用HS256对称加密算法签 名, 第二个参数为秘钥
                   .setExpiration(expDate);
       }
   
       public static void main(String[] args) throws Exception {
           String token = createJWT("123");
           System.out.println(token);
           Claims claims = parseJWT(token);
           System.out.println(claims);
       }
   
   }
   ```

   src/main/java/com/example/demo/utils/WebUtils.java

   ```java
   import jakarta.servlet.http.HttpServletResponse;
   import java.io.IOException;
   
   
   public class WebUtils {
       /**
        * 将字符串渲染到客户端
        *
        * @param response 渲染对象
        * @param string   待渲染的字符串
        * @return null
        */
       public static String renderString(HttpServletResponse response, String
               string) {
           try {
               response.setStatus(200);
               response.setContentType("application/json");
               response.setCharacterEncoding("utf-8");
               response.getWriter().print(string);
           } catch (IOException e) {
               e.printStackTrace();
           }
           return null;
       }
   }
   ```



5. 数据库sys_user表

   ```yaml
   SET NAMES utf8mb4;
   SET FOREIGN_KEY_CHECKS = 0;
   
   -- ----------------------------
   -- Table structure for sys_user
   -- ----------------------------
   DROP TABLE IF EXISTS `sys_user`;
   CREATE TABLE `sys_user`  (
     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
     `user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NULL' COMMENT '用户名',
     `nick_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NULL' COMMENT '昵称',
     `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NULL' COMMENT '密码',
     `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
     `email` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
     `phonenumber` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
     `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户性别（0男，1女，2未知）',
     `avatar` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
     `user_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '1' COMMENT '用户类型（0管理员，1普通用户）',
     `create_by` bigint NULL DEFAULT NULL COMMENT '创建人的用户id',
     `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
     `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
     `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
     `del_flag` int NULL DEFAULT 0 COMMENT '删除标志（0代表未删除，1代表已删除）',
     PRIMARY KEY (`id`) USING BTREE
   ) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;
   
   SET FOREIGN_KEY_CHECKS = 1;
   ```



6. 实体类

   src/main/java/com/example/demo/entity/User.java

   ```java
   import com.baomidou.mybatisplus.annotation.IdType;
   import com.baomidou.mybatisplus.annotation.TableField;
   import com.baomidou.mybatisplus.annotation.TableId;
   import com.baomidou.mybatisplus.annotation.TableName;
   import lombok.AllArgsConstructor;
   import lombok.Data;
   import lombok.NoArgsConstructor;
   
   import java.io.Serializable;
   import java.util.Date;
   
   @Data
   @AllArgsConstructor
   @NoArgsConstructor
   @TableName(value = "sys_user")
   public class User implements Serializable {
       private static final long serialVersionUID = -40356785423868312L;
       /**
        * 主键
        */
       @TableId(value = "id",type = IdType.AUTO)
       private Long id;
       /**
        * 用户名
        */
       private String userName;
       /**
        * 昵称
        */
       private String nickName;
       /**
        * 密码
        */
       private String password;
       /**
        * 账号状态（0正常 1停用）
        */
       private String status;
       /**
        * 邮箱
        */
       private String email;
       /**
        * 手机号
        */
       private String phonenumber;
       /**
        * 用户性别（0男，1女，2未知）
        */
       private String sex;
       /**
        * 头像
        */
       private String avatar;
       /**
        * 用户类型（0管理员，1普通用户）
        */
       private String userType;
       /**
        * 创建人的用户id
        */
       private Long createBy;
       /**
        * 创建时间
        */
       private Date createTime;
       /**
        * 更新人
        */
       private Long updateBy;
       /**
        * 更新时间
        */
       private Date updateTime;
       /**
        * 删除标志（0代表未删除，1代表已删除）
        */
       private Integer delFlag;
   }
   ```

   mapper

   src/main/java/com/example/demo/mapper/UserMapper.java

   ```java
   import com.baomidou.mybatisplus.core.mapper.BaseMapper;
   import com.example.demo.entity.User;
   import org.apache.ibatis.annotations.Mapper;
   
   @Mapper
   public interface UserMapper extends BaseMapper<User> {
   }
   ```



7. 响应类

   src/main/java/com/example/demo/entity/RespBean.java

   ```java
   import lombok.AllArgsConstructor;
   import lombok.Data;
   import lombok.NoArgsConstructor;
   
   //封装响应信息
   
   @Data
   @AllArgsConstructor
   @NoArgsConstructor
   public class RespBean {
   
       private long code;
       private String msg;
       private Object data;
   
       //成功
       public static RespBean success() {
           return new RespBean(RespBeanEnum.success.getCode(), RespBeanEnum.success.getMsg(), null);
       }
   
       //成功，同时携带自定义响应信息
       public static RespBean success(RespBeanEnum respBeanEnum) {
           return new RespBean(respBeanEnum.getCode(), respBeanEnum.getMsg(), null);
       }
   
       //成功，同时携带数据返回
       public static RespBean success(Object data) {
           return new RespBean(RespBeanEnum.success.getCode(), RespBeanEnum.success.getMsg(), data);
       }
   
       //成功，同时携带自定义响应信息和数据返回
       public static RespBean success(RespBeanEnum respBeanEnum, Object data) {
           return new RespBean(respBeanEnum.getCode(), respBeanEnum.getMsg(), data);
       }
   
       //失败
       public static RespBean error(RespBeanEnum respBeanEnum) {
           return new RespBean(respBeanEnum.getCode(), respBeanEnum.getMsg(), null);
       }
   
       //失败，同时携带数据返回
       public static RespBean error(RespBeanEnum respBeanEnum, Object data) {
           return new RespBean(respBeanEnum.getCode(), respBeanEnum.getMsg(), data);
       }
   }
   ```

   src/main/java/com/example/demo/entity/RespBeanEnum.java

   ```java
   import lombok.AllArgsConstructor;
   import lombok.Getter;
   
   //响应状态码和信息定义
   
   @Getter
   @AllArgsConstructor
   public enum RespBeanEnum {
   
       //通用信息
       success(200, "操作成功"),
       ERROR(500, "服务端异常"),
   
       //用户登录
       LOGIN_SUCCESS(200, "登录成功"),
       LOGOUT_SUCCESS(200, "退出登录成功"),
       LOGIN_ERROR(400, "账号或密码错误");
   
       private final Integer code;
       private final String msg;
   }
   ```



8. SecurityConfig

   src/main/java/com/example/demo/config/SecurityConfig.java

   ```java
   import com.example.demo.filter.JwtAuthenticationTokenFilter;
   import jakarta.annotation.Resource;
   import org.springframework.context.annotation.Bean;
   import org.springframework.context.annotation.Configuration;
   import org.springframework.security.authentication.AuthenticationManager;
   import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
   import org.springframework.security.config.annotation.web.builders.HttpSecurity;
   import org.springframework.security.config.http.SessionCreationPolicy;
   import org.springframework.security.core.Authentication;
   import org.springframework.security.core.AuthenticationException;
   import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
   import org.springframework.security.crypto.password.PasswordEncoder;
   import org.springframework.security.web.SecurityFilterChain;
   import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
   
   import static org.springframework.security.config.Customizer.withDefaults;
   
   
   @Configuration
   public class SecurityConfig {
       @Resource
       private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
   
       @Bean
       public PasswordEncoder passwordEncoder() {
           //BCrypt 哈希算法来加密密码
           return new BCryptPasswordEncoder();
       }
   
       @Bean
       public AuthenticationManager authManager(HttpSecurity http) throws Exception {
           AuthenticationManagerBuilder authenticationManagerBuilder =
                   http.getSharedObject(AuthenticationManagerBuilder.class);
           // 在这里配置用户认证方式，比如从数据库中加载用户等
           return authenticationManagerBuilder.build();
       }
   
       @Bean
       public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
           http
                   //关闭 CSRF
                   .csrf(csrf -> csrf.disable())
                   //不通过 Session 获取 SecurityContext
                   .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                   .authorizeHttpRequests(authz -> authz
                           //对于登录接口允许匿名访问
                           .requestMatchers("/user/login").permitAll()
                           //其他所有请求需要认证
                           .anyRequest().authenticated())
                   .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
   
           return http.build();
       }
   
   }
   ```

9. LoginUser

   src/main/java/com/example/demo/entity/LoginUser.java

   ```java
   import lombok.AllArgsConstructor;
   import lombok.Data;
   import lombok.NoArgsConstructor;
   import org.springframework.security.core.GrantedAuthority;
   import org.springframework.security.core.userdetails.UserDetails;
   
   import java.util.Collection;
   import java.util.List;
   
   
   @Data
   @AllArgsConstructor
   @NoArgsConstructor
   public class LoginUser implements UserDetails {
   
       private User user;
   
       //获取权限信息
       @Override
       public Collection<? extends GrantedAuthority> getAuthorities() {
           return List.of();
       }
   
       @Override
       public String getPassword() {
           return user.getPassword();
       }
   
       @Override
       public String getUsername() {
           return user.getUserName();
       }
   
       @Override
       public boolean isAccountNonExpired() {
           return true;
       }
   
       @Override
       public boolean isAccountNonLocked() {
           return true;
       }
   
       @Override
       public boolean isCredentialsNonExpired() {
           return true;
       }
   
       @Override
       public boolean isEnabled() {
           return true;
       }
   }
   ```



10. UserDetailsServiceImpl

    src/main/java/com/example/demo/service/impl/UserDetailsServiceImpl.java

    ```java
    import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
    import com.example.demo.entity.LoginUser;
    import com.example.demo.entity.User;
    import com.example.demo.mapper.UserMapper;
    import jakarta.annotation.Resource;
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.security.core.userdetails.UserDetailsService;
    import org.springframework.security.core.userdetails.UsernameNotFoundException;
    import org.springframework.stereotype.Service;
    import java.util.Objects;
    
    
    @Service
    public class UserDetailsServiceImpl implements UserDetailsService {
        @Resource
        private UserMapper userMapper;
    
        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            //查询用户信息（认证）
            User user = userMapper.selectOne(new QueryWrapper<User>().eq("user_name", username));
            //如果没有查询到用户，就抛出异常
            if (Objects.isNull(user)) {
                //此异常会被spring security的异常过滤器捕获到
                throw new RuntimeException("用户名或者密码错误");
            }
    
            //todo 查询对应的权限信息（授权）
    
            //把数据封装成userDetails返回
            return new LoginUser(user);
        }
    }
    ```



11. 自定义过滤器JwtAuthenticationTokenFilter

    src/main/java/com/example/demo/filter/JwtAuthenticationTokenFilter.java

    ```java
    import com.alibaba.fastjson.JSONObject;
    import com.example.demo.entity.LoginUser;
    import com.example.demo.utils.JwtUtil;
    import com.example.demo.utils.RedisCache;
    import io.jsonwebtoken.Claims;
    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.stereotype.Component;
    import org.springframework.util.StringUtils;
    import org.springframework.web.filter.OncePerRequestFilter;
    
    import java.io.IOException;
    import java.util.Objects;
    
    //我们需要自定义一个过滤器，这个过滤器会去获取请求头中的token，对token进行解析取出其中的userid。使用userid去redis中获取对应的LoginUser对象,然后封装Authentication对象存入SecurityContextHolder
    //原版过滤器有时候有问题，这里选择继承spring提供的过滤器OncePerRequestFilter
    @Component
    public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
        @Autowired
        private RedisCache redisCache;
    
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            //获取token
            String token = request.getHeader("token");
            if (!StringUtils.hasText(token)) {
                //未登录，放行，后面的过滤器会检测SecurityContextHolder是否存有该用户
                filterChain.doFilter(request, response);
                return;
            }
            //解析token
            String userid;
            try {
                Claims claims = JwtUtil.parseJWT(token);
                userid = claims.getSubject();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("token非法");
            }
            //从redis中获取用户信息
            String redisKey = "login:" + userid;
            Object userObject = redisCache.getCacheObject(redisKey);
            if (Objects.isNull(userObject)) {
                throw new RuntimeException("用户未登录");
            }
            LoginUser loginUser = ((JSONObject) userObject).toJavaObject(LoginUser.class);
            //存入SecurityContextHolder
            //TODO 获取权限信息封装到Authentication中
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginUser, null, null);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            //放行
            filterChain.doFilter(request, response);
        }
    }
    ```



12. LoginService

    src/main/java/com/example/demo/service/LoginService.java

    ```java
    import com.example.demo.entity.RespBean;
    import com.example.demo.entity.User;
    
    
    public interface LoginService {
        RespBean logout();
        RespBean login(User user);
    }
    ```



13. 登录和退出功能实现

    LoginServiceImpl

    src/main/java/com/example/demo/service/impl/LoginServiceImpl.java

    ```java
    import com.example.demo.entity.LoginUser;
    import com.example.demo.entity.RespBean;
    import com.example.demo.entity.RespBeanEnum;
    import com.example.demo.entity.User;
    import com.example.demo.service.LoginService;
    import com.example.demo.utils.JwtUtil;
    import com.example.demo.utils.RedisCache;
    import jakarta.annotation.Resource;
    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.stereotype.Service;
    
    import java.util.HashMap;
    import java.util.Objects;
    import java.util.concurrent.TimeUnit;
    
    @Service
    public class LoginServiceImpl implements LoginService {
        @Resource
        private AuthenticationManager authenticationManager;
        @Resource
        private RedisCache redisCache;
    
        @Override
        public RespBean logout() {
            //获取SecurityContextHolder中的用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            String userid = loginUser.getUser().getId().toString();
            //删除redis中的值
            redisCache.deleteObject("login:" + userid);
            return RespBean.success(RespBeanEnum.LOGOUT_SUCCESS);
        }
    
        @Override
        public RespBean login(User user) {
            //通过AuthenticationManager的authenticate方法来进行用户认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUserName(), user.getPassword())
            );
            //如果认证没成功，给出相应提示
            if (Objects.isNull(authentication)) {
                throw new RuntimeException("登录失败");
            }
            //如果认证成功的话要生成一个jwt，放入响应中返回，并且把完整的用户信息出入redis，userId作为key
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            String userId = loginUser.getUser().getId().toString();
            String jwt = JwtUtil.createJWT(userId);
            HashMap<String, String> map = new HashMap<>();
            map.put("jwt", jwt);
            redisCache.setCacheObject("login:" + userId, loginUser, 30, TimeUnit.MINUTES);
            return RespBean.success(RespBeanEnum.LOGIN_SUCCESS, map);
        }
    }
    ```



### 授权

1. SecurityConfig加入注释，启用授权

   ```java
   @EnableWebSecurity
   @EnableGlobalMethodSecurity(prePostEnabled = true)
   ```



2. 去LoginController修改测试方法

   ```java
   @GetMapping("/hi")
   //如果用户用test权限，就让该用户访问
   @PreAuthorize("hasAuthority('test')")
   public RespBean hi() {
       return RespBean.success("hi");
   }
   ```



3. 封装权限信息

   修改LoginUser，添加属性

   ```java
   //存储权限信息
   private List<String> permissions;
   ```

   在UserDetailsServiceImpl封装权限信息（这里是硬编码，后面会调整）

   ```java
   //查询对应的权限信息（授权）
   List<String> list = new ArrayList<>(Arrays.asList("test", "admin"));
   
   //把数据封装成userDetails返回
   return new LoginUser(user, list);
   ```

   修改LoginUser的getAuthorities()方法

   ```java
   //redis处于安全考虑，不让这个成员变量序列化，防止报异常，加入注解忽略
   @JSONField(serialize = false)
   private List<SimpleGrantedAuthority> authorities;
   
   public LoginUser(User user, List<String> permissions) {
       this.user = user;
       this.permissions = permissions;
   }
   
   //获取权限信息
   @Override
   public Collection<? extends GrantedAuthority> getAuthorities() {
       //把permissions中，String类型的权限信息封装成它的实现类SimpleGrantedAuthority
       if (authorities != null) {
           return authorities;
       }
       authorities = permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
       return authorities;
   }
   ```

   修改认证过程，封装权限，JwtAuthenticationTokenFilter

   ```java
   //获取权限信息封装到Authentication中
   UsernamePasswordAuthenticationToken authenticationToken =
           new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
   SecurityContextHolder.getContext().setAuthentication(authenticationToken);
   ```

   下面进行测试，访问接口

   权限正确时

   ![image-20240806140430685](https://imgbed.zukedog.cn/i/2024/08/06/n7zywj-2.png)

   修改权限

   ```java
   @GetMapping("/hi")
   //如果用户用test权限，就让该用户访问
   @PreAuthorize("hasAuthority('test111')")
   public RespBean hi() {
       return RespBean.success("hi");
   }
   ```

   此时再访问接口就会报403



4. 优化：如何从数据库查询权限信息进行疯转

    1. RBAC权限模型：RBAC权限模型（Role-Based Access Control）即：基于角色的权限控制。这是目前最常被开发者使用 也是相对易用、通用权限模型。

       建立权限表

       ```sql
       USE `spring-security`;
       
       DROP TABLE IF EXISTS `sys_menu`;
       CREATE TABLE `sys_menu` (
       `id` bigint(20) NOT NULL AUTO_INCREMENT,
       `menu_name` varchar(64) NOT NULL DEFAULT 'NULL' COMMENT '菜单名',
       `path` varchar(200) DEFAULT NULL COMMENT '路由地址',
       `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
       `visible` char(1) DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
       `status` char(1) DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
       `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
       `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
       `create_by` bigint(20) DEFAULT NULL,
       `create_time` datetime DEFAULT NULL,
       `update_by` bigint(20) DEFAULT NULL,
       `update_time` datetime DEFAULT NULL,
       `del_flag` int(11) DEFAULT '0' COMMENT '是否删除（0未删除 1已删除）',
       `remark` varchar(500) DEFAULT NULL COMMENT '备注',
       PRIMARY KEY (`id`)
       ) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';
       
       DROP TABLE IF EXISTS `sys_role`;
       CREATE TABLE `sys_role` (
       `id` bigint(20) NOT NULL AUTO_INCREMENT,
       `name` varchar(128) DEFAULT NULL,
       `role_key` varchar(100) DEFAULT NULL COMMENT '角色权限字符串',
       `status` char(1) DEFAULT '0' COMMENT '角色状态（0正常 1停用）',
       `del_flag` int(1) DEFAULT '0' COMMENT 'del_flag',
       `create_by` bigint(200) DEFAULT NULL,
       `create_time` datetime DEFAULT NULL,
       `update_by` bigint(200) DEFAULT NULL,
       `update_time` datetime DEFAULT NULL,
       `remark` varchar(500) DEFAULT NULL COMMENT '备注',
       PRIMARY KEY (`id`)
       ) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
       
       DROP TABLE IF EXISTS `sys_role_menu`;
       CREATE TABLE `sys_role_menu` (
       `role_id` bigint(200) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
       `menu_id` bigint(200) NOT NULL DEFAULT '0' COMMENT '菜单id',
       PRIMARY KEY (`role_id`,`menu_id`)
       ) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4;
       
       DROP TABLE IF EXISTS `sys_user_role`;
       CREATE TABLE `sys_user_role` (
       `user_id` bigint(200) NOT NULL AUTO_INCREMENT COMMENT '用户id',
       `role_id` bigint(200) NOT NULL DEFAULT '0' COMMENT '角色id',
       PRIMARY KEY (`user_id`,`role_id`)
       ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
       ```

       表测试数据

       ![image-20240806142800680](https://imgbed.zukedog.cn/i/2024/08/06/nlv0q8-2.png)

       ![image-20240806142841515](https://imgbed.zukedog.cn/i/2024/08/06/nmcjcz-2.png)

       ![image-20240806142857112](https://imgbed.zukedog.cn/i/2024/08/06/nmfpiw-2.png)

       ![image-20240806142915859](https://imgbed.zukedog.cn/i/2024/08/06/nmjq8m-2.png)



      ![image-20240806142929168](https://imgbed.zukedog.cn/i/2024/08/06/nmv3eh-2.png)

      测试查询SQL

      ```sql
      # 根据 userid 查询 permissions 对应的role和menu都必须是正常状态
      # DISTINCT是去重，因为角色的权限可能是重复的
      SELECT
        DISTINCT m.`perms`
      FROM
        sys_user_role ur
        LEFT JOIN `sys_role` r ON ur.`role_id` = r.`id`
        LEFT JOIN `sys_role_menu` rm ON ur.`role_id` = rm.`role_id`
        LEFT JOIN `sys_menu` m ON m.`id` = rm.`menu_id`
      WHERE
        user_id = 2
        AND r.`status` = 0
        AND m.`status` = 0
      ```

2. 创建菜单表(Menu)实体类

   ```java
   import com.baomidou.mybatisplus.annotation.TableId;
   import com.baomidou.mybatisplus.annotation.TableName;
   import com.fasterxml.jackson.annotation.JsonInclude;
   import lombok.AllArgsConstructor;
   import lombok.Data;
   import lombok.NoArgsConstructor;
   
   import java.io.Serializable;
   import java.util.Date;
   
   
   //菜单表(Menu)实体类
   
   @TableName(value = "sys_menu")
   @Data
   @AllArgsConstructor
   @NoArgsConstructor
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public class Menu implements Serializable {
       private static final long serialVersionUID = -54979041104113736L;
       @TableId
       private Long id;
       /**
        * 菜单名
        */
       private String menuName;
       /**
        * 路由地址
        */
       private String path;
       /**
        * 组件路径
        */
       private String component;
       /**
        * 菜单状态（0显示 1隐藏）
        */
       private String visible;
       /**
        * 菜单状态（0正常 1停用）
        */
       private String status;
       /**
        * 权限标识
        */
       private String perms;
       /**
        * 菜单图标
        */
       private String icon;
       private Long createBy;
       private Date createTime;
       private Long updateBy;
       private Date updateTime;
       /**
        * 是否删除（0未删除 1已删除）
        */
       private Integer delFlag;
       /**
        * 备注
        */
       private String remark;
   }
   ```

3. 定义MenuMapper以及查询方法

   ```java
   import com.baomidou.mybatisplus.core.mapper.BaseMapper;
   import com.example.demo.entity.Menu;
   import org.apache.ibatis.annotations.Mapper;
   
   import java.util.List;
   
   @Mapper
   public interface MenuMapper extends BaseMapper<Menu> {
       List<String> selectPermsByUserId(Long id);
   }
   ```

   创建src/main/resources/mapper/MenuMapper.xml

   ```xml
   <?xml version="1.0" encoding="UTF-8" ?>
   <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
           "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >
   <mapper namespace="com.example.demo.mapper">
       <select id="selectPermsByUserId" resultType="java.lang.String">
           SELECT DISTINCT m.`perms`
           FROM sys_user_role ur
                    LEFT JOIN `sys_role` r ON ur.`role_id` = r.`id`
                    LEFT JOIN `sys_role_menu` rm ON ur.`role_id` = rm.`role_id`
                    LEFT JOIN `sys_menu` m ON m.`id` = rm.`menu_id`
           WHERE user_id = #{userid}
             AND r.`status` = 0
             AND m.`status` = 0
       </select>
   </mapper>
   ```

4. 然后我们可以在UserDetailsServiceImpl中去调用该mapper的方法查询权限信息封装到LoginUser对象中即可

   ```java
   import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
   import com.example.demo.entity.LoginUser;
   import com.example.demo.entity.User;
   import com.example.demo.mapper.MenuMapper;
   import com.example.demo.mapper.UserMapper;
   import jakarta.annotation.Resource;
   import org.springframework.security.core.userdetails.UserDetails;
   import org.springframework.security.core.userdetails.UserDetailsService;
   import org.springframework.security.core.userdetails.UsernameNotFoundException;
   import org.springframework.stereotype.Service;
   
   import java.util.ArrayList;
   import java.util.Arrays;
   import java.util.List;
   import java.util.Objects;
   
   //处理登录请求，前往数据库查询账密
   //在没有写spring security（SecurityConfig）前，此时数据库存储的是铭文密码，并且密码前面加入{noop}，比如'{noop}123'，这是它的规定
   
   /**
    * @author zukedog@163.com
    * @date 2024/8/5 15:10
    */
   @Service
   public class UserDetailsServiceImpl implements UserDetailsService {
       @Resource
       private UserMapper userMapper;
       @Resource
       private MenuMapper menuMapper;
   
       @Override
       public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
           //查询用户信息（认证）
           User user = userMapper.selectOne(new QueryWrapper<User>().eq("user_name", username));
           //如果没有查询到用户，就抛出异常
           if (Objects.isNull(user)) {
               //此异常会被spring security的异常过滤器捕获到
               throw new RuntimeException("用户名或者密码错误");
           }
   
           //查询对应的权限信息（授权）
           //List<String> list = new ArrayList<>(Arrays.asList("test", "admin"));
           List<String> list = menuMapper.selectPermsByUserId(user.getId());
   
           //把数据封装成userDetails返回
           return new LoginUser(user, list);
       }
   }
   ```

5. 测试

   ```java
   @GetMapping("/hi")
   //如果用户用test权限，就让该用户访问
   @PreAuthorize("hasAuthority('system:dept:list')")
   public RespBean hi() {
       return RespBean.success("hi");
   }
   ```

   ![image-20240806150539488](https://imgbed.zukedog.cn/i/2024/08/06/ow6gb4-2.png)

   ```java
   @GetMapping("/hi")
   //如果用户用test权限，就让该用户访问
   @PreAuthorize("hasAuthority('system:test:list')")
   public RespBean hi() {
       return RespBean.success("hi");
   }
   ```

   ![image-20240806150702097](https://imgbed.zukedog.cn/i/2024/08/06/owwjqf-2.png)

   ```java
   @GetMapping("/hi")
   //如果用户用test权限，就让该用户访问
   @PreAuthorize("hasAuthority('xxx')")
   public RespBean hi() {
       return RespBean.success("hi");
   }
   ```

   权限不对，报403了



### 自定义失败处理

​		我们还希望在认证失败或者是授权失败的情况下也能和我们的接口一样返回相同结构的json，这样可以 让前端能对响应进行统一的处理。要实现这个功能我们需要知道SpringSecurity的异常处理机制。 在SpringSecurity中，如果我们在认证或者授权的过程中出现了异常会被ExceptionTranslationFilter捕 获到。在ExceptionTranslationFilter中会去判断是认证失败还是授权失败出现的异常。 如果是认证过程中出现的异常会被封装成AuthenticationException然后调用 AuthenticationEntryPoint对象的方法去进行异常处理。 如果是授权过程中出现的异常会被封装成AccessDeniedException然后调用AccessDeniedHandler对 象的方法去进行异常处理。 所以如果我们需要自定义异常处理，我们只需要自定义AuthenticationEntryPoint和 AccessDeniedHandler然后配置给SpringSecurity即可。

自定义实现类，处理认证失败

```java
import com.alibaba.fastjson.JSON;
import com.example.demo.entity.RespBean;
import com.example.demo.entity.RespBeanEnum;
import com.example.demo.utils.WebUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

//处理认证失败

/**
 * @author zukedog@163.com
 * @date 2024/8/6 16:05
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        //处理异常
        WebUtils.renderString(response, JSON.toJSONString(RespBean.error(RespBeanEnum.AUTHENTICATION_FAILED)));
    }
}
```

自定义实现类，处理授权失败

```java
import com.alibaba.fastjson.JSON;
import com.example.demo.entity.RespBean;
import com.example.demo.entity.RespBeanEnum;
import com.example.demo.utils.WebUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

//处理授权失败

@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        //处理异常
        WebUtils.renderString(response, JSON.toJSONString(RespBean.error(RespBeanEnum.LOGIN_ERROR)));
    }
}
```

将两个实现类配置给SpringSecurity

```java
package com.example.demo.config;

import com.example.demo.filter.JwtAuthenticationTokenFilter;
import com.example.demo.handle.AccessDeniedHandlerImpl;
import com.example.demo.handle.AuthenticationEntryPointImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

//SpringSecurity配置类

/**
 * @author zukedog@163.com
 * @date 2024/8/5 16:05
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Resource
    private AccessDeniedHandlerImpl accessDeniedHandler;
    @Resource
    private AuthenticationEntryPointImpl authenticationEntryPoint;
    @Resource
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        //BCrypt 哈希算法来加密密码
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        // 在这里配置用户认证方式，比如从数据库中加载用户等
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //关闭 CSRF
                .csrf(csrf -> csrf.disable())
                //不通过 Session 获取 SecurityContext
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        //对于登录接口允许匿名访问
                        .requestMatchers("/user/login").permitAll()
                        //其他所有请求需要认证
                        .anyRequest().authenticated())
                //自定义一个过滤器，这个过滤器会去获取请求头中的token
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling -> {
                    exceptionHandling
                            .accessDeniedHandler(accessDeniedHandler)
                            .authenticationEntryPoint(authenticationEntryPoint);
                });

        return http.build();
    }

}
```

自行测试即可看到效果

### 跨域

​		浏览器出于安全的考虑，使用 XMLHttpRequest对象发起 HTTP请求时必须遵守同源策略，否则就是跨 域的HTTP请求，默认情况下是被禁止的。 同源策略要求源相同才能正常进行通信，即协议、域名、端 口号都完全一致。 前后端分离项目，前端项目和后端项目一般都不是同源的，所以肯定会存在跨域请求的问题。 所以我们就要处理一下，让前端能进行跨域请求。

先对SpringBoot配置，运行跨域请求，CorsConfig

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 设置允许跨域的路径
        registry.addMapping("/**")
                // 设置允许跨域请求的域名
                .allowedOriginPatterns("*")
                // 是否允许cookie
                .allowCredentials(true)
                // 设置允许的请求方式
                .allowedMethods("GET", "POST", "DELETE", "PUT")
                // 设置允许的header属性
                .allowedHeaders("*")
                // 跨域允许时间
                .maxAge(3600);
    }
}
```

开启SpringSecurity的跨域访问

```java
import com.example.demo.filter.JwtAuthenticationTokenFilter;
import com.example.demo.handle.AccessDeniedHandlerImpl;
import com.example.demo.handle.AuthenticationEntryPointImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

//SpringSecurity配置类

/**
 * @author zukedog@163.com
 * @date 2024/8/5 16:05
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Resource
    private AccessDeniedHandlerImpl accessDeniedHandler;
    @Resource
    private AuthenticationEntryPointImpl authenticationEntryPoint;
    @Resource
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        //BCrypt 哈希算法来加密密码
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        // 在这里配置用户认证方式，比如从数据库中加载用户等
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //关闭 CSRF
                .csrf(csrf -> csrf.disable())
                //不通过 Session 获取 SecurityContext
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        //对于登录接口允许匿名访问
                        .requestMatchers("/user/login").permitAll()
                        //其他所有请求需要认证
                        .anyRequest().authenticated())
                //自定义一个过滤器，这个过滤器会去获取请求头中的token
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling -> {
                    exceptionHandling
                            .accessDeniedHandler(accessDeniedHandler)
                            .authenticationEntryPoint(authenticationEntryPoint);
                })
                //允许跨域
                .cors(withDefaults());

        return http.build();
    }

}
```



### 其他问题

#### 其他权限校验方法

​		我们前面都是使用@PreAuthorize注解，然后在在其中使用的是hasAuthority方法进行校验。 SpringSecurity还为我们提供了其它方法例如：hasAnyAuthority，hasRole，hasAnyRole等。这里我们先不急着去介绍这些方法，我们先去理解hasAuthority的原理，然后再去学习其他方法你就更 容易理解，而不是死记硬背区别。并且我们也可以选择定义校验方法，实现我们自己的校验逻辑。 hasAuthority方法实际是执行到了SecurityExpressionRoot的hasAuthority，大家只要断点调试既可知 道它内部的校验原理。 它内部其实是调用authentication的getAuthorities方法获取用户的权限列表。然后判断我们存入的方法 参数数据在权限列表中。

hasAnyAuthority方法可以传入多个权限，只有用户有其中任意一个权限都可以访问对应资源。

```java
@GetMapping("/hi")
@PreAuthorize("hasAnyAuthority('system:dept:list','system:test:list')")
public RespBean hi() {
	return RespBean.success("hi");
}

```

hasRole要求有对应的角色才可以访问，但是它内部会把我们传入的参数拼接上 ROLE_ 后再去比较。所 以这种情况下要用用户对应的权限也要有 ROLE_ 这个前缀才可以。

```java
@GetMapping("/hi")
@PreAuthorize("hasRole('system:dept:list')")
public RespBean hi() {
	return RespBean.success("hi");
}
```

hasAnyRole 有任意的角色就可以访问。它内部也会把我们传入的参数拼接上 ROLE_ 后再去比较。所以 这种情况下要用用户对应的权限也要有 ROLE_ 这个前缀才可以。

```java
@GetMapping("/hi")
@PreAuthorize("hasAnyRole('system:test:list','system:dept:list')")
public RespBean hi() {
	return RespBean.success("hi");
}
```

#### 自定义权限校验方法

我们也可以定义自己的权限校验方法，在@PreAuthorize注解中使用我们的方法。

```java
@Component("ex")
public class SGExpressionRoot {
	public boolean hasAuthority(String authority){
        //获取当前用户的权限
        Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        List<String> permissions = loginUser.getPermissions();
        //判断用户权限集合中是否存在authority
        return permissions.contains(authority);
    }
}
```

在SPEL表达式中使用 @ex相当于获取容器中bean的名字未ex的对象。然后再调用这个对象的 hasAuthority方法

```java
@RequestMapping("/hello")
@PreAuthorize("@ex.hasAuthority('system:dept:list')")
public String hello(){
	return "hello";
}
```

#### 基于配置的权限控制

我们也可以在配置类中使用使用配置的方式对资源进行权限控制。

```java
import com.example.demo.filter.JwtAuthenticationTokenFilter;
import com.example.demo.handle.AccessDeniedHandlerImpl;
import com.example.demo.handle.AuthenticationEntryPointImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

//SpringSecurity配置类

/**
 * @author zukedog@163.com
 * @date 2024/8/5 16:05
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Resource
    private AccessDeniedHandlerImpl accessDeniedHandler;
    @Resource
    private AuthenticationEntryPointImpl authenticationEntryPoint;
    @Resource
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        //BCrypt 哈希算法来加密密码
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        // 在这里配置用户认证方式，比如从数据库中加载用户等
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //关闭 CSRF
                .csrf(csrf -> csrf.disable())
                //不通过 Session 获取 SecurityContext
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        //对于登录接口允许匿名访问
                        .requestMatchers("/user/login").permitAll()
                        //定义接口授权
                        .requestMatchers("/hi").hasAuthority("system:test:list")
                        //其他所有请求需要认证
                        .anyRequest().authenticated())
                //自定义一个过滤器，这个过滤器会去获取请求头中的token
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling -> {
                    exceptionHandling
                            .accessDeniedHandler(accessDeniedHandler)
                            .authenticationEntryPoint(authenticationEntryPoint);
                })
                //允许跨域
                .cors(withDefaults());

        return http.build();
    }

}
```

#### CSRF

​		CSRF是指跨站请求伪造（Cross-site request forgery），是web常见的攻击之一。 https://blog.csdn.net/freeking101/article/details/86537087 SpringSecurity去防止CSRF攻击的方式就是通过csrf_token。后端会生成一个csrf_token，前端发起请 求的时候需要携带这个csrf_token,后端会有过滤器进行校验，如果没有携带或者是伪造的就不允许访 问。 我们可以发现CSRF攻击依靠的是cookie中所携带的认证信息。但是在前后端分离的项目中我们的认证信 息其实是token，而token并不是存储中cookie中，并且需要前端代码去把token设置到请求头中才可 以，所以CSRF攻击也就不用担心了。

#### 认证成功处理器

​		实际上在UsernamePasswordAuthenticationFilter进行登录认证的时候，如果登录成功了是会调用 AuthenticationSuccessHandler的方法进行认证成功后的处理的。AuthenticationSuccessHandler就是 登录成功处理器。 我们也可以自己去自定义成功处理器进行成功后的相应处理。

```java
@Component
public class SGSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
        HttpServletResponse response, Authentication authentication) throws IOException,
        ServletException {
        System.out.println("认证成功了");
    }
}
```

然后再去SpringSecurity配置即可

#### 认证失败处理器

```java
@Component
public class SGFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
        HttpServletResponse response, AuthenticationException exception) throws
        IOException, ServletException {
        System.out.println("认证失败了");
    }
}
```

然后再去SpringSecurity配置即可

#### 登出成功处理器

```java
@Component
public class SGLogoutSuccessHandler implements LogoutSuccessHandler {
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse
        response, Authentication authentication) throws IOException, ServletException {
        System.out.println("注销成功");
    }
}
```

然后再去SpringSecurity配置即可



> 此为学习笔记，原作者：B站三更草堂



