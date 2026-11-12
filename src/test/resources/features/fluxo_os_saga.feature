Feature: Fluxo completo da saga de Ordem de Serviço
  Como orquestrador da oficina
  Quero conduzir a OS do recebimento até a finalização
  Para coordenar orçamento, pagamento e execução de forma consistente

  Scenario: Happy path da saga orquestrada
    Given uma nova ordem de servico para o cliente
    When a saga e iniciada
    Then o status da OS deve ser "AGUARDANDO_ORCAMENTO"
    And o evento de geracao de orcamento deve ter sido publicado
    When o orcamento e gerado com valor 450.00
    Then o status da OS deve ser "AGUARDANDO_APROVACAO"
    When o cliente aprova o orcamento
    Then o status da OS deve ser "AGUARDANDO_PAGAMENTO"
    And o evento de solicitacao de pagamento deve ter sido publicado
    When o pagamento e aprovado
    Then o status da OS deve ser "EM_EXECUCAO"
    And o evento de inicio de execucao deve ter sido publicado
    When a execucao e finalizada
    Then o status da OS deve ser "FINALIZADA"
    And a saga deve estar COMPLETED

  Scenario: Compensacao quando o pagamento falha
    Given uma nova ordem de servico para o cliente
    When a saga e iniciada
    And o orcamento e gerado com valor 200.00
    And o cliente aprova o orcamento
    When o pagamento falha com motivo "cartao recusado"
    Then o status da OS deve ser "FALHA_COMPENSADA"
    And eventos de compensacao devem ter sido publicados
