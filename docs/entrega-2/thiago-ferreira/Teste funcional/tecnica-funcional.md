# Técnica funcional — cadastro e login (Thiago Ferreira)

Os casos de teste do cadastro e do login foram escolhidos pela especificação, sem olhar o código, com três técnicas:
partição de equivalência, tabela de decisão e análise de valor limite. A especificação é a história de usuário U4
("Cadastrar-se como cliente"), usada no caso de teste manual CT-03, mais as regras do login. Cada caso aponta o teste
unitário que o implementa.

## Partição de equivalência: cadastro (U4)

Cada entrada foi dividida em classes de valores que o sistema deve tratar do mesmo jeito. Basta um teste por classe,
porque qualquer valor da classe deve dar o mesmo resultado.

| Entrada | Classe | Válida? | Resultado esperado | Teste (`CadastroTest`) |
| --- | --- | --- | --- | --- |
| Campos do usuário | todos preenchidos | sim | grava o cliente | `deveMapearTodosOsCamposDoEndereco` |
| Campos do usuário | campo ausente no JSON (5 casos) | não | recusa, nada é gravado | `deveRecusarCampoObrigatorioAusente` |
| Campos do usuário | campo vazio `""` | não | recusa; **hoje grava (DEF-05)** | `naoDeveGravarClienteComCamposObrigatoriosVazios` |
| Endereço | campo ausente no JSON (5 casos) | não | recusa, nada é gravado | `deveRecusarCampoDeEnderecoAusente` |
| `numero` | texto `"77"` ou número `77` | sim | aceita os dois formatos | `deveAceitarNumeroComoNumeroJson` |
| Usuário | ainda não cadastrado | sim | grava | `doPostDeveCadastrar` |
| Usuário | já cadastrado | não | recusa; **hoje grava (achado novo)** | `naoDeveGravarUsuarioDuplicado` |
| Nome | com acento | sim | grava intacto; **hoje corrompe (achado novo)** | `devePreservarAcentosDoNome` |
| Corpo da requisição | JSON malformado | não | nada é gravado | `deveRecusarJsonMalformado` |
| Corpo da requisição | vazio | não | erro tratado; hoje dá HTTP 500 | `caracterizacaoCorpoVazio` |
| Campos extras | `fg_ativo` ou `admin` enviados pelo cliente | não | ignorados; o cadastro nasce ativo | `deveIgnorarCamposExtras` |

Os três casos em negrito são testes `defeito-conhecido`: ficam vermelhos até o defeito ser corrigido.

## Tabela de decisão: login

O login só libera o acesso quando três condições são verdadeiras ao mesmo tempo. Cada coluna é uma regra (uma
combinação de condições); "—" significa que a condição não importa naquela regra.

| Condição | R1 | R2 | R3 | R4 | R5 |
| --- | --- | --- | --- | --- | --- |
| Banco responde | sim | sim | sim | sim | **não** |
| Usuário existe | **não** | sim | sim | sim | — |
| Senha confere | — | **não** | sim | sim | — |
| `fg_ativo == 1` | — | — | **não** | sim | — |
| **Resultado** | negado | negado | negado | **entra** | negado |
| Teste (`DaoClienteTest`) | `deveRecusarUsuarioInexistente` | `deveNegarQuandoHashDiferente` | `deveRecusarClienteInativo` | `deveUsarHashDoEncryptadorInjetado` | `deveNegarLoginQuandoBancoFalha` |

## Análise de valor limite

Os erros costumam aparecer nas bordas das classes, então os testes usam o valor exato da regra e os vizinhos dele.

| Entrada | Valores testados | Resultado esperado | Teste |
| --- | --- | --- | --- |
| `fg_ativo` | -1, 0, **1**, 2 | só o 1 entra | `deveExigirFgAtivoIgualAUm` (-1, 0, 2) e `testeLoginComSucesso` (1) |
| Senha: maiúsculas e espaços | `"Senha Forte"` × `"senha forte"`; `"123456"` × `"123456 "` | hashes diferentes | `deveDiferenciarMaiusculasEEspacos` (`EncryptadorMD5Test`) e `deveDiferenciarMaiusculasNaSenha` (`DaoClienteTest`) |
| Senha nula | `null` | devolve `null` sem lançar exceção | `caracterizacaoTextoNuloDevolveNull` (`EncryptadorMD5Test`) |

Duas classes ainda não têm teste: campo preenchido só com espaços (`"   "`, variação do DEF-05) e senha com acento no
login.
