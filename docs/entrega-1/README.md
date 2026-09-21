# Entrega 1 — Testes unitários

Código original: projeto APS-04-Lanchonete-Online-em-Java (histórico preservado neste repositório; último commit
anterior aos testes do grupo: `a8a812b`). Base desta entrega: `develop` em `2b86d10`.

## Como executar

Requer Java 8 e Maven 3.8, ou apenas Docker:

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.8.4-openjdk-8 mvn -B clean test
```

- `mvn clean test` — suíte padrão. Relatórios em `target/surefire-reports`, cobertura JaCoCo em `target/site/jacoco/index.html`.
- `mvn test -Pdefeitos` — só os testes marcados com `@Tag("defeito-conhecido")`. Eles descrevem o comportamento
  esperado e ficam vermelhos enquanto o defeito existir; por isso ficam fora da suíte padrão (o `Dockerfile` roda `mvn package`).

## Resultado (21/09/2026, Maven 3.8.4, OpenJDK 1.8.0_322)

| Suíte | Executados | Passaram | Falharam |
|---|---|---|---|
| Padrão (`mvn clean test`) | 115 | 115 | 0 |
| Defeitos conhecidos (`-Pdefeitos`) | 5 | 0 | 5 |

## Classes por integrante

| Área | Classe(s) sob teste | Testes |
|---|---|---|
| 1. Autenticação e sessão | `Helpers.ValidadorCookie` | `ValidadorCookieTest`, `ValidadorCookieLimitesTest` |
| 2. Carrinho e pedidos | `Controllers.comprar` | `ComprarTest`, `ComprarRegrasTest` |
| 3. Cadastro de clientes | `DAO.DaoCliente` (login e cadastro com endereço), `Controllers.cadastro`, `Helpers.EncryptadorMD5` | `DaoClienteTest`, `CadastroTest`, `EncryptadorMD5Test` |
| 4. Cardápio e montagem | `Controllers.salvarLancheCliente`, `DAO.DaoLanche` | `SalvarLancheClienteTest`, `DaoLancheTest` |
| 5. Insumos e estoque | `DAO.DaoIngrediente`, `Controllers.salvarIngrediente`, `Controllers.alterarIngrediente` | `DaoIngredienteTest`, `IngredienteControllersTest` |

Observação: `DaoIngrediente` e `DaoLanche` são essencialmente CRUD. A regra não CRUD da área 4 está em
`salvarLancheCliente` (cálculo do preço e vínculo dos ingredientes). Na área 5 o sistema não tem regra de estoque
(não há baixa na compra nem validação de valores); os servlets de insumo (autorização de funcionário + contrato do
JSON) foram a melhor alternativa encontrada.

## Cobertura das classes em escopo (JaCoCo 0.8.11, só testes unitários)

| Classe | Linhas | Ramos |
|---|---|---|
| `Helpers.ValidadorCookie` | 40/42 | 22/22 |
| `Controllers.comprar` | 70/72 | 15/16 |
| `DAO.DaoCliente` | 78/99 | 13/16 |
| `Controllers.cadastro` | 32/39 | 2/4 |
| `Helpers.EncryptadorMD5` | 14/14 | 2/2 |
| `Controllers.salvarLancheCliente` | 44/58 | 9/14 |
| `DAO.DaoLanche` | 86/89 | 6/6 |
| `DAO.DaoIngrediente` | 103/106 | 6/6 |
| `Controllers.salvarIngrediente` | 34/41 | 5/8 |
| `Controllers.alterarIngrediente` | 35/42 | 5/8 |

## Defeitos encontrados

| ID | Defeito | Teste que reproduz |
|---|---|---|
| DEF-01 | `salvarLancheCliente` nunca vincula os ingredientes ao lanche montado: o mesmo `Iterator` é consumido no cálculo do preço e reutilizado no laço do vínculo | `SalvarLancheClienteTest.deveVincularCadaIngredienteAoLanche` |
| DEF-02 | `comprar` ignora a quantidade no total: 2 × 15,00 + 1 × 5,00 é gravado como 20,00 (o carrinho mostra 35,00) | `ComprarRegrasTest.deveMultiplicarPrecoPelaQuantidade` |
| DEF-03 | `comprar` usa o `id` do JSON e não o do cookie: cliente logado grava pedido em nome de outro | `ComprarRegrasTest.naoDeveGravarPedidoParaOutroCliente` |
| DEF-04 | Consultas por nome/usuário concatenam SQL: apóstrofo quebra o comando (e permite injeção) | `DaoClienteTest.caracterizacaoPesquisaPorUsuarioConcatenaSql` |
| DEF-05 | Cadastro aceita nome, usuário e senha vazios (em `cadastro.js`, `!field.name == 'complemento'` é sempre falso, então a validação da tela também não dispara) | `CadastroTest.naoDeveGravarClienteComCamposObrigatoriosVazios` |
| DEF-07 | Insumo aceita quantidade e valores negativos (sem validação no servlet nem `CHECK` no banco) | `IngredienteControllersTest.naoDeveGravarEstoqueOuValoresNegativos` |

Outras observações: não existe expiração de token no servidor (`tb_tokens` só guarda o texto; o único prazo é o
`MaxAge` de 30 min do cookie); `tb_clientes.usuario` não tem `UNIQUE`.

## Alterações no código de produção (testabilidade)

Somente construtores de injeção, mantendo o construtor padrão e o comportamento no Tomcat: `DaoToken`, `DaoEndereco`,
`DaoIngrediente`, `DaoPedido`, `DaoBebida` (recebem `Connection`), `DaoCliente` (recebe `Connection` e `DaoEndereco`),
`cadastro`, `salvarLancheCliente`, `salvarIngrediente` e `alterarIngrediente` (recebem validador e DAOs).
Nenhum defeito foi corrigido nesta entrega.
