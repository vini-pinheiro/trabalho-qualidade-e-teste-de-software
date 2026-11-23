# Especificação de Requisitos — Módulo Lanche

## 1. Escopo

O módulo Lanche contempla:

- Representação dos dados de um lanche por meio da entidade `Lanche`.
- Cadastro e consulta de lanches.
- Persistência de informações por meio de `DaoLanche`.
- Associação de ingredientes aos lanches.
- Montagem de lanches personalizados.
- Integração com o fluxo de cadastro de lanches dos clientes.
- Validação das regras de negócio relacionadas à composição de lanches.

## 2. Requisitos Funcionais

### 2.1. Cadastro e manutenção de lanches

| ID | Requisito funcional | Prioridade |
|---|---|---|
| RF-LAN-001 | O sistema deve permitir cadastrar um lanche informando nome, descrição e valor de venda. | Alta |
| RF-LAN-002 | O sistema deve permitir cadastrar um lanche como produto disponibilizado pela administração. | Alta |
| RF-LAN-003 | O sistema deve permitir cadastrar um lanche personalizado associado ao fluxo de criação do cliente. | Alta |
| RF-LAN-004 | O sistema deve armazenar o status de ativação do lanche. | Alta |
| RF-LAN-005 | O sistema deve permitir consultar os lanches cadastrados pelo nome. | Alta |
| RF-LAN-006 | O sistema deve permitir listar os lanches disponíveis para consulta no cardápio. | Alta |
| RF-LAN-007 | O sistema deve retornar uma representação vazia de lanche quando não existir registro correspondente à pesquisa por nome, conforme o contrato atual do DAO. | Média |
| RF-LAN-008 | O sistema deve preservar os dados cadastrados de um lanche durante sua recuperação do banco de dados. | Alta |

### 2.2. Validação dos dados do lanche

| ID | Requisito funcional | Prioridade |
|---|---|---|
| RF-LAN-009 | O sistema deve rejeitar o cadastro de um lanche nulo. | Alta |
| RF-LAN-010 | O sistema deve rejeitar lanches sem nome. | Alta |
| RF-LAN-011 | O sistema deve rejeitar lanches cujo nome seja vazio ou contenha somente espaços em branco. | Alta |
| RF-LAN-012 | O sistema deve rejeitar lanches sem descrição. | Alta |
| RF-LAN-013 | O sistema deve rejeitar descrições vazias ou compostas somente por espaços em branco. | Alta |
| RF-LAN-014 | O sistema deve rejeitar lanches sem valor de venda. | Alta |
| RF-LAN-015 | O sistema deve rejeitar valores de venda negativos. | Alta |
| RF-LAN-016 | O sistema deve rejeitar valores de venda que sejam `NaN` ou infinitos. | Alta |
| RF-LAN-017 | O sistema deve permitir o cadastro de um lanche com valor de venda igual a zero, caso essa regra seja mantida na implementação. | Média |
| RF-LAN-018 | O sistema deve rejeitar pesquisas por nome quando o nome informado for nulo, vazio ou composto somente por espaços em branco. | Alta |

### 2.2. Consulta de lanches

| ID | Requisito funcional | Prioridade |
|---|---|---|
| RF-LAN-019 | O sistema deve recuperar os lanches cadastrados que estejam ativos. | Alta |
| RF-LAN-020 | O sistema deve retornar uma lista vazia quando não houver lanches ativos. | Alta |
| RF-LAN-021 | O sistema deve permitir pesquisar um lanche utilizando seu nome como parâmetro. | Alta |
| RF-LAN-022 | O sistema deve permitir realizar a pesquisa por nome utilizando um objeto `Lanche` ou uma `String`, conforme as assinaturas disponibilizadas pelo DAO. | Média |
| RF-LAN-023 | O sistema deve retornar os dados do lanche encontrado, incluindo identificador, nome, descrição, valor de venda e status. | Alta |

### 2.3. Associação de ingredientes

| ID | Requisito funcional | Prioridade |
|---|---|---|
| RF-LAN-024 | O sistema deve permitir associar um ingrediente cadastrado a um lanche. | Alta |
| RF-LAN-025 | O sistema deve armazenar a quantidade do ingrediente utilizada no lanche. | Alta |
| RF-LAN-026 | O sistema deve rejeitar a associação quando o lanche for nulo. | Alta |
| RF-LAN-027 | O sistema deve rejeitar a associação quando o ingrediente for nulo. | Alta |
| RF-LAN-028 | O sistema deve rejeitar a associação quando o identificador do lanche for inválido. | Alta |
| RF-LAN-029 | O sistema deve rejeitar a associação quando o identificador do ingrediente for inválido. | Alta |
| RF-LAN-030 | O sistema deve rejeitar a associação quando a quantidade do ingrediente for igual ou inferior a zero. | Alta |

### 2.5. Montagem de lanches personalizados

| ID | Requisito funcional | Prioridade |
|---|---|---|
| RF-LAN-031 | O sistema deve permitir montar um lanche personalizado a partir de ingredientes previamente cadastrados. | Alta |
| RF-LAN-032 | O sistema deve permitir selecionar mais de um ingrediente para compor um mesmo lanche. | Alta |
| RF-LAN-033 | O sistema deve permitir informar a quantidade de cada ingrediente selecionado. | Alta |
| RF-LAN-034 | O sistema deve validar os ingredientes selecionados antes de concluir a montagem do lanche. | Alta |
| RF-LAN-035 | O sistema deve impedir a montagem quando houver ingredientes inválidos ou inexistentes. | Alta |
| RF-LAN-036 | O sistema deve calcular o valor de venda do lanche personalizado com base nos ingredientes utilizados, caso essa seja a regra de precificação adotada pelo projeto. | Alta |
| RF-LAN-037 | O sistema deve preservar a associação entre o lanche montado e seus ingredientes. | Alta |
| RF-LAN-038 | O sistema deve impedir a conclusão de uma montagem que não atenda às regras mínimas de composição definidas pelo negócio. | Alta |
| RF-LAN-039 | O sistema deve disponibilizar os dados do lanche montado para utilização no fluxo de pedido do cliente. | Alta |

## 3. Requisitos Não Funcionais

### 3.1. Confiabilidade e integridade

| ID | Requisito não funcional | Critério de verificação |
|---|---|---|
| RNF-LAN-001 | O módulo deve garantir que operações de persistência não sejam consideradas bem-sucedidas quando ocorrer uma falha no banco de dados. | Simular `SQLException` e verificar o tratamento da falha. |
| RNF-LAN-002 | O módulo deve preservar a integridade dos dados de lanches e ingredientes associados. | Verificar se os dados enviados para persistência correspondem aos dados recebidos. |
| RNF-LAN-003 | O módulo deve evitar associações de ingredientes com identificadores inválidos. | Testar identificadores nulos, zero e negativos. |
| RNF-LAN-004 | O módulo deve liberar os recursos JDBC utilizados, mesmo quando ocorrer uma exceção durante a operação. | Verificar fechamento de `Connection`, `PreparedStatement` e `ResultSet`, conforme a responsabilidade de cada objeto. |
| RNF-LAN-005 | O módulo deve manter o comportamento consistente entre consultas sem resultados e consultas com resultados. | Testar lista vazia, registro inexistente e registros encontrados. |

### 3.2. Segurança

| ID | Requisito não funcional | Critério de verificação |
|---|---|---|
| RNF-LAN-006 | O módulo deve utilizar consultas parametrizadas para evitar injeção de SQL. | Informar strings contendo comandos SQL e verificar que sejam tratadas como valores. |
| RNF-LAN-007 | O módulo não deve permitir que dados inválidos sejam persistidos por ausência de validação. | Testar entradas inválidas nos métodos de cadastro e associação. |
| RNF-LAN-008 | O módulo deve evitar exposição desnecessária de informações internas do banco de dados ao consumidor da aplicação. | Verificar o tratamento e a propagação das exceções nas camadas superiores. |

### 3.2. Manutenibilidade e arquitetura

| ID | Requisito não funcional | Critério de verificação |
|---|---|---|
| RNF-LAN-009 | As regras de negócio da montagem de lanches devem estar separadas das operações de persistência. | Verificar se `MontagemLancheService` não concentra operações JDBC diretamente. |
| RNF-LAN-010 | As classes devem apresentar responsabilidades bem definidas. | Avaliar se o DAO cuida de persistência e o serviço cuida das regras de negócio. |
| RNF-LAN-011 | O módulo deve permitir a substituição de dependências externas durante os testes. | Verificar se é possível fornecer dependências simuladas, como `Connection` ou DAO. |
| RNF-LAN-012 | As regras de validação devem ser consistentes entre os diferentes fluxos de cadastro e montagem. | Testar as mesmas condições de entrada em operações que compartilham regras. |
| RNF-LAN-013 | As operações devem apresentar tratamento de erros consistente e compreensível. | Verificar tipos de exceção e mensagens para situações inválidas e falhas de infraestrutura. |

### 3.3. Desempenho e eficiência

| ID | Requisito não funcional | Critério de verificação |
|---|---|---|
| RNF-LAN-014 | As consultas ao banco devem utilizar parâmetros para evitar a concatenação de valores diretamente no SQL. | Inspecionar a execução das consultas e os parâmetros enviados. |
| RNF-LAN-015 | O módulo deve evitar consultas desnecessárias ao banco durante a montagem de um lanche. | Avaliar a quantidade de chamadas ao DAO em cenários de montagem. |
| RNF-LAN-016 | O módulo deve evitar processamento redundante na validação e composição de ingredientes. | Avaliar a implementação e testar entradas com diferentes quantidades de ingredientes. |

### 3.5. Testabilidade

| ID | Requisito não funcional | Critério de verificação |
|---|---|---|
| RNF-LAN-017 | As classes devem permitir testes unitários sem exigir conexão com um banco de dados real. | Executar os testes utilizando mocks ou stubs. |
| RNF-LAN-018 | As regras de negócio devem permitir testes isolados de suas dependências externas. | Testar o serviço utilizando um DAO simulado. |
| RNF-LAN-019 | As classes de maior complexidade devem possuir cobertura estrutural de pelo menos 80% pelo critério todas as arestas, conforme os requisitos do trabalho. | Gerar relatório de cobertura e verificar a métrica. |
| RNF-LAN-020 | As classes de maior complexidade devem alcançar pelo menos 80% de escore de mutação, conforme os requisitos do trabalho. | Executar testes de mutação e analisar os mutantes sobreviventes. |


## 4. Matriz de Rastreabilidade

A matriz abaixo relaciona cada requisito aos arquivos que o implementam e aos
arquivos que efetivamente o validam. `Parcial` indica que existe cobertura para
uma parte do fluxo ou que o requisito é atendido pelo serviço, mas não pelo
servlet legado. `Não atendido` indica que há um teste de caracterização ou uma
evidência de que o comportamento atual viola o requisito.

### 4.1. Requisitos funcionais

| Requisito | Implementação | Validação | Situação |
|---|---|---|---|
| RF-LAN-001 a RF-LAN-002 | `src/java/DAO/DaoLanche.java` (`salvar`) | `src/test/java/DAO/DaoLancheTest.java` (`deveSalvarLancheAtivo`, `deveSalvarLancheClienteERetornarId`) | Validado |
| RF-LAN-003 | `src/java/DAO/DaoLanche.java` (`salvarCliente`), `src/java/Controllers/salvarLancheCliente.java` | `src/test/java/DAO/DaoLancheTest.java`, `src/test/java/Controllers/SalvarLancheClienteTest.java` | Parcial: o servlet não usa `MontagemLancheService` |
| RF-LAN-004 | `src/java/Model/Lanche.java`, `src/java/DAO/DaoLanche.java` | `src/test/java/DAO/DaoLancheTest.java` (`deveSalvarLancheAtivo`, `deveSalvarLancheClienteERetornarId`, `deveListarTodosOsLanchesAtivos`) | Validado |
| RF-LAN-005, RF-LAN-007 a RF-LAN-008, RF-LAN-021 a RF-LAN-023 | `src/java/DAO/DaoLanche.java` (`pesquisaPorNome`, `criarLanche`) | `src/test/java/DAO/DaoLancheTest.java` (`deveEncontrarLanchePeloNome`, `deveRetornarLancheVazioQuandoNaoEncontrarNome`, `deveEncontrarLancheUtilizandoObjeto`) | Validado |
| RF-LAN-006, RF-LAN-019 a RF-LAN-020 | `src/java/DAO/DaoLanche.java` (`listarTodos`) | `src/test/java/DAO/DaoLancheTest.java` (`deveListarTodosOsLanchesAtivos`, `deveRetornarListaVaziaQuandoNaoExistiremLanches`) | Validado |
| RF-LAN-009 a RF-LAN-018 | `src/java/DAO/DaoLanche.java` (`validarLanche`, `validarNome`) | `src/test/java/DAO/DaoLancheTest.java` (testes de dados nulos, vazios, em branco, `NaN`, infinito, negativo e zero) | Validado |
| RF-LAN-024 a RF-LAN-030 | `src/java/DAO/DaoLanche.java` (`vincularIngrediente`) | `src/test/java/DAO/DaoLancheTest.java` (`deveVincularIngredienteComSucesso` e testes de identificadores, nulos e quantidade) | Validado |
| RF-LAN-031 a RF-LAN-034, RF-LAN-036, RF-LAN-038 | `src/java/Service/MontagemLancheService.java` | `src/test/java/Service/MontagemLancheServiceTest.java` (montagem, validação, cálculo, composição vazia e limite de valor) | Validado no serviço |
| RF-LAN-035 | `src/java/Service/MontagemLancheService.java` (`validarIngredientesSelecionados`) | `src/test/java/Service/MontagemLancheServiceTest.java` (`deveRejeitarIdentificadorInvalido`, `deveRejeitarIngredienteInativo`) | Validado no serviço; o servlet legado tem tratamento diferente |
| RF-LAN-037 | `src/java/Service/MontagemLancheService.java` | `src/test/java/Service/MontagemLancheServiceTest.java` (`deveMontarComSucesso`) e `src/test/java/Controllers/SalvarLancheClienteTest.java` (`deveVincularCadaIngredienteAoLanche`) | Parcial: o serviço vincula, mas o teste do servlet caracteriza que nenhum ingrediente é vinculado |
| RF-LAN-039 | `src/java/Service/MontagemLancheService.java`, `src/java/Controllers/salvarLancheCliente.java` | `src/test/java/Service/MontagemLancheServiceTest.java` (`deveMontarComSucesso`), `src/test/java/Controllers/SalvarLancheClienteTest.java` (`deveResponderComLinkDoCarrinho`) | Parcial |

`src/java/Model/Ingrediente.java` e `src/java/DAO/DaoIngrediente.java` fornecem
os dados de entrada, mas não são os arquivos que validam RF-LAN-024 a
RF-LAN-035; por isso não aparecem como responsáveis por esses requisitos.

### 4.2. Requisitos não funcionais

| Requisito | Implementação / evidência | Arquivo de validação | Situação |
|---|---|---|---|
| RNF-LAN-001 a RNF-LAN-005 | `src/java/DAO/DaoLanche.java` (exceções, `try-with-resources`, consultas com e sem resultado) | `src/test/java/DAO/DaoLancheTest.java` (falhas de banco, fechamento de recursos, lista vazia e registro inexistente) | Validado |
| RNF-LAN-006, RNF-LAN-014 | `src/java/DAO/DaoLanche.java` (`?` e `PreparedStatement`) | `src/test/java/DAO/DaoLancheTest.java` (`deveUtilizarParametroParaPesquisarNome`) | Validado |
| RNF-LAN-007 | `src/java/DAO/DaoLanche.java`, `src/java/Service/MontagemLancheService.java` | `src/test/java/DAO/DaoLancheTest.java`, `src/test/java/Service/MontagemLancheServiceTest.java` | Validado |
| RNF-LAN-008 | `src/java/DAO/DaoLanche.java`, `src/java/Controllers/salvarLancheCliente.java` | Não há teste específico de exposição de erro | Não validado |
| RNF-LAN-009 a RNF-LAN-010 | `src/java/Service/MontagemLancheService.java`, `src/java/DAO/DaoLanche.java` | Inspeção dos arquivos de implementação; `src/test/java/Service/MontagemLancheServiceTest.java` valida a separação por injeção do DAO | Parcial |
| RNF-LAN-011, RNF-LAN-017 a RNF-LAN-018 | Construtores com dependência injetável em `src/java/DAO/DaoLanche.java` e `src/java/Service/MontagemLancheService.java` | `src/test/java/DAO/DaoLancheTest.java`, `src/test/java/Service/MontagemLancheServiceTest.java` | Validado |
| RNF-LAN-012 a RNF-LAN-013 | `src/java/DAO/DaoLanche.java`, `src/java/Service/MontagemLancheService.java` | Testes de validação e mensagens em `src/test/java/DAO/DaoLancheTest.java` e `src/test/java/Service/MontagemLancheServiceTest.java` | Parcial |
| RNF-LAN-015 a RNF-LAN-016 | `src/java/Service/MontagemLancheService.java` (`calcularValorComposicao`, validação em uma passagem) | `src/test/java/Service/MontagemLancheServiceTest.java` | Parcial: não há teste que meça chamadas redundantes ao DAO |
| RNF-LAN-019 | Configuração JaCoCo em `pom.xml` | `docs/ai/vinicius-fonseca/AI-LOG.md` (registro do resultado de cobertura) | Evidência documental; relatório reproduzível não está em `docs/entrega-2` |
| RNF-LAN-020 | Configuração PIT em `pom.xml` | `docs/ai/vinicius-fonseca/AI-LOG.md` (registro do resultado de mutação) | Evidência documental; relatório reproduzível não está em `docs/entrega-2` |

### 4.3. Pendências identificadas

| Pendência | Evidência |
|---|---|
| O servlet `salvarLancheCliente` reutiliza um `Iterator` já consumido e não vincula os ingredientes. | `src/test/java/Controllers/SalvarLancheClienteTest.java` (`deveVincularCadaIngredienteAoLanche`, marcado `defeito-conhecido`, e `caracterizacaoNenhumIngredienteEhVinculado`) |
| O servlet aceita composição vazia e quantidade negativa, contrariando as regras do serviço. | `src/test/java/Controllers/SalvarLancheClienteTest.java` (`caracterizacaoComposicaoVaziaGeraLancheDePrecoZero`, `caracterizacaoQuantidadeNegativaReduzPreco`) |
| A matriz anterior atribuía RF-LAN-024 a RF-LAN-035 a `Ingrediente/DaoIngrediente`, mas a validação está em `DaoLancheTest` e `MontagemLancheServiceTest`. | Arquivos de teste listados na seção 4.1 |
| RNF-LAN-008, RNF-LAN-015, RNF-LAN-019 e RNF-LAN-020 não possuem, nesta entrega, teste ou relatório verificável diretamente no diretório de evidências. | Seção 4.2 |