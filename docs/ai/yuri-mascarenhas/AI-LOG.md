# Registro de Uso de Inteligência Artificial (AI-LOG)

**Aluno:** Yuri Mascarenhas
**Entrega:** Etapa 1 — Testes Unitários Automatizados
**Módulo:** Autenticação, Sessões e Controle de Acesso (`Helpers.ValidadorCookie`)

> **Este arquivo está incompleto: só o Yuri pode preenchê-lo.**
> As linhas abaixo trazem o que consta no histórico do repositório. Preencha as colunas de ferramenta, prompt e
> resultado com o que você realmente usou. Se não usou IA em alguma atividade, escreva "Sem uso de IA" na linha —
> isso também é uma resposta válida e é melhor do que deixar em branco.

---

| Responsável | Atividade | Ferramenta | Prompt / Instrução Utilizada | Resultado da IA | Decisão | Validação |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Yuri Mascarenhas | Criação de `ValidadorCookieTest` (commit `77ea328`, 19/09): testes de extração de ID e validação de cookies | _preencher_ | _preencher_ | _preencher_ | _preencher_ | _preencher_ |
| Yuri Mascarenhas | Refatoração de `ValidadorCookie` para receber `DaoToken` por construtor, permitindo mock (commits `2c346c9` e `8adca79`, 19/09) | _preencher_ | _preencher_ | _preencher_ | _preencher_ | _preencher_ |
| Yuri Mascarenhas | Testes de validação de token e de remoção no logout (commit `baf71a2`, 20/09) | _preencher_ | _preencher_ | _preencher_ | _preencher_ | _preencher_ |
| Yuri Mascarenhas | Execução do caso de teste manual CT-01 (21/09): 8 passos, todos PASSOU | Sem uso de IA | — | — | — | Execução na aplicação em Docker, com prints anexados ao documento do caso. |

---

## Observações

- A ampliação da suíte desta área (`ValidadorCookieLimitesTest`, com cookies ausentes, nomes trocados, duplicidade e
  falha do DAO) foi feita com apoio de ferramenta de IA operada por Felipe Paixão e está registrada em
  [`docs/ai/felipe-paixao/AI-LOG.md`](../felipe-paixao/AI-LOG.md).
- Achado desta área: não existe expiração de token no servidor. A tabela `tb_tokens` guarda apenas o texto do token;
  o único prazo é o `MaxAge` de 30 minutos do cookie no navegador.
