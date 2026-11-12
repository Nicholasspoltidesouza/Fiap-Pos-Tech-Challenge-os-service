package com.postech.osservice.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI osServiceOpenApi() {
        return new OpenAPI()
                .addServersItem(new Server().url("http://localhost:8081").description("Local"))
                .info(new Info()
                        .title("OS Service API")
                        .description("""
                                Ordem de Serviço + Saga Orchestrator (FIAP Fase 4).

                                ## Fluxo feliz (exemplos)
                                1. POST /api/ordens-servico — cria OS e dispara orcamento.gerar
                                2. Aguarde AGUARDANDO_APROVACAO (Billing gera orçamento)
                                3. POST /api/ordens-servico/{id}/aprovar-orcamento com {"aprovado":true}
                                4. Após pagamento mock, OS vai para EM_EXECUCAO
                                5. Finalize no Execution Service e consulte GET /api/ordens-servico/{id}

                                ## Compensação
                                Use {"aprovado":false} no passo 3 para rejeitar e compensar.

                                Banco próprio: PostgreSQL (os_service_db).
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("FIAP Pós Tech").url("https://github.com/Nicholasspoltidesouza")));
    }
}
