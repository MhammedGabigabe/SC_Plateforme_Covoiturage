package ma.youcode.covoiturage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Plateforme de covoiturage multi-étapes")
                        .version("1.0")
                        .description("""
                                API REST permettant aux conducteurs de publier des trajets avec plusieurs étapes \
                                et aux passagers de réserver des places sur tout ou partie du parcours.

                                Identification simulée : les conducteurs envoient l'en-tête X-Conducteur-Id \
                                et les passagers l'en-tête X-Passager-Id.

                                Les erreurs sont renvoyées au format ProblemDetail \
                                (timestamp, status, title, detail, instance)."""));
    }
}