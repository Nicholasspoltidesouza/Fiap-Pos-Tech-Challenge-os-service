package com.postech.osservice.presentation.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.postech.osservice.application.dto.AprovarOrcamentoRequest;
import com.postech.osservice.application.dto.CriarOrdemServicoRequest;
import com.postech.osservice.application.dto.HistoricoStatusResponse;
import com.postech.osservice.application.dto.OrdemServicoResponse;
import com.postech.osservice.application.usecase.OrdemServicoUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ordens-servico")
@Tag(name = "Ordens de Serviço", description = "CRUD and saga-driven lifecycle of service orders")
public class OrdemServicoController {

    private final OrdemServicoUseCase ordemServicoUseCase;

    public OrdemServicoController(OrdemServicoUseCase ordemServicoUseCase) {
        this.ordemServicoUseCase = ordemServicoUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Create service order and start saga",
            description = "Persists OS in PostgreSQL, creates saga_instance and publishes orcamento.gerar",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "FluxoFeliz",
                                    summary = "Abrir OS com valor estimado",
                                    value = """
                                            {
                                              "clienteId": "11111111-1111-1111-1111-111111111111",
                                              "veiculoId": "22222222-2222-2222-2222-222222222222",
                                              "descricaoProblema": "Barulho no motor ao acelerar",
                                              "valorEstimado": 350.00
                                            }
                                            """),
                            @ExampleObject(
                                    name = "SemValorEstimado",
                                    summary = "Billing usa fallback 150.00",
                                    value = """
                                            {
                                              "clienteId": "33333333-3333-3333-3333-333333333333",
                                              "veiculoId": "44444444-4444-4444-4444-444444444444",
                                              "descricaoProblema": "Troca de pastilha de freio"
                                            }
                                            """)
                    })))
    public ResponseEntity<OrdemServicoResponse> criar(@Valid @RequestBody CriarOrdemServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordemServicoUseCase.criar(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service order by id", description = "Reads from PostgreSQL os_service_db.ordem_servico")
    public ResponseEntity<OrdemServicoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ordemServicoUseCase.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "List all service orders")
    public ResponseEntity<List<OrdemServicoResponse>> listar() {
        return ResponseEntity.ok(ordemServicoUseCase.listar());
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Get status history of a service order", description = "Reads ordem_servico_status_historico")
    public ResponseEntity<List<HistoricoStatusResponse>> historico(@PathVariable UUID id) {
        return ResponseEntity.ok(ordemServicoUseCase.historico(id));
    }

    @PostMapping("/{id}/aprovar-orcamento")
    @Operation(
            summary = "Approve or reject quote (continues or compensates saga)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "Aprovar",
                                    summary = "Continua saga → pagamento",
                                    value = "{\"aprovado\": true}"),
                            @ExampleObject(
                                    name = "Rejeitar",
                                    summary = "Compensa saga → FALHA_COMPENSADA",
                                    value = "{\"aprovado\": false}")
                    })))
    public ResponseEntity<OrdemServicoResponse> aprovarOrcamento(
            @PathVariable UUID id,
            @Valid @RequestBody AprovarOrcamentoRequest request) {
        return ResponseEntity.ok(ordemServicoUseCase.aprovarOrcamento(id, request));
    }

    @PostMapping("/{id}/entregar")
    @Operation(summary = "Mark service order as delivered")
    public ResponseEntity<OrdemServicoResponse> entregar(@PathVariable UUID id) {
        return ResponseEntity.ok(ordemServicoUseCase.entregar(id));
    }
}
