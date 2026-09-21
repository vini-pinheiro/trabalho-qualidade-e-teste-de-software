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
    statusManual: "Responsável: Yuri Mascarenhas. Issue #6 fechada pelo grupo. O documento do caso, no formato do CT-04, não está no repositório.",
    achadoTitulo: "Observação de segurança", achado: "Não existe expiração de token no servidor: tb_tokens guarda só o texto. O único prazo é o MaxAge de 30 min do cookie no navegador.",
    ref: "ValidadorCookieTest · ValidadorCookieLimitesTest · issues #1 e #6",
    fala1: "Minha parte é login e sessão.\nA classe chama ValidadorCookie.\n[PAUSA]\nO que ela faz dá pra explicar em uma frase:\nchega uma requisição, ela olha os cookies,\nprocura o cookie chamado token,\ne pergunta pro banco se aquele token existe.\n[PAUSA]\nPor que ela não é CRUD?\nPorque ela não salva e não lê tabela nenhuma.\nEla decide. Ela olha o nome do cookie e escolhe o caminho.\n[PAUSA]\nNos testes o banco é falso, a gente usa Mockito.\nEntão roda em menos de um segundo, sem subir nada.\n[PAUSA]\nE não testamos só o caminho feliz.\nTestamos requisição sem cookie nenhum,\ntoken de cliente tentando entrar como funcionário,\ne esse aí do slide: cookie repetido.",
    fala2: "26 testes passando.\nE os 22 ramos da classe cobertos.\nOu seja: todo if e todo else foi executado pelo menos uma vez.\n[PAUSA]\nAgora a parte interessante.\nA gente foi procurar onde o token expira.\n[PAUSA]\nE descobriu que ele não expira.\n[PAUSA]\nNo servidor, a tabela guarda só o texto do token. Nada mais.\nQuem expira é o cookie no navegador, em 30 minutos.\nSó que se alguém copiar aquele token, ele funciona pra sempre.\n[PAUSA]\nA gente não inventou uma regra que não existe.\nRegistrou como observação de segurança, e está no slide.",
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
    statusManual: "Responsável: Vinicius Rocha Pinheiro. Issue #7 fechada pelo grupo. O documento do caso, no formato do CT-04, não está no repositório.",
    achadoTitulo: "Issues #21 e #22 · DEF-02 e DEF-03", achado: "O total ignora a quantidade (35,00 na tela, 20,00 gravado). E o cliente do pedido vem do id do JSON, sem conferir com o cookie: dá para gravar pedido em nome de outro cliente.",
    ref: "ComprarTest · ComprarRegrasTest · issues #2 e #7 · DEF-02, DEF-03",
    fala1: "Minha parte é o carrinho e o fechamento do pedido.\nA classe é o servlet comprar.\n[PAUSA]\nEle tem regra de verdade:\nvalida o cookie, lê o JSON que o carrinho manda,\nsepara o que é lanche e o que é bebida,\nsoma o total, e grava o pedido com os itens.\n[PAUSA]\nO detalhe importante do meu teste é esse:\neu não acredito na mensagem que o sistema responde.\n[PAUSA]\nEu pego o Pedido que ele manda pro banco e abro pra conferir.\nO total, o cliente, a quantidade de cada item.\n[PAUSA]\nPorque dá pra um teste passar só olhando\na mensagem Pedido salvo com sucesso,\ne o pedido estar todo errado por dentro.",
    fala2: "22 testes passando.\nE dois vermelhos de propósito. Já explico o porquê.\n[PAUSA]\nPõe no carrinho dois lanches de quinze reais\ne um refrigerante de cinco.\nA tela mostra trinta e cinco.\n[PAUSA]\nO que o sistema grava no banco? Vinte.\n[PAUSA]\nEle soma o preço uma vez por item e ignora a quantidade.\nNa prática: quanto mais você compra, mais barato fica.\nEssa é a issue vinte e um.\n[PAUSA]\nE tem outra, a vinte e dois, que é pior.\nO id do cliente vem do JSON que o navegador manda.\nO sistema nunca compara com o cookie de quem está logado.\n[PAUSA]\nEntão eu, logado na minha conta,\nconsigo fazer um pedido no nome de qualquer um de vocês.",
  },
  {
    n: 3, area: "Cadastro de clientes", quem: "Thiago Ferreira",
    classe: "DAO.DaoCliente + Controllers.cadastro + Helpers.EncryptadorMD5", metodos: "login, salvar, pesquisaPorUsuario · processRequest · encryptar",
    naoCrud: "login decide o acesso (hash MD5 igual E cliente ativo); salvar reaproveita endereço já existente. cadastro e EncryptadorMD5 não são DAO nem entidade.",
    estrategia: "Connection e DaoEndereco injetados por construtor (sem Objenesis/reflexão). Hash esperado vem da RFC 1321, não do próprio sistema.",
    tituloCodigo: "DaoClienteTest — cliente inativo",
    codigo: ["// senha correta, mas fg_ativo = 0", "bancoRespondeClienteDoLogin(", "  \"carlos.silva\", MD5_DE_123456, 0);", "", "assertFalse(dao.login(", "  tentativaDeLogin(\"carlos.silva\", \"123456\")));"],
    testes: "29", cobertura: "13/16", defeitos: "1",
    manual: "CT-03 — Validações no cadastro de usuário (campos obrigatórios, duplicados).",
    statusManual: "Responsável: Thiago Ferreira. Não executado — issue #8 aberta. Roteiro pronto: campos obrigatórios vazios e usuário duplicado, cenários que os testes já reprovam.",
    achadoTitulo: "Issues #24 e #23 · DEF-05 e DEF-04", achado: "Servidor aceita nome, usuário e senha vazios; em cadastro.js a condição !field.name == 'complemento' é sempre falsa. E pesquisaPorUsuario concatena SQL: usuário com apóstrofo quebra o comando.",
    ref: "DaoClienteTest · CadastroTest · EncryptadorMD5Test · issues #3 e #8 · DEF-04, DEF-05",
    fala1: "Minha parte é o cadastro e o login do cliente.\nSão três classes: o DaoCliente, o servlet de cadastro e o EncryptadorMD5.\n[PAUSA]\nO DaoCliente é um DAO, então tem bastante CRUD nele.\nMas o método login tem uma decisão de verdade:\nsó entra se o hash da senha bater\ne se o cliente estiver ativo.\n[PAUSA]\nUma coisa que a gente mudou e que vale comentar:\no teste que já existia usava Objenesis e reflexão\npra conseguir enfiar um banco falso dentro da classe.\nFuncionava, mas ninguém entendia ao ler.\n[PAUSA]\nA gente trocou por um construtor normal, que recebe a conexão.\nFicou bem mais simples.\n[PAUSA]\nE o hash esperado a gente não tirou do próprio sistema.\nSe o sistema estiver errado, o teste erraria junto.\nPegamos da tabela oficial do MD5.",
    fala2: "29 testes passando.\nE aqui aparecem dois problemas.\n[PAUSA]\nO primeiro: o cadastro aceita campo vazio.\nNome vazio, usuário vazio, senha vazia.\nO servidor grava e responde Usuário cadastrado.\n[PAUSA]\nAí vocês perguntam: mas a tela não valida?\nNão valida.\n[PAUSA]\nTem um if no JavaScript escrito errado,\nque nunca é verdadeiro.\nEntão aquele alerta de campo obrigatório nunca aparece.\nEssa é a issue vinte e quatro.\n[PAUSA]\nO segundo é a busca por usuário.\nEla monta o SQL colando texto.\n[PAUSA]\nEntão se a pessoa se chama O'Brien, com apóstrofo, o login quebra.\nE se ela for esperta, digita SQL no campo de usuário\ne o banco executa.\nIssue vinte e três.",
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
    statusManual: "Executado em 20/09 por Vinicius Fonseca: passo 'Adicionar' FALHOU (nada acontece) e 'Cancelar' FALHOU. Virou a issue #17; a causa está na #20.",
    achadoTitulo: "Issue #20 · DEF-01", achado: "A suspeita de 'ingredientes não salvos' não está no DaoLanche (o INSERT é executado). Está no servlet: o mesmo Iterator é usado em dois laços, e o segundo nunca roda.",
    ref: "DaoLancheTest · SalvarLancheClienteTest · issues #4, #9 e #17 · DEF-01",
    fala1: "Minha parte é a montagem do lanche personalizado.\nAquela tela onde o cliente escolhe pão, carne, queijo, bacon.\n[PAUSA]\nO DaoLanche é CRUD puro,\nentão a regra que eu testei está no servlet.\n[PAUSA]\nEle faz três coisas:\ncalcula o preço somando ingrediente por ingrediente,\nsalva o lanche como inativo, porque não é do cardápio,\ne liga cada ingrediente ao lanche.\n[PAUSA]\nNo teste eu faço a conta na mão:\ndois queijos, três bacons e um pão dá dezoito e cinquenta.\n[PAUSA]\nSe eu deixasse o teste usar a conta do próprio código,\no código poderia mudar e o teste mudaria junto, sem reclamar.\nFazendo na mão, ele reclama.",
    fala2: "Esse slide é o meu favorito.\n[PAUSA]\nA gente já desconfiava que os ingredientes não estavam sendo salvos.\nE a suspeita era do DaoLanche.\n[PAUSA]\nFomos olhar: o DaoLanche está certo.\nEle executa o INSERT normalmente.\n[PAUSA]\nO problema está no servlet.\nTem dois laços lá, um embaixo do outro, usando o mesmo Iterator.\n[PAUSA]\nO primeiro laço, o do preço, consome tudo.\nQuando chega no segundo, o que liga os ingredientes,\nnão sobrou nada. Ele simplesmente não roda.\n[PAUSA]\nO teste esperava três ingredientes ligados ao lanche.\nRecebeu zero.\n[PAUSA]\nE olha que interessante: isso bate com o teste manual.\nDia vinte eu montei um lanche na mão,\ncliquei em Adicionar, e não aconteceu nada.\nVirou a issue dezessete.\nAgora a gente sabe o motivo, que é a issue vinte.",
  },
  {
    n: 5, area: "Insumos e estoque", quem: "Felipe Paixão",
    classe: "DAO.DaoIngrediente + Controllers.salvarIngrediente / alterarIngrediente", metodos: "salvar, alterar, remover, listarTodosPorLanche, pesquisaPorNome · processRequest",
    naoCrud: "Lacuna assumida: DaoIngrediente é CRUD e o sistema não tem regra de estoque (não há baixa na compra). Melhor alternativa: os servlets, que autorizam o funcionário e interpretam o JSON.",
    estrategia: "Connection injetada; um caso feliz e um de falha por operação. Nos servlets: autorizado, não autorizado, quantidade inválida, valores negativos.",
    tituloCodigo: "IngredienteControllersTest — acesso ao painel",
    codigo: ["when(validadorCookie.validarFuncionario(any()))", "  .thenReturn(false);", "", "cadastroDeInsumo.processRequest(request, response);", "", "assertEquals(\"erro\", resposta.toString().trim());", "verifyNoInteractions(ingredienteDao);"],
    testes: "16", cobertura: "6/6", defeitos: "1",
    manual: "CT-05 — Gestão de insumos e atualização de estoque (inclui entradas negativas).",
    statusManual: "Responsável: Felipe Paixão. Não executado — issue #10 aberta. Roteiro pronto: cadastrar insumo com quantidade -5 e zerar o estoque pelo painel.",
    achadoTitulo: "Issue #25 · DEF-07", achado: "Quantidade -5 e valores negativos são aceitos: 'Ingrediente Salvo!'. Não há validação no servlet nem CHECK no banco. Não assumimos regra de 'venda maior que compra'.",
    ref: "DaoIngredienteTest · IngredienteControllersTest · issues #5 e #10 · DEF-07",
    fala1: "Minha parte é estoque e insumos.\nE eu vou ser honesto com vocês sobre uma coisa.\n[PAUSA]\nO requisito pedia uma classe que não fosse CRUD,\ncom uma complexidade razoável.\nNa minha área, essa classe não existe.\n[PAUSA]\nO DaoIngrediente é CRUD do começo ao fim.\nE o sistema não tem regra de estoque nenhuma:\nquando alguém compra, o estoque não baixa.\n[PAUSA]\nEntão a gente fez duas coisas.\nCobriu o DAO, que era o que a issue cinco pedia.\nE pegou como classe não CRUD os servlets do painel,\nque pelo menos decidem quem pode entrar e quem não pode.\n[PAUSA]\nEssa é a limitação do nosso trabalho, e ela está escrita no slide.\nA gente preferiu deixar visível\na inventar uma classe só pra fechar cinco nomes.",
    fala2: "16 testes passando.\n[PAUSA]\nO defeito daqui: o painel aceita número negativo.\n[PAUSA]\nCadastra um insumo com quantidade menos cinco, ele aceita.\nPreço de compra negativo, aceita também.\nE responde Ingrediente salvo.\n[PAUSA]\nNão tem validação no servlet,\ne o banco também não tem restrição nenhuma.\nIssue vinte e cinco.\n[PAUSA]\nE tem uma coisa que a gente decidiu não fazer.\n[PAUSA]\nNão escrevemos teste dizendo\nque preço de venda tem que ser maior que o de compra.\nParece óbvio, mas não existe nada no sistema que diga isso.\nA gente testa o que está escrito, não o que a gente acha.",
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
  texto(s, "Yuri Mascarenhas · Vinicius Rocha Pinheiro · Thiago Ferreira · Vinicius Fonseca de Freitas · Felipe Paixão", { x: 0.6, y: 4.3, w: 6.2, h: 0.6, fontSize: 12, color: "E8DFDB" });
  s.addNotes("Boa noite, pessoal.\nA gente é o grupo do Code Burguer's.\nÉ um sistema de lanchonete online, feito em Java, com Servlet e JDBC.\n[PAUSA]\nSó que não fomos nós que escrevemos esse sistema.\nEle veio pronto, de um projeto antigo.\nO nosso trabalho foi outro: testar ele.\nE achar o que está quebrado.\n[PAUSA]\nA gente dividiu em cinco áreas, uma pra cada um.\nCada pessoa tem dois slides:\no que testou, e o que encontrou.\nAdianto que encontramos bastante coisa.");
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
  bloco(s2, 3.2, 2.9, 2.7, "Situação", a.statusManual, 1.75);
  cartao(s2, 6.4, 1.3, 3.1, 3.7, COR.fundo);
  texto(s2, a.achadoTitulo.toUpperCase(), { x: 6.6, y: 1.5, w: 2.7, h: 0.5, fontSize: 9.5, bold: true, color: COR.mostarda, charSpacing: 1 });
  texto(s2, "Área de " + a.quem, { x: 6.6, y: 2.05, w: 2.7, h: 0.3, fontSize: 10, italic: true, color: "E8DFDB" });
  texto(s2, a.achado, { x: 6.6, y: 2.45, w: 2.7, h: 2.4, fontSize: 12.5, color: COR.branco });
  rodape(s2, "Sem testes de integração nesta entrega  ·  " + a.ref);
  s2.addNotes(a.fala2);
});

// 12. Encerramento
{
  const s = pres.addSlide();
  s.background = { color: COR.fundo };
  texto(s, "Encerramento", { x: 0.6, y: 0.45, w: 8.8, h: 0.6, fontSize: 32, bold: true, color: COR.branco, fontFace: "Cambria" });
  const destaques = [["115", "testes unitários\n0 falhas (mvn clean test)", COR.mostarda], ["5", "testes vermelhos na suíte\n-Pdefeitos, um por defeito", COR.tomate], ["6", "defeitos documentados\nissues #20 a #25 no GitHub", COR.branco]];
  destaques.forEach((d, i) => {
    texto(s, d[0], { x: 0.6 + i * 3.0, y: 1.25, w: 2.7, h: 0.8, fontSize: 48, bold: true, color: d[2], fontFace: "Cambria" });
    texto(s, d[1], { x: 0.6 + i * 3.0, y: 2.05, w: 2.7, h: 0.6, fontSize: 12, color: "E8DFDB" });
  });
  texto(s, "PENDÊNCIAS E LIMITES", { x: 0.6, y: 3.0, w: 8.8, h: 0.25, fontSize: 10, bold: true, color: COR.mostarda, charSpacing: 1 });
  texto(s, [
    { text: "CT-03 (Thiago Ferreira) e CT-05 (Felipe Paixão) ainda não executados; CT-04 executado por Vinicius Fonseca com falha.", options: { bullet: true, breakLine: true } },
    { text: "Área 5 sem classe não CRUD de complexidade razoável: o sistema não tem regra de estoque.", options: { bullet: true, breakLine: true } },
    { text: "Nenhum defeito foi corrigido: o objetivo foi encontrar, reproduzir e registrar.", options: { bullet: true, breakLine: true } },
    { text: "Cobertura mede o que foi exercitado, não ausência de defeitos.", options: { bullet: true } },
  ], { x: 0.6, y: 3.3, w: 8.8, h: 1.6, fontSize: 13, color: COR.branco, paraSpaceAfter: 4 });
  texto(s, "Maven 3.8.4 · OpenJDK 1.8.0_322 · JUnit 5.8.2 · Mockito 4.6.1 · JaCoCo 0.8.11 · execução de 21/09/2026 · docs/entrega-1/README.md", { x: 0.6, y: 5.15, w: 8.8, h: 0.25, fontSize: 9, color: "B9ABA5" });
  s.addNotes("Fechando.\n[PAUSA]\n115 testes rodando, todos passando.\n[PAUSA]\nMais cinco testes que ficam vermelhos de propósito.\nEles descrevem como deveria funcionar,\ne falham enquanto o defeito existir.\n[PAUSA]\nE seis defeitos abertos no GitHub, da issue vinte até a vinte e cinco.\n[PAUSA]\nO que ficou faltando, e a gente não vai esconder:\ndois testes manuais ainda não rodaram, o CT-03 e o CT-05.\nA área de estoque ficou sem classe de regra de negócio,\ncomo eu mostrei ali atrás.\nE a gente não corrigiu nenhum defeito,\nporque o objetivo dessa entrega era encontrar e registrar.\n[PAUSA]\nUma última coisa, porque é fácil confundir:\ncobertura alta não quer dizer que não tem defeito.\nQuer dizer só que aquele trecho foi executado.\n[PAUSA]\nTanto é que a gente tem cobertura alta e seis defeitos.\n[PAUSA]\nÉ isso. Obrigado, e a gente está aberto pra perguntas.");
}

pres.writeFile({ fileName: path.join(__dirname, "apresentacao-entrega-1.pptx") }).then((f) => console.log("gerado:", path.basename(f)));
