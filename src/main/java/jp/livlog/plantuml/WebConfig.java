package jp.livlog.plantuml;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jp.livlog.plantuml.servlet.AsciiCoderServlet;
import jp.livlog.plantuml.servlet.AsciiServlet;
import jp.livlog.plantuml.servlet.Base64Servlet;
import jp.livlog.plantuml.servlet.CheckSyntaxServlet;
import jp.livlog.plantuml.servlet.EpsServlet;
import jp.livlog.plantuml.servlet.EpsTextServlet;
import jp.livlog.plantuml.servlet.ImgServlet;
import jp.livlog.plantuml.servlet.LanguageServlet;
import jp.livlog.plantuml.servlet.MapServlet;
import jp.livlog.plantuml.servlet.MetadataServlet;
import jp.livlog.plantuml.servlet.OldProxyServlet;
import jp.livlog.plantuml.servlet.PdfServlet;
import jp.livlog.plantuml.servlet.PlantUmlUIHelperServlet;
import jp.livlog.plantuml.servlet.ProxyServlet;
import jp.livlog.plantuml.servlet.SvgServlet;

@Configuration
public class WebConfig {

    @Bean
    public ServletRegistrationBean <ImgServlet> imgServlet() {

        return new ServletRegistrationBean <>(new ImgServlet(), "/png/*", "/img/*");
    }


    @Bean
    public ServletRegistrationBean <SvgServlet> svgServlet() {

        return new ServletRegistrationBean <>(new SvgServlet(), "/svg/*");
    }


    @Bean
    public ServletRegistrationBean <PdfServlet> pdfServlet() {

        return new ServletRegistrationBean <>(new PdfServlet(), "/pdf/*");
    }


    @Bean
    public ServletRegistrationBean <EpsServlet> epsServlet() {

        return new ServletRegistrationBean <>(new EpsServlet(), "/eps/*");
    }


    @Bean
    public ServletRegistrationBean <EpsTextServlet> epsTextServlet() {

        return new ServletRegistrationBean <>(new EpsTextServlet(), "/epstext/*");
    }


    @Bean
    public ServletRegistrationBean <Base64Servlet> base64Servlet() {

        return new ServletRegistrationBean <>(new Base64Servlet(), "/base64/*");
    }


    @Bean
    public ServletRegistrationBean <AsciiServlet> asciiServlet() {

        return new ServletRegistrationBean <>(new AsciiServlet(), "/txt/*");
    }


    @Bean
    public ServletRegistrationBean <ProxyServlet> proxyServlet() {

        return new ServletRegistrationBean <>(new ProxyServlet(), "/proxy");
    }


    @Bean
    public ServletRegistrationBean <OldProxyServlet> oldProxyServlet() {

        return new ServletRegistrationBean <>(new OldProxyServlet(), "/proxy/*");
    }


    @Bean
    public ServletRegistrationBean <MapServlet> mapServlet() {

        return new ServletRegistrationBean <>(new MapServlet(), "/map/*");
    }


    @Bean
    public ServletRegistrationBean <CheckSyntaxServlet> checkSyntaxServlet() {

        return new ServletRegistrationBean <>(new CheckSyntaxServlet(), "/check/*");
    }


    @Bean
    public ServletRegistrationBean <LanguageServlet> languageServlet() {

        return new ServletRegistrationBean <>(new LanguageServlet(), "/language");
    }


    @Bean
    public ServletRegistrationBean <AsciiCoderServlet> asciiCoderServlet() {

        return new ServletRegistrationBean <>(new AsciiCoderServlet(), "/coder/*");
    }


    @Bean
    public ServletRegistrationBean <PlantUmlUIHelperServlet> plantUmlUIHelperServlet() {

        return new ServletRegistrationBean <>(new PlantUmlUIHelperServlet(), "/ui-helper/*");
    }


    @Bean
    public ServletRegistrationBean <MetadataServlet> metadataServlet() {

        return new ServletRegistrationBean <>(new MetadataServlet(), "/metadata/*");
    }
}
