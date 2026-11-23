# Registro de Uso de Inteligência Artificial (AI-LOG)

**Aluno:** Thiago Ferreira
**Entregas:** Etapa 1 — Testes Unitários Automatizados; Etapa 2 — Técnicas de Teste, Integração, Sistema e Inspeção
**Módulo:** Gestão de Clientes e Endereços (`DAO.DaoCliente`, `Controllers.cadastro`, `Helpers.EncryptadorMD5`)

---

## Entrega 1

| Responsável | Atividade | Ferramenta | Prompt / Instrução Utilizada | Resultado da IA | Decisão (Aceito / Alterado / Rejeitado) | Validação |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Thiago Ferreira | Criação de `DaoClienteTest` (commit `32c5b22`, 21/09): login com sucesso, senha incorreta e consultas por usuário e por ID | _preencher_ | _preencher_ | _preencher_ | _preencher_ | _preencher_ |
| Thiago Ferreira | Uso de Objenesis e reflexão para injetar uma conexão falsa no campo privado do DAO | _preencher_ | _preencher_ | _preencher_ | _preencher_ | _preencher_ |
| Thiago Ferreira | Execução do caso de teste manual CT-03 (21/09): 2 passos PASSOU, 5 FALHOU | Sem uso de IA | — | — | — | Execução na aplicação em Docker, com prints anexados ao documento do caso. |

## Entrega 2

Usei a IA como apoio nas tarefas que exigiam muito trabalho repetitivo: gerar casos de teste, rodar as ferramentas
(JaCoCo, PIT, SonarQube, Docker) e montar tabelas e evidências. As decisões de escopo, o que aceitar ou manter e a
conferência dos resultados foram minhas. Cada linha abaixo separa o que a IA fez do que eu fiz.

| Responsável | Atividade | Ferramenta | Prompt / Instrução Utilizada | Resultado da IA | Decisão (Aceito / Alterado / Rejeitado) | Validação |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Thiago Ferreira | Melhorar e ampliar testes unitários | Claude (Claude Code) | "Faça mais casos de teste unitários, atentando-se a isolar as dependências, nas classes `DaoCliente`, `cadastro` e `EncryptadorMD5`." | Propôs um construtor que recebe o `EncryptadorMD5` (para trocá-lo por mock no `login()`) e gerou 45 métodos de teste novos, incluindo 3 testes de defeito conhecido (usuário duplicado, acentos e DEF-04 em `pesquisaPorUsuario`). Registrou tudo em `HISTORICO-ALTERACOES.md`. | **Alterado**: na primeira versão, 2 testes falhavam por *strict stubbing* do Mockito e os testes de campo ausente apagavam mais do que o campo testado; foram corrigidos antes de eu aceitar. Eu apliquei as mudanças à mão na develop. | Rodei a suíte no IntelliJ: o erro de construtor que apareceu era meu (faltava aplicar a mudança no `DaoCliente`). Depois de corrigir, os testes passaram. Os 4 que ficaram vermelhos no IntelliJ eram exatamente os testes `defeito-conhecido`, que devem falhar até os defeitos serem corrigidos. |
| Thiago Ferreira | Partição de equivalência, tabela de decisão e valor limite | Claude (Claude Code) | "Monte as tabelas de partição de equivalência, tabela de decisão e valor limite do cadastro e do login, indicando o teste que implementa cada caso." | Montou as três tabelas a partir da história U4 (cadastro) e das regras do login, ligando cada classe, regra ou valor limite ao teste que o implementa, e apontou 2 classes ainda sem teste. | **Aceito** | A IA conferiu que cada teste citado existe na develop. Os casos marcados como defeito são os mesmos que falharam no meu teste manual CT-03. |
| Thiago Ferreira | Teste de mutação | Claude (Claude Code) | "Rode o PIT nas minhas classes e explique os mutantes que sobreviveram." | Explicou que o PIT só muta as classes indicadas, testou que a lista com vírgula não funciona e analisou os mutantes sobreviventes, justificando os equivalentes. | **Aceito** | Rodei o PIT do `DaoCliente` no IntelliJ: 54 de 59 (92%). Depois das correções da tarefa 9, a IA rodou de novo no Docker oficial: 48 de 49 (98%). O único sobrevivente remove um `setUsuario` cujo valor nunca é lido. |
| Thiago Ferreira | Teste de integração | Claude (Claude Code) | "Crie um teste de integração do cadastro, com o servlet, os DAOs e o banco real." | Criou o `CadastroIT` com 4 casos (1 de defeito conhecido). | **Aceito** | Eu rodei o teste no Docker: 3 de 3 passaram. |
| Thiago Ferreira | Teste manual automatizado com Selenium | Claude (Claude Code) | "Automatize o caso de teste manual CT-03 com Selenium." | Criou o Page Object `CadastroPage` e o `CadastroE2E` com 4 cenários ligados aos passos do CT-03 e executou os testes no Docker (aplicação, Selenium e banco). | **Aceito** | O resultado bate com o meu CT-03 manual: os passos que passaram à mão passam no Selenium, e os que falharam falham pelo mesmo motivo (prints em `docs/entrega-2/selenium/thiago-ferreira/`). |
| Thiago Ferreira | Teste de inspeção | Claude (Claude Code) | "Baseado no feedback gerado pelo SonarQube, faça correções nas classes `DaoCliente`, `cadastro` e `EncryptadorMD5`. OBS: não faça mudanças de nomes de classes." | Analisou os 33 apontamentos do SonarQube nas 3 classes, propôs as correções e, com o meu aval, aplicou-as. Criou um teste novo para o tratamento de erro do `doPost`. | **Alterado**: revisei a lista de correções antes de autorizar e decidi manter 8 apontamentos (nomes de pacote e de classe usados pelo grupo, o DEF-04 com teste vermelho de propósito, o MD5 das senhas do banco e as `RuntimeException`, para não criar uma classe nova). | Análise antes e depois no próprio SonarQube: 33 → 8 apontamentos, 0 bugs e 0 vulnerabilidades nas minhas classes. Depois das correções: 183 testes passando e os 8 testes de defeito ainda vermelhos. |

---

## Observações

- A documentação completa da minha parte (tabelas, grafos, prints e justificativas de cada tarefa) está no
  documento da Entrega 2 e em [`docs/entrega-2/thiago-ferreira/`](../../entrega-2/thiago-ferreira/).
- A abordagem com Objenesis e reflexão da Entrega 1 foi substituída por injeção de `Connection` e `DaoEndereco` via
  construtor. Essa alteração foi feita com apoio de ferramenta de IA operada por Felipe Paixão e está registrada em
  [`docs/ai/felipe-paixao/AI-LOG.md`](../felipe-paixao/AI-LOG.md).
- Achados desta área confirmados no teste manual CT-03 e agora automatizados: o cadastro aceita campos obrigatórios
  vazios (DEF-05), aceita dois clientes com o mesmo usuário e grava nomes com acento corrompidos.
- Limitação encontrada: o teste de defeito dos acentos passa por acaso quando o Maven roda no Windows sem UTF-8. No
  Docker oficial e no IntelliJ, que usam UTF-8, ele falha como deve.