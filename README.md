# OS Service — Ordem de Serviço (Fase 4)

Microserviço Spring Boot responsável por **Ordens de Serviço** da oficina mecânica e pela **orquestração da Saga** entre orçamento, pagamento e execução.

## Arquitetura

```
Cliente/API ──► OS Service (orquestrador) ──► RabbitMQ (oficina.events)
                       │                              │
                       ▼                              ▼
                 PostgreSQL (próprio)     orcamento / pagamento / execucao
```

- **Banco próprio**: `os_service_db` (sem acesso ao DB de outros serviços).
- **Mensageria**: exchange topic `oficina.events`.
- **Padrão Saga**: **ORCHESTRATED** (este serviço decide o próximo passo e a compensação).

### Por que Saga Orquestrada?

Na Fase 4 a OS é o **agregado pivô** do fluxo de negócio. Centralizar a máquina de estados e a compensação no OS Service:

1. Evita acoplamento ciclico entre serviços participantes (orçamento, pagamento, execução).
2. Facilita auditoria (`saga_instance` + histórico de status).
3. Torna explícito o caminho feliz e o caminho de falha (compensação com eventos `compensar.*`).
4. Escala melhor para teaching/demo do Tech Challenge do que choreography pura, onde a lógica ficaria espalhada.

## Fluxo da Saga

| Passo | Trigger | Status OS | Evento publicado |
|-------|---------|-----------|------------------|
| 1 | `POST /api/ordens-servico` | `AGUARDANDO_ORCAMENTO` | `ordem.criada`, `orcamento.gerar` |
| 2 | `OrcamentoGerado` | `AGUARDANDO_APROVACAO` | — |
| 3 | `POST .../aprovar-orcamento` (true) | `AGUARDANDO_PAGAMENTO` | `pagamento.solicitar` |
| 4 | `PagamentoAprovado` | `EM_EXECUCAO` | `execucao.iniciar` |
| 5 | `ExecucaoFinalizada` | `FINALIZADA` | — |
| 6 | `POST .../entregar` | `ENTREGUE` | — |
| Falha | rejeição / `PagamentoFalhou` | `FALHA_COMPENSADA` | `compensar.*` |

Status suportados: `RECEBIDA`, `AGUARDANDO_ORCAMENTO`, `AGUARDANDO_APROVACAO`, `AGUARDANDO_PAGAMENTO`, `EM_EXECUCAO`, `FINALIZADA`, `ENTREGUE`, `CANCELADA`, `FALHA_COMPENSADA`.

## Stack

- Java 25, Spring Boot 4.0.3, Maven
- Spring Web, Validation, Data JPA, AMQP (RabbitMQ), Security (demo), Actuator, Micrometer Prometheus
- springdoc-openapi, JaCoCo (meta 80% em domain/saga/usecase), JUnit 5, Mockito, Cucumber BDD
- PostgreSQL, Docker, Kubernetes, GitHub Actions

## Segurança (demo)

Por padrão `security.permit-all=true` (todas as rotas liberadas para facilitar a demo da Fase 4).

Para exigir autenticação HTTP Basic: `SECURITY_PERMIT_ALL=false`. Em produção, integrar JWT do Auth Service / API Gateway.

## Como rodar

### 1) Infra local (Postgres + RabbitMQ)

```bash
docker compose up -d postgres rabbitmq
```

### 2) Aplicação

```bash
# copie .env.example se desejar
mvn spring-boot:run
```

- API: http://localhost:8081  
- Swagger: http://localhost:8081/swagger-ui.html  
- Health: http://localhost:8081/actuator/health  
- Prometheus: http://localhost:8081/actuator/prometheus  
- RabbitMQ UI: http://localhost:15672 (guest/guest)

### 3) App via Docker Compose

```bash
docker compose --profile app up --build
```

### 4) Kubernetes

```bash
docker build -t os-service:latest .
kubectl apply -f k8s/
```

## API principal

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/api/ordens-servico` | Cria OS e inicia saga |
| GET | `/api/ordens-servico` | Lista OS |
| GET | `/api/ordens-servico/{id}` | Detalhe |
| GET | `/api/ordens-servico/{id}/historico` | Histórico de status |
| POST | `/api/ordens-servico/{id}/aprovar-orcamento` | Aprova/rejeita orçamento |
| POST | `/api/ordens-servico/{id}/entregar` | Marca entrega |

Coleção Postman: `postman/os-service.postman_collection.json`.

## Contratos RabbitMQ

- Exchange: `oficina.events` (topic)
- Consome: `os.orcamento.gerado`, `os.orcamento.aprovado`, `os.orcamento.rejeitado`, `os.pagamento.aprovado`, `os.pagamento.falhou`, `os.execucao.iniciada`, `os.execucao.finalizada`
- Publica: `ordem.criada`, `orcamento.gerar`, `pagamento.solicitar`, `execucao.iniciar`, `compensar.orcamento`, `compensar.pagamento`, `compensar.execucao`

Envelope JSON:

```json
{
  "eventId": "uuid",
  "eventType": "OrdemServicoCriada",
  "ocorridoEm": "2026-11-12T11:00:00Z",
  "ordemServicoId": "uuid",
  "clienteId": "uuid",
  "payload": {}
}
```

## Persistência

- `ordem_servico`
- `ordem_servico_status_historico`
- `saga_instance` (`RUNNING|COMPLETED|COMPENSATING|FAILED|COMPENSATED`)

## Testes e cobertura

```bash
mvn -B verify
```

- Unitários: domínio (`OrdemServico`, `SagaState`), `SagaOrchestrator`, `OrdemServicoUseCase`
- BDD Cucumber: `src/test/resources/features/fluxo_os_saga.feature` (happy path + compensação)
- JaCoCo: gate de **80%** de linhas nos pacotes `domain.model`, `application.saga`, `application.usecase`

### Evidência de cobertura

Após `mvn verify`, abra:

- `target/site/jacoco/index.html`
- Relatório XML: `target/site/jacoco/jacoco.xml` (usado pelo Sonar)

O pipeline CI/CD sobe o artefato `jacoco-report` em todo push/PR.

## Estrutura de pacotes

```
com.postech.osservice
  domain/          # OrdemServico, Status, SagaState, exceptions
  application/     # usecases, DTOs, ports, SagaOrchestrator
  infrastructure/  # JPA, RabbitMQ, Security, OpenAPI
  presentation/    # Controllers + ExceptionHandler
```

## Observações

- Mercado Pago **não** faz parte deste serviço (fica no serviço de pagamento).
- Este serviço **não** acessa bancos de outros microserviços.
