# Registro de Uso de Inteligência Artificial (AI-LOG)

**Aluno:** Thiago Ferreira
**Entrega:** Etapa 1 — Testes Unitários Automatizados
**Módulo:** Gestão de Clientes e Endereços (`DAO.DaoCliente`)

> **Este arquivo está incompleto: só o Thiago pode preenchê-lo.**
> As linhas abaixo trazem o que consta no histórico do repositório. Preencha as colunas de ferramenta, prompt e
> resultado com o que você realmente usou. Se não usou IA em alguma atividade, escreva "Sem uso de IA" na linha —
> isso também é uma resposta válida e é melhor do que deixar em branco.

---

| Responsável | Atividade | Ferramenta | Prompt / Instrução Utilizada | Resultado da IA | Decisão | Validação |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Thiago Ferreira | Criação de `DaoClienteTest` (commit `32c5b22`, 21/09): login com sucesso, senha incorreta e consultas por usuário e por ID | _preencher_ | _preencher_ | _preencher_ | _preencher_ | _preencher_ |
| Thiago Ferreira | Uso de Objenesis e reflexão para injetar uma conexão falsa no campo privado do DAO | _preencher_ | _preencher_ | _preencher_ | _preencher_ | _preencher_ |
| Thiago Ferreira | Execução do caso de teste manual CT-03 (21/09): 2 passos PASSOU, 5 FALHOU | Sem uso de IA | — | — | — | Execução na aplicação em Docker, com prints anexados ao documento do caso. |

---

## Observações

- A abordagem com Objenesis e reflexão foi substituída por injeção de `Connection` e `DaoEndereco` via construtor,
  para deixar o teste mais simples de ler. Essa alteração foi feita com apoio de ferramenta de IA operada por
  Felipe Paixão e está registrada em [`docs/ai/felipe-paixao/AI-LOG.md`](../felipe-paixao/AI-LOG.md), junto com a
  ampliação dos casos (cliente inativo, usuário inexistente, falha de SQL e cadastro com endereço).
- Achados desta área confirmados no teste manual CT-03: o cadastro aceita campos obrigatórios vazios, aceita dois
  clientes com o mesmo usuário (o login passa a valer para o último) e grava nomes com acento corrompidos.
