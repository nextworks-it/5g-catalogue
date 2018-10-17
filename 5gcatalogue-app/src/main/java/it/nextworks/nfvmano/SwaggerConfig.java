package it.nextworks.nfvmano;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

   @Bean
   public Docket api(){
        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            	.apis(RequestHandlerSelectors.any())
            	.paths(PathSelectors.regex("/nsd/.*"))
            	.build()
            .pathMapping("/")
            .apiInfo(apiInfo());
   }

   @Bean
   public UiConfiguration uiConfig() {
     return UiConfiguration.DEFAULT;
   }
   
   private ApiInfo apiInfo() {
       ApiInfo apiInfo = new ApiInfo(
           "5G App and Service Catalogue",
           "The API of the 5G App and Service Catalogue",
           "1.0",
           "",
           new Contact("Francesca Moscatelli", "http://www.nextworks.it", "f.moscatelli@nextworks.it"),
           "Apache License Version 2.0",
           "http://www.apache.org/licenses/LICENSE-2.0"
       );
       return apiInfo;
   }
  
}
