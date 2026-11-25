# Vídeo da Entrega 2 — Code Burguer's

`code-burguers-entrega2.mp4` · 4min48s · 1080p · legenda embutida e em `.srt` · acelerado 1,25x.

## Capítulos

- `0:00` Code Burguer's · Entrega 2
- `0:18` Yuri Mascarenhas · Autenticação e sessão
- `0:22` Yuri Mascarenhas · números
- `0:33` Yuri Mascarenhas · Selenium
- `1:09` Vinicius Rocha Pinheiro · Carrinho e pedidos
- `1:13` Vinicius Rocha Pinheiro · números
- `1:23` Vinicius Rocha Pinheiro · Selenium
- `1:50` Vinicius Rocha Pinheiro · SonarQube
- `1:59` Thiago Ferreira · Cadastro de clientes
- `2:03` Thiago Ferreira · números
- `2:12` Thiago Ferreira · Selenium
- `2:49` Thiago Ferreira · SonarQube
- `2:57` Vinicius Fonseca de Freitas · Montagem de lanches
- `3:01` Vinicius Fonseca de Freitas · números
- `3:17` Vinicius Fonseca de Freitas · SonarQube
- `3:38` Felipe Paixão · Insumos e estoque
- `3:42` Felipe Paixão · números
- `3:51` Felipe Paixão · Selenium
- `4:22` Felipe Paixão · SonarQube
- `4:31` Fechamento

## Como foi gravado

- Testes de sistema (`AutenticacaoE2E`, `CheckoutE2E`, `CadastroE2E`, `EstoqueE2E`) rodando no Selenium com `-De2e.demo=true` (janela visível, destaque em cada passo); os vermelhos de propósito foram incluídos com `-Dgrupos.excluidos=`.
- Resultado das gravações: 17 testes, 13 verdes e 4 vermelhos de propósito (RNF de segurança, usuário duplicado, campos vazios, acento).
- Números do fechamento tirados da saída real: `mvn clean verify` (277, 0 falhas, cobertura aprovada), `-Pdefeitos` (8 vermelhos), `-Pintegracao` (25, 0 falhas).
- Prints do SonarQube: os que já estão em `docs/entrega-2/sonar` e `docs/entrega-2/thiago-ferreira/Teste de inspeção`.

## Narração

Voz gerada com a Gemini Live API (modelo `gemini-3.8-live`, voz Charon), uma fala por vez. A coluna de conferência compara a transcrição da própria voz com o texto.

| id | fala | conferência |
|---|---|---|
| a1 | Code Burguer's: Entrega 2 de Qualidade e Teste de Software. | 100% |
| a2 | Aqui o sistema se testa sozinho: o Selenium dirige o Chrome, o teste confere o banco a cada passo, e o SonarQube mostra o código antes e depois. | 100% |
| a3 | São cinco áreas, uma por integrante. | 100% |
| y1 | Agora é a parte do Yuri Mascarenhas: login e sessão. | 100% |
| y2 | O ValidadorCookie foi de onze para vinte e oito testes, por partição de equivalência: todas as vinte e duas arestas e setenta e três de setenta e quatro mutantes mortos. | 100% |
| y3 | No navegador, o login válido grava o token no banco e cria o cookie do cliente. | 100% |
| y4 | Senha errada não cria cookie, e o painel aberto sem login volta para o login de funcionário. | 100% |
| y5 | Depois do logout, o cookie antigo é reapresentado, e o sistema recusa. | 100% |
| y6 | E esse fica vermelho de propósito: o token do cliente, copiado para o cookie de funcionário, abre o painel de estoque. | 100% |
| y7 | Esse teste tinha saído do master; aqui ele rodou a partir do histórico e precisa voltar. | 100% |
| p1 | Agora é a parte do Vinicius Pinheiro: carrinho e fechamento do pedido. | 100% |
| p2 | Ele montou a base do grupo no pom: JaCoCo, PITest e Failsafe. No servlet comprar, cem por cento das arestas e trinta de trinta mutantes mortos. | 100% |
| p3 | O robô entra como cliente e coloca um lanche e uma bebida no carrinho. | 100% |
| p4 | O total confere: vinte e seis e cinquenta. Escolhe a forma de pagamento e finaliza. | 100% |
| p5 | Pedido salvo, e o teste confere no PostgreSQL o pedido e os itens. | 100% |
| p6 | Nos negativos, sem login o carrinho volta para o login, e sem cookie a compra é recusada sem criar pedido. | 100% |
| p7 | No SonarQube, o comprar tinha vinte e dois apontamentos. Sobraram dois, só de nome, e a complexidade do método principal caiu de vinte e um para dois. | 100% |
| t1 | Agora é a parte do Thiago Ferreira: cadastro e login do cliente. | 100% |
| t2 | Tabela de decisão no login, valor limite no campo ativo, quinze de dezesseis ramos e quarenta e oito de quarenta e nove mutantes mortos. | 100% |
| t3 | O robô cadastra uma cliente, faz login, e o carrinho mostra o nome dela. No banco, a senha está em MD5. | 100% |
| t4 | Os defeitos do teste manual continuam vermelhos pelo mesmo motivo: o usuário duplicado é aceito, o formulário vazio dá erro quinhentos, e o acento corrompe o nome. | 100% |
| t5 | E o endereço do carrinho só aparece certo quando o id do cliente coincide com o do endereço. Nessa execução coincidiu. | 100% |
| t6 | No SonarQube, de trinta e três apontamentos para oito, e sumiram os dois bugs. Um deles imprimia o hash da senha no console. | 100% |
| f1 | Agora é a parte do Vinicius Fonseca: montagem do lanche personalizado. | 100% |
| f2 | A regra saiu do DaoLanche para o MontagemLancheService, e os requisitos foram escritos antes dos testes. | 100% |
| f3 | Lanche nulo, nome vazio, valor NaN, infinito, zero, ingrediente repetido: em todo caso inválido, o banco nem é chamado. | 100% |
| f4 | No SonarQube, ele escolheu o salvarLancheCliente, que tinha cinco apontamentos, e corrigiu: campo mutável, exceções sem tratamento e um possível NullPointerException. E o PR dos testes passou no Quality Gate. | 100% |
| f5 | O lanche ainda é salvo sem os ingredientes, e o teste segue vermelho de propósito. Integração e Selenium desse módulo ficaram pendentes. | 100% |
| e1 | Agora é a parte do Felipe Paixão: insumos e estoque. | 100% |
| e2 | Com asserções mais rigorosas no DaoIngrediente, os mutantes mortos foram de trinta e cinco de cinquenta e cinco para vinte e nove de vinte e nove. | 100% |
| e3 | O robô cadastra um cheddar pelo painel, e o teste confere o insumo no banco e na tabela do estoque. | 100% |
| e4 | Depois dá baixa no bacon, de cinquenta para trinta e cinco, e remove. | 100% |
| e5 | Sem login, o estoque é bloqueado. E o defeito vinte e cinco continua: o painel aceita quantidade negativa, e o banco grava. | 100% |
| e6 | No SonarQube, os dez apontamentos do DaoIngrediente foram zerados, e a consulta passou a usar parâmetro: um nome com apóstrofo não quebra mais o SQL. | 100% |
| z1 | Fechando: duzentos e setenta e sete testes unitários passando, e oito vermelhos de propósito, um para cada defeito conhecido. | 100% |
| z2 | Mutação e cobertura altas dizem que os testes são fortes. Não dizem que o sistema não tem defeito. | 100% |
| z3 | Esse foi o Code Burguer's. Obrigado! | 100% |
