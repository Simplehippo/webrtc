package com.example.webrtc.config;

import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义Web配置
 */
@Slf4j
@Configuration
public class WebConfig extends WebMvcConfigurationSupport {

    /**
     * CORS跨域支持
      * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .maxAge(3600)
                .allowCredentials(true);
    }

    /**
     * 静态资源支持
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 第一个方法设置访问路径前缀，第二个方法设置资源路径
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }

    /**
     * 配置拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

    }


    /**
     * fastjson配置
     * @param converters
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        // 序列化配置
        FastJsonConfig config = new FastJsonConfig();
        config.setSerializerFeatures(
//                QuoteFieldNames,                  // 输出key时是否使用双引号
//                WriteNullListAsEmpty,             // 是否输出值为null的字段
//                WriteNullNumberAsZero,            // 数值字段如果为null,输出为0,而非null
//                WriteNullStringAsEmpty,           //字符类型字段如果为null,输出为"",而非null
//                WriteNullBooleanAsFalse,          //Boolean字段如果为null,输出为false,而非null
//                WriteNullStringAsEmpty,           // null String不输出
//                WriteMapNullValue,                //null String也要输出
//                WriteDateUseDateFormat,           //Date的日期转换器
//                DisableCircularReferenceDetect,   //禁止循环引用
//                WriteNullListAsEmpty              //List字段如果为null,输出为[],而非null
        );
        // 处理中文乱码问题
        List<MediaType> fastMediaTypes = new ArrayList<>();
        fastMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        converter.setSupportedMediaTypes(fastMediaTypes);
        // 在convert中添加配置信息
        converter.setFastJsonConfig(config);
        // 将convert添加到converters当中
        converters.add(converter);
    }
}