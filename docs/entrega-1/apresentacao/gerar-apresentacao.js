// Gera apresentacao-entrega-1.pptx (12 slides). Uso: npm install pptxgenjs && node gerar-apresentacao.js
// Os números vêm da execução de 21/09/2026 registrada em docs/entrega-1/README.md.
const path = require("path");
const pptxgen = require("pptxgenjs");

const COR = { fundo: "2B1B17", mostarda: "F2A900", tomate: "C8352B", texto: "2B1B17", suave: "6B5B55", cartao: "F6F1EE", branco: "FFFFFF", verde: "2E7D32" };
const FONTE = "Calibri";
const MONO = "Courier New";

const pres = new pptxgen();
pres.layout = "LAYOUT_16x9";
pres.title = "Code Burguer's - Entrega 1";

function texto(slide, conteudo, opcoes) {
  slide.addText(conteudo, Object.assign({ fontFace: FONTE, color: COR.texto, margin: 0, isTextBox: true, valign: "top" }, opcoes));
}

function cabecalho(slide, numero, area, titulo, responsavel) {
  slide.background = { color: COR.branco };
  slide.addShape(pres.ShapeType.ellipse, { x: 0.5, y: 0.35, w: 0.6, h: 0.6, fill: { color: COR.mostarda } });
  texto(slide, String(numero), { x: 0.5, y: 0.35, w: 0.6, h: 0.6, fontSize: 20, bold: true, align: "center", valign: "middle" });
  texto(slide, area.toUpperCase() + "  ·  " + responsavel, { x: 1.25, y: 0.33, w: 8.2, h: 0.28, fontSize: 11, color: COR.suave, charSpacing: 2 });
  texto(slide, titulo, { x: 1.25, y: 0.58, w: 8.2, h: 0.5, fontSize: 26, bold: true, fontFace: "Cambria" });
}

function rodape(slide, referencia) {
  texto(slide, referencia, { x: 0.5, y: 5.2, w: 9, h: 0.25, fontSize: 8.5, color: COR.suave });
}

function cartao(slide, x, y, w, h, cor) {
  slide.addShape(pres.ShapeType.roundRect, { x, y, w, h, rectRadius: 0.08, fill: { color: cor || COR.cartao }, line: { color: cor || COR.cartao } });
}

function bloco(slide, x, y, w, rotulo, corpo, altura) {
  texto(slide, rotulo.toUpperCase(), { x, y, w, h: 0.22, fontSize: 10, bold: true, color: COR.tomate, charSpacing: 1 });
  texto(slide, corpo, { x, y: y + 0.24, w, h: altura || 0.6, fontSize: 13 });
}

function codigo(slide, x, y, w, h, titulo, linhas) {
  cartao(slide, x, y, w, h, COR.fundo);
  texto(slide, titulo, { x: x + 0.2, y: y + 0.15, w: w - 0.4, h: 0.25, fontSize: 10, bold: true, color: COR.mostarda });
  texto(slide, linhas.join("\n"), { x: x + 0.2, y: y + 0.45, w: w - 0.4, h: h - 0.6, fontSize: 9.5, fontFace: MONO, color: COR.branco });
}

function numero(slide, x, y, valor, legenda, cor) {
  texto(slide, valor, { x, y, w: 2.1, h: 0.7, fontSize: 40, bold: true, color: cor || COR.texto, fontFace: "Cambria" });
  texto(slide, legenda, { x, y: y + 0.72, w: 2.1, h: 0.45, fontSize: 11, color: COR.suave });
}

const AREAS = [
  {
    n: 1, area: "Autenticação e sessão", quem: "Yuri Mascarenhas",
    classe: "Helpers.ValidadorCookie", metodos: "validar, validarFuncionario, deletar, getCookieIdCliente/Funcionario",
    naoCrud: "Não acessa tabela nem é entidade: percorre os cookies, decide pelo nome (token x tokenFuncionario) e delega ao DaoToken.",
    estrategia: "DaoToken mockado. Partições: sem cookies, nome trocado, token válido/inválido, duplicado, valor malformado, falha do DAO.",
    tituloCodigo: "ValidadorCookieLimitesTest — cookie duplicado",
    codigo: ["Cookie[] cookies = {", "  new Cookie(\"token\", \"7-valido\"),", "  new Cookie(\"token\", \"7-invalido\") };", "", "assertFalse(validador.validar(cookies));", "// validar() fica com o ULTIMO cookie,", "// getCookieIdCliente() com o PRIMEIRO"],
    testes: "26", cobertura: "22/22", defeitos: "0",
    manual: "CT-01 — Autenticação de usuário e validação de cookie (login, logout, página restrita).",
    statusManual: "Issue #6 fechada pelo grupo. Registro da execução no documento do grupo.",
    achadoTitulo: "Observação", achado: "Não existe expiração de token no servidor: tb_tokens guarda só o texto. O único prazo é o MaxAge de 30 min do cookie no navegador.",
    ref: "ValidadorCookieTest · ValidadorCookieLimitesTest · issues #1 e #6",
    fala1: "Minha parte é autenticação e sessão. A classe é o ValidadorCookie: ela não é CRUD, só recebe os cookies da requisição, procura o cookie certo pelo nome e pergunta ao DaoToken se o token existe. Nos testes o DaoToken é um mock, então nada depende de banco. Além dos casos básicos, testamos as fronteiras: requisição sem cookie, token de cliente tentando passar por funcionário, cookie duplicado e valor malformado. Para demonstrar: abrir ValidadorCookieLimitesTest e rodar mvn test -Dtest=ValidadorCookie*.",
    fala2: "São 26 testes passando e todos os 22 ramos da classe cobertos. O achado mais interessante: procuramos a regra de expiração de token e ela não existe no servidor. A tabela só guarda o texto do token; quem expira é o cookie no navegador, em 30 minutos. Registramos como observação de segurança, não inventamos um resultado. O teste manual é o CT-01, issue 6.",
  },
  {
    n: 2, area: "Carrinho e pedidos", quem: "Vinicius Rocha Pinheiro",
    classe: "Controllers.comprar", metodos: "processRequest (doGet/doPost)",
    naoCrud: "Servlet com regra de negócio: valida o cookie, interpreta o JSON do carrinho, separa lanches e bebidas, soma o total e vincula os itens.",
    estrategia: "Validador e 4 DAOs mockados; ArgumentCaptor confere o Pedido enviado ao DAO (total, cliente, quantidades), não só a mensagem.",
    tituloCodigo: "ComprarRegrasTest — DEF-02 (suíte -Pdefeitos)",
    codigo: ["// carrinho: 2 x X-Burger + 1 x Refrigerante", "double esperado = 2 * 15.00 + 1 * 5.00;", "", "enviar(\"{...\\\"X-Burger\\\": [\\\"15.00\\\",\\\"lanche\\\",2]...}\");", "", "assertEquals(esperado,", "  pedidoEnviadoAoDao().getValor_total(), 0.001);", "// expected: <35.0> but was: <20.0>"],
    testes: "22", cobertura: "15/16", defeitos: "2",
    manual: "CT-02 — Adição de itens e finalização de compra.",
    statusManual: "Issue #7 fechada pelo grupo. Registro da execução no documento do grupo.",
    achadoTitulo: "Defeitos DEF-02 e DEF-03", achado: "O total ignora a quantidade (35,00 na tela, 20,00 gravado). E o cliente do pedido vem do id do JSON, sem conferir com o cookie: dá para gravar pedido em nome de outro cliente.",
    ref: "ComprarTest · ComprarRegrasTest · issues #2 e #7 · DEF-02, DEF-03",
    fala1: "Minha parte é o carrinho. A classe é o servlet comprar, que tem regra de verdade: valida o cookie, lê o JSON que o carrinho.js monta, separa lanche de bebida, soma o total e grava os vínculos. Mockamos o validador e os quatro DAOs e usamos ArgumentCaptor para olhar o Pedido que chega no DAO. O valor esperado não sai do próprio código: usamos a conta que a tela do carrinho faz, preço vezes quantidade.",
    fala2: "São 22 testes passando na suíte normal e dois testes que ficam vermelhos de propósito na suíte de defeitos. O primeiro: com 2 lanches de 15 e 1 refrigerante de 5 a tela mostra 35, mas o servlet grava 20, porque soma o preço uma vez por item. O segundo: o id do cliente vem do JSON e nunca é comparado com o cookie. Para demonstrar: mvn test -Pdefeitos -Dtest=ComprarRegrasTest.",
  },
  {
    n: 3, area: "Cadastro de clientes", quem: "Felipe Paixão",
    classe: "DAO.DaoCliente + Controllers.cadastro + Helpers.EncryptadorMD5", metodos: "login, salvar, pesquisaPorUsuario · processRequest · encryptar",
    naoCrud: "login decide o acesso (hash MD5 igual E cliente ativo); salvar reaproveita endereço já existente. cadastro e EncryptadorMD5 não são DAO nem entidade.",
    estrategia: "Connection e DaoEndereco injetados por construtor (sem Objenesis/reflexão). Hash esperado vem da RFC 1321, não do próprio sistema.",
    tituloCodigo: "DaoClienteTest — cliente inativo",
    codigo: ["// senha correta, mas fg_ativo = 0", "bancoRespondeClienteDoLogin(", "  \"carlos.silva\", MD5_DE_123456, 0);", "", "assertFalse(dao.login(", "  tentativaDeLogin(\"carlos.silva\", \"123456\")));"],
    testes: "29", cobertura: "13/16", defeitos: "1",
    manual: "CT-03 — Validações no cadastro de usuário (campos obrigatórios, duplicados).",
    statusManual: "Issue #8 aberta: execução manual pendente.",
    achadoTitulo: "Defeitos DEF-05 e DEF-04", achado: "Servidor aceita nome, usuário e senha vazios; em cadastro.js a condição !field.name == 'complemento' é sempre falsa. E pesquisaPorUsuario concatena SQL: usuário com apóstrofo quebra o comando.",
    ref: "DaoClienteTest · CadastroTest · EncryptadorMD5Test · issues #3 e #8 · DEF-04, DEF-05",
    fala1: "Minha parte é o cadastro de clientes. O DaoCliente é um DAO, mas o login tem uma decisão: só entra se o hash MD5 bater e o cliente estiver ativo. E o salvar reaproveita um endereço que já existe. Completamos com duas classes que não são CRUD: o servlet cadastro e o EncryptadorMD5. Simplificamos o teste antigo, que usava Objenesis e reflexão, para injeção por construtor. O hash esperado vem dos vetores da RFC, inclusive o MD5 de 'a', que começa com zero e testa o preenchimento.",
    fala2: "São 29 testes passando. Defeitos: o servidor grava cliente com nome, usuário e senha vazios, e a validação da tela também não funciona por causa de uma condição sempre falsa no JavaScript. Outro: a busca por usuário concatena texto no SQL, então um apóstrofo quebra o login. O MD5 foi tratado como comportamento legado, só caracterizamos. O CT-03 manual ainda está pendente, issue 8.",
  },
  {
    n: 4, area: "Cardápio e montagem de lanches", quem: "Vinicius Fonseca de Freitas",
    classe: "Controllers.salvarLancheCliente + DAO.DaoLanche", metodos: "processRequest · salvar, salvarCliente, vincularIngrediente, pesquisaPorNome",
    naoCrud: "DaoLanche é CRUD. A regra está no servlet: preço = soma de (valor de venda x quantidade) dos ingredientes, lanche gravado inativo e vínculo de cada ingrediente.",
    estrategia: "DAOs mockados com um 'estoque' em memória; o preço esperado é calculado à mão no teste.",
    tituloCodigo: "SalvarLancheClienteTest — DEF-01 (suíte -Pdefeitos)",
    codigo: ["enviar(LANCHE_DA_CASA); // 3 ingredientes", "", "verify(lancheDao, times(3))", "  .vincularIngrediente(eq(lancheGravado), any());", "", "// Wanted 3 times, but was 0:", "// o Iterator ja foi consumido no", "// calculo do preco"],
    testes: "22", cobertura: "9/14", defeitos: "1",
    manual: "CT-04 — Montagem de lanche customizado.",
    statusManual: "Executado em 20/09 por Vinicius Fonseca: passo 'Adicionar' FALHOU (nada acontece) e 'Cancelar' FALHOU. Issue #17 aberta.",
    achadoTitulo: "Defeito DEF-01", achado: "A suspeita de 'ingredientes não salvos' não está no DaoLanche (o INSERT é executado). Está no servlet: o mesmo Iterator é usado em dois laços, e o segundo nunca roda.",
    ref: "DaoLancheTest · SalvarLancheClienteTest · issues #4, #9 e #17 · DEF-01",
    fala1: "Minha parte é a montagem de lanches. O DaoLanche é CRUD, então a regra que escolhemos está no servlet salvarLancheCliente: ele calcula o preço somando valor de venda vezes quantidade de cada ingrediente, grava o lanche como inativo e deveria vincular os ingredientes. O teste calcula o preço esperado à mão: 2 queijos, 3 bacons e 1 pão dá 18,50.",
    fala2: "Tínhamos uma suspeita de que os ingredientes não eram salvos. Investigamos: o DaoLanche faz o INSERT normalmente. O problema é no servlet, que usa o mesmo Iterator em dois laços; o primeiro consome tudo e o segundo nunca executa. O teste espera 3 vínculos e recebe zero. No teste manual CT-04, feito em 20 de setembro, o botão Adicionar não respondeu, e isso virou a issue 17. Demonstração: mvn test -Pdefeitos -Dtest=SalvarLancheClienteTest.",
  },
  {
    n: 5, area: "Insumos e estoque", quem: "Thiago Ferreira",
    classe: "DAO.DaoIngrediente + Controllers.salvarIngrediente / alterarIngrediente", metodos: "salvar, alterar, remover, listarTodosPorLanche, pesquisaPorNome · processRequest",
    naoCrud: "Lacuna assumida: DaoIngrediente é CRUD e o sistema não tem regra de estoque (não há baixa na compra). Melhor alternativa: os servlets, que autorizam o funcionário e interpretam o JSON.",
    estrategia: "Connection injetada; um caso feliz e um de falha por operação. Nos servlets: autorizado, não autorizado, quantidade inválida, valores negativos.",
    tituloCodigo: "IngredienteControllersTest — acesso ao painel",
    codigo: ["when(validadorCookie.validarFuncionario(any()))", "  .thenReturn(false);", "", "cadastroDeInsumo.processRequest(request, response);", "", "assertEquals(\"erro\", resposta.toString().trim());", "verifyNoInteractions(ingredienteDao);"],
    testes: "16", cobertura: "6/6", defeitos: "1",
    manual: "CT-05 — Gestão de insumos e atualização de estoque (inclui entradas negativas).",
    statusManual: "Issue #10 aberta: execução manual pendente.",
    achadoTitulo: "Defeito DEF-07", achado: "Quantidade -5 e valores negativos são aceitos: 'Ingrediente Salvo!'. Não há validação no servlet nem CHECK no banco. Não assumimos regra de 'venda maior que compra'.",
    ref: "DaoIngredienteTest · IngredienteControllersTest · issues #5 e #10 · DEF-07",
    fala1: "Minha parte é insumos e estoque. Aqui somos transparentes: o DaoIngrediente é CRUD e o sistema não tem regra de estoque, a compra nem dá baixa. Então cobrimos o DAO, que era a issue 5, e escolhemos como classe não CRUD os servlets de insumo, que verificam se quem chama é funcionário e convertem o JSON. Mostramos que um token de cliente não entra no painel.",
    fala2: "São 16 testes passando e os 6 ramos do DAO cobertos. O defeito: quantidade e valores negativos são aceitos e gravados, sem validação no servlet nem restrição no banco. Não afirmamos que o preço de venda precisa ser maior que o de compra, porque nada no sistema sustenta essa regra. O CT-05 manual ainda está pendente, issue 10.",
  },
];

// 1. Abertura
{
  const s = pres.addSlide();
  s.background = { color: COR.fundo };
  s.addShape(pres.ShapeType.ellipse, { x: 7.1, y: 0.9, w: 3.8, h: 3.8, fill: { color: COR.mostarda } });
  s.addShape(pres.ShapeType.ellipse, { x: 7.9, y: 1.7, w: 2.2, h: 2.2, fill: { color: COR.tomate } });
  texto(s, "QUALIDADE E TESTE DE SOFTWARE  ·  ENTREGA 1", { x: 0.6, y: 0.9, w: 6.2, h: 0.3, fontSize: 12, color: COR.mostarda, charSpacing: 3 });
  texto(s, "Code Burguer's", { x: 0.6, y: 1.35, w: 6.4, h: 0.9, fontSize: 48, bold: true, color: COR.branco, fontFace: "Cambria" });
  texto(s, "Plano de teste, testes unitários e testes manuais de uma lanchonete online em Java (Servlets + JDBC + PostgreSQL)", { x: 0.6, y: 2.35, w: 6.0, h: 0.9, fontSize: 16, color: "E8DFDB" });
  texto(s, "Yuri Mascarenhas · Vinicius Rocha Pinheiro · Felipe Paixão · Vinicius Fonseca de Freitas · Thiago Ferreira", { x: 0.6, y: 4.3, w: 6.2, h: 0.6, fontSize: 12, color: "E8DFDB" });
  s.addNotes("Abertura. O sistema é o Code Burguer's, uma lanchonete online em Java com Servlets e JDBC, baseado no projeto APS-04. Dividimos em cinco áreas, uma por integrante: cada um apresenta a classe que testou, a estratégia, o resultado e o que encontrou.");
}

// 2 a 11. Dois slides por integrante
AREAS.forEach((a) => {
  const s1 = pres.addSlide();
  cabecalho(s1, a.n, a.area, "O que testamos e como", a.quem);
  bloco(s1, 0.5, 1.3, 4.3, "Classe e métodos", [{ text: a.classe, options: { bold: true, breakLine: true } }, { text: a.metodos, options: { color: COR.suave, fontSize: 11 } }], 0.75);
  bloco(s1, 0.5, 2.4, 4.3, a.n === 5 ? "Não CRUD? Lacuna registrada" : "Por que não é CRUD", a.naoCrud, 1.0);
  bloco(s1, 0.5, 3.75, 4.3, "Estratégia unitária", a.estrategia, 1.0);
  codigo(s1, 5.1, 1.3, 4.4, 3.6, a.tituloCodigo, a.codigo);
  rodape(s1, a.ref);
  s1.addNotes(a.fala1);

  const s2 = pres.addSlide();
  cabecalho(s2, a.n, a.area, "Resultados e o que encontramos", a.quem);
  numero(s2, 0.5, 1.35, a.testes, "testes unitários passando (suíte padrão)", COR.verde);
  numero(s2, 0.5, 2.6, a.cobertura, "ramos cobertos na classe principal (JaCoCo)");
  numero(s2, 0.5, 3.85, a.defeitos, "teste(s) vermelho(s) na suíte de defeitos", a.defeitos === "0" ? COR.texto : COR.tomate);
  cartao(s2, 3.0, 1.3, 3.1, 3.7);
  bloco(s2, 3.2, 1.5, 2.7, "Cenário manual", a.manual, 1.1);
  bloco(s2, 3.2, 3.0, 2.7, "Situação", a.statusManual, 1.8);
  cartao(s2, 6.4, 1.3, 3.1, 3.7, COR.fundo);
  texto(s2, a.achadoTitulo.toUpperCase(), { x: 6.6, y: 1.5, w: 2.7, h: 0.25, fontSize: 10, bold: true, color: COR.mostarda, charSpacing: 1 });
  texto(s2, a.achado, { x: 6.6, y: 1.85, w: 2.7, h: 3.0, fontSize: 13, color: COR.branco });
  rodape(s2, "Sem testes de integração nesta entrega  ·  " + a.ref);
  s2.addNotes(a.fala2);
});

// 12. Encerramento
{
  const s = pres.addSlide();
  s.background = { color: COR.fundo };
  texto(s, "Encerramento", { x: 0.6, y: 0.45, w: 8.8, h: 0.6, fontSize: 32, bold: true, color: COR.branco, fontFace: "Cambria" });
  const destaques = [["115", "testes unitários\n0 falhas (mvn clean test)", COR.mostarda], ["5", "testes vermelhos na suíte\n-Pdefeitos, um por defeito", COR.tomate], ["6", "defeitos documentados\nDEF-01 a 05 e DEF-07", COR.branco]];
  destaques.forEach((d, i) => {
    texto(s, d[0], { x: 0.6 + i * 3.0, y: 1.25, w: 2.7, h: 0.8, fontSize: 48, bold: true, color: d[2], fontFace: "Cambria" });
    texto(s, d[1], { x: 0.6 + i * 3.0, y: 2.05, w: 2.7, h: 0.6, fontSize: 12, color: "E8DFDB" });
  });
  texto(s, "PENDÊNCIAS E LIMITES", { x: 0.6, y: 3.0, w: 8.8, h: 0.25, fontSize: 10, bold: true, color: COR.mostarda, charSpacing: 1 });
  texto(s, [
    { text: "Testes manuais CT-03 e CT-05 ainda não executados; CT-04 executado com falha (issue #17).", options: { bullet: true, breakLine: true } },
    { text: "Área 5 sem classe não CRUD de complexidade razoável: o sistema não tem regra de estoque.", options: { bullet: true, breakLine: true } },
    { text: "Nenhum defeito foi corrigido: o objetivo foi encontrar, reproduzir e registrar.", options: { bullet: true, breakLine: true } },
    { text: "Cobertura mede o que foi exercitado, não ausência de defeitos.", options: { bullet: true } },
  ], { x: 0.6, y: 3.3, w: 8.8, h: 1.6, fontSize: 13, color: COR.branco, paraSpaceAfter: 4 });
  texto(s, "Maven 3.8.4 · OpenJDK 1.8.0_322 · JUnit 5.8.2 · Mockito 4.6.1 · JaCoCo 0.8.11 · execução de 21/09/2026 · docs/entrega-1/README.md", { x: 0.6, y: 5.15, w: 8.8, h: 0.25, fontSize: 9, color: "B9ABA5" });
  s.addNotes("Fechamento. A suíte tem 115 testes unitários passando. Separamos 5 testes que descrevem o comportamento correto e ficam vermelhos enquanto os defeitos existirem; eles rodam com mvn test -Pdefeitos. No total documentamos 6 defeitos. Sendo honestos sobre os limites: dois testes manuais ainda não foram executados, a área de estoque não tem uma classe de regra de negócio porque o sistema não tem essa regra, e não corrigimos nenhum defeito, porque o objetivo da entrega era testar e reportar.");
}

pres.writeFile({ fileName: path.join(__dirname, "apresentacao-entrega-1.pptx") }).then((f) => console.log("gerado:", path.basename(f)));
