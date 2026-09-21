# Registro de Uso de Inteligência Artificial (AI-LOG)

**Aluno:** Felipe Paixão
**Entrega:** Etapa 1 — ampliação da suíte de testes, documentação e apresentação
**Módulo:** Gestão de Estoque e Insumos (`DAO.DaoIngrediente`, servlets de insumo), além do apoio à consolidação da entrega

---

| Responsável | Atividade | Ferramenta | Prompt / Instrução Utilizada | Resultado da IA | Decisão | Validação |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Felipe Paixão | Auditoria das classes escolhidas pelo grupo frente ao requisito "classe não CRUD" | Claude Code (Anthropic) | Pedi uma análise honesta de quais classes realmente atendiam ao requisito, com instrução explícita de não inventar complexidade para fechar cinco nomes. | Apontou que `DaoIngrediente` e `DaoLanche` são CRUD e que o sistema não possui regra de estoque (a compra não dá baixa). Sugeriu usar os servlets de insumo como melhor alternativa e registrar a lacuna. | **Aceito**, com a lacuna declarada abertamente no relatório e no slide 10 | Conferência manual dos métodos das duas classes; a limitação está escrita na entrega em vez de mascarada. |
| Felipe Paixão | Testes unitários de `DAO.DaoIngrediente` (issue #5) e dos servlets `salvarIngrediente` / `alterarIngrediente` | Claude Code (Anthropic) | Pedi cobertura de valores de compra/venda, quantidades zero e negativas, item inexistente e falha de persistência, distinguindo regra existente de melhoria proposta. | Gerou `DaoIngredienteTest` e `IngredienteControllersTest`, com um caso feliz e um de falha por operação, além dos casos de autorização do painel. | **Aceito** | `mvn clean test` executado em container Maven 3.8.4 / OpenJDK 8: 115 testes, 0 falhas. Cobertura conferida no relatório do JaCoCo. |
| Felipe Paixão | Construtores de injeção nos DAOs e servlets para permitir mocks | Claude Code (Anthropic) | Perguntei como isolar o banco sem alterar o comportamento da aplicação no Tomcat. | Adicionou construtores que recebem `Connection` ou os DAOs, mantendo o construtor padrão intacto. Em `DaoCliente`, também tornou substituível o `DaoEndereco` criado internamente. | **Aceito** | Revisei o diff linha a linha: nenhuma regra de negócio foi alterada. Build do WAR e suíte completa executados com sucesso. |
| Felipe Paixão | Separação dos testes que expõem defeitos abertos | Claude Code (Anthropic) | Perguntei o que fazer com testes que falham porque o sistema está errado, já que o `Dockerfile` roda `mvn package`. | Propôs `@Tag("defeito-conhecido")` com um perfil Maven separado, em vez de `@Disabled`. | **Aceito** | `mvn test -Pdefeitos` mantém os 5 testes visivelmente vermelhos, um por defeito, sem quebrar o build padrão. |
| Felipe Paixão | Infraestrutura de testes de integração com PostgreSQL em container | Claude Code (Anthropic) | A IA havia montado Failsafe, um `docker-compose` de teste e 37 testes de integração. | Estrutura funcional, porém pesada para o escopo da Etapa 1. | **Rejeitado pelo grupo** | Decidimos remover tudo e manter apenas testes unitários, conforme o escopo da entrega. Os arquivos foram excluídos antes do commit. |
| Felipe Paixão | Regra de negócio sugerida para valores de insumo | Claude Code (Anthropic) | Discuti se valeria testar que o preço de venda deve superar o de compra. | A própria ferramenta alertou que nada no domínio sustenta essa regra e recomendou não criar o teste. | **Aceito (não implementado)** | Nenhum teste foi escrito com essa premissa; a observação consta no relatório. |
| Felipe Paixão | Investigação do defeito de montagem de lanche (issues #17 e #20) | Claude Code (Anthropic) | Pedi para confirmar ou descartar a suspeita de que `DaoLanche` não salvava os ingredientes. | Descartou a suspeita: o `INSERT` é executado. Localizou a causa real no servlet `salvarLancheCliente`, que reutiliza o mesmo `Iterator` em dois laços. | **Aceito** | Teste `deveVincularCadaIngredienteAoLanche` reproduz a falha (esperava 3 vínculos, recebeu 0), coerente com o resultado do CT-04 executado em 20/09. |
| Felipe Paixão | Slides da apresentação e roteiro de falas | Claude Code (Anthropic) | Pedi 12 slides, dois por integrante, e um roteiro em formato teleprompter. | Gerou `gerar-apresentacao.js` e `gerar-roteiro.py`, que produzem o `.pptx` e o `.pdf`. O roteiro lê as falas das notas do apresentador para os dois não divergirem. | **Alterado** | Corrigimos a atribuição dos módulos por integrante e substituímos os cartões que ainda diziam "não executado" pelos resultados reais dos casos manuais. |
| Felipe Paixão | Consolidação do relatório da entrega | Claude Code (Anthropic) | Pedi um relatório com comandos, ambiente, resultados, cobertura e defeitos, sem arredondar números. | Produziu `docs/entrega-1/README.md` a partir dos relatórios do Surefire e do JaCoCo. | **Aceito** | Números conferidos contra a saída real do Maven; os cinco defeitos vistos apenas nos testes manuais estão listados à parte, como ainda sem issue. |

---

## Observações

- **Alcance:** a ampliação da suíte não ficou restrita ao módulo de estoque. A ferramenta foi operada por mim nas
  cinco áreas, gerando também `ValidadorCookieLimitesTest`, `ComprarRegrasTest`, `CadastroTest`, `EncryptadorMD5Test`
  e `SalvarLancheClienteTest`, além da simplificação do `DaoClienteTest`. Os testes originais de cada área continuam
  sendo os que cada integrante commitou entre 19 e 21/09, e cada um mantém o seu próprio registro em `docs/ai/`.
- A execução dos casos de teste manuais (CT-01 a CT-05) foi feita pelos integrantes na aplicação em execução, com os
  prints anexados a cada documento. Cinco defeitos apareceram somente nessa etapa e não haviam sido detectados pelos
  testes automatizados, entre eles o erro HTTP 500 ao finalizar uma compra com bebida no carrinho.
- Nenhum defeito foi corrigido nesta entrega: o objetivo foi encontrar, reproduzir e registrar.
- Os testes unitários escritos com apoio da ferramenta foram executados e revisados antes do commit. Cada integrante
  deve conseguir abrir e explicar os testes da sua área.
