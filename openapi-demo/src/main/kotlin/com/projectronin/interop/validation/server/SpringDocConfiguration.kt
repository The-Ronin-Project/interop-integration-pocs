package com.projectronin.interop.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.annotation.Generated

// This gets generated using Jakarta, but we haven't moved to the Jakarta versions yet, so it's been copied here.
@Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"])
@Configuration
class SpringDocConfiguration {
    @Bean
    fun apiInfo(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Test API")
                    .description("Just a basic API to test some stuff")
                    .version("0.0.1")
            )
    }
}
