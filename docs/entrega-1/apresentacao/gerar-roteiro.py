"""Gera roteiro-apresentacao.pdf a partir das notas do apresentador do PPTX.

Formato teleprompter: uma ideia por linha, fonte grande, pausas marcadas.
Uso: pip install python-pptx reportlab && python gerar-roteiro.py
"""
import os
import re

from pptx import Presentation
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import cm
from reportlab.platypus import (BaseDocTemplate, Frame, KeepTogether, NextPageTemplate, PageBreak, PageTemplate,
                                Paragraph, Spacer, Table, TableStyle)

PASTA = os.path.dirname(os.path.abspath(__file__))
MARROM = colors.HexColor("#2B1B17")
TOMATE = colors.HexColor("#C8352B")
CINZA = colors.HexColor("#6B5B55")
CREME = colors.HexColor("#F6F1EE")

# (slides, quem fala, parte, tempo, o que abrir na hora de demonstrar)
PARTES = [
    ([1], "Yuri Mascarenhas", "Abertura", "1 min", None),
    ([2, 3, 4], "Yuri Mascarenhas", "Autenticação, sessões e controle de acesso", "4 min",
     "ValidadorCookieLimitesTest · mvn test -Dtest=ValidadorCookie*"),
    ([5, 6, 7], "Vinicius Rocha Pinheiro", "Carrinho, pedidos e checkout", "4 min",
     "mvn test -Pdefeitos -Dtest=ComprarRegrasTest · mostrar 'expected: <35.0> but was: <20.0>'"),
    ([8, 9, 10], "Thiago Ferreira", "Gestão de clientes e endereços", "4 min",
     "DaoClienteTest (deveRecusarClienteInativo) · cadastro.js, a condição !field.name == 'complemento'"),
    ([11, 12, 13], "Vinicius Fonseca de Freitas", "Cardápio e montagem de lanches", "4 min",
     "salvarLancheCliente.java, os dois while(keys.hasNext()) · mvn test -Pdefeitos -Dtest=SalvarLancheClienteTest"),
    ([14, 15, 16], "Felipe Paixão", "Gestão de estoque e insumos", "4 min",
     "IngredienteControllersTest (deveBloquearQuemNaoEhFuncionario e caracterizacaoValoresNegativosSaoAceitos)"),
    ([17], "Felipe Paixão", "Encerramento", "1 min", "mvn clean test · mostrar 'Tests run: 115, Failures: 0'"),
    ([18, 19, 20], "Qualquer um do grupo", "Anexos — só se perguntarem", "reserva",
     "Estrutura no repositório, issues no GitHub e a saída das duas suítes"),
]

PERGUNTAS = [
    ("Por que tem teste vermelho de propósito?",
     "Porque ele descreve como deveria funcionar. Enquanto o defeito existir, ele falha. A gente deixou num perfil separado "
     "pra não quebrar o build normal, e não usou @Disabled, que só esconderia o problema."),
    ("Vocês corrigiram os defeitos?",
     "Não. O objetivo dessa entrega era encontrar e registrar. Cada defeito tem um teste que reproduz ele e uma issue no GitHub."),
    ("Por que vocês mexeram no código de produção?",
     "Só adicionamos construtores que recebem a conexão ou os DAOs, pra conseguir usar mocks. O construtor padrão continua igual "
     "e o comportamento no Tomcat não mudou."),
    ("A classe de estoque não é CRUD mesmo?",
     "É CRUD, e a gente assume isso. O sistema não tem regra de estoque: quando alguém compra, nada baixa. Usamos os servlets "
     "do painel como melhor alternativa e deixamos a lacuna registrada."),
    ("Cobertura alta quer dizer que está sem bug?",
     "Não. Quer dizer só que aquelas linhas foram executadas pelos testes. Tanto é que a nossa cobertura é alta e mesmo assim "
     "achamos seis defeitos."),
]


def estilos():
    return {
        "titulo": ParagraphStyle("titulo", fontSize=24, leading=28, fontName="Helvetica-Bold", textColor=MARROM),
        "sub": ParagraphStyle("sub", fontSize=11, leading=15, fontName="Helvetica", textColor=CINZA),
        "quem": ParagraphStyle("quem", fontSize=22, leading=26, fontName="Helvetica-Bold", textColor=MARROM),
        "slide": ParagraphStyle("slide", fontSize=11, leading=14, fontName="Helvetica-Bold", textColor=TOMATE,
                                spaceBefore=14, spaceAfter=6),
        "pausa": ParagraphStyle("pausa", fontSize=10, leading=13, fontName="Helvetica-Bold", textColor=TOMATE,
                                spaceBefore=9, spaceAfter=9),
        "fala": ParagraphStyle("fala", fontSize=18, leading=26, fontName="Helvetica", textColor=MARROM,
                               alignment=TA_LEFT, spaceAfter=2),
        "nota": ParagraphStyle("nota", fontSize=10.5, leading=14, fontName="Helvetica", textColor=MARROM),
        "pergunta": ParagraphStyle("pergunta", fontSize=12, leading=15, fontName="Helvetica-Bold", textColor=TOMATE,
                                   spaceBefore=10, spaceAfter=3),
    }


def caixa(texto, e, largura):
    """Bloco em creme, para a dica de demonstração."""
    tabela = Table([[Paragraph(texto, e["nota"])]], colWidths=[largura])
    tabela.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), CREME),
        ("LEFTPADDING", (0, 0), (-1, -1), 10), ("RIGHTPADDING", (0, 0), (-1, -1), 10),
        ("TOPPADDING", (0, 0), (-1, -1), 7), ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
    ]))
    return tabela


def falar(nota, e):
    """Converte a nota do slide em parágrafos: fala grande, marcações em vermelho."""
    partes = []
    for linha in nota.split("\n"):
        linha = linha.strip()
        if not linha:
            continue
        marcacao = re.match(r"^\[(.+)\]$", linha)
        if marcacao:
            partes.append(Paragraph("— " + marcacao.group(1).upper() + " —", e["pausa"]))
        else:
            partes.append(Paragraph(linha, e["fala"]))
    return partes


def marcador(texto):
    """Cabeçalho repetido em toda página do bloco: quem fala e em que slide."""
    def desenhar(canvas, doc):
        canvas.saveState()
        canvas.setFont("Helvetica", 9)
        canvas.setFillColor(CINZA)
        largura_pagina, altura_pagina = landscape(A4)
        if texto:
            canvas.drawString(3 * cm, altura_pagina - 1.1 * cm, texto)
        canvas.drawRightString(largura_pagina - 3 * cm, 0.9 * cm, str(canvas.getPageNumber()))
        canvas.restoreState()
    return desenhar


def main():
    deck = Presentation(os.path.join(PASTA, "apresentacao-entrega-1.pptx"))
    notas = {i: s.notes_slide.notes_text_frame.text.strip() for i, s in enumerate(deck.slides, 1)}
    e = estilos()
    doc = BaseDocTemplate(os.path.join(PASTA, "roteiro-apresentacao.pdf"), pagesize=landscape(A4),
                          title="Roteiro - Entrega 1", leftMargin=3 * cm, rightMargin=3 * cm,
                          topMargin=1.8 * cm, bottomMargin=1.6 * cm)
    quadro = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="corpo",
                   leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    modelos = [PageTemplate(id="capa", frames=[quadro], onPage=marcador(""))]
    for i, (slides, quem, _parte, _tempo, _dem) in enumerate(PARTES):
        rotulo = "Slide %d" % slides[0] if len(slides) == 1 else "Slides %d a %d" % (slides[0], slides[-1])
        modelos.append(PageTemplate(id="parte%d" % i, frames=[quadro], onPage=marcador(quem + "  ·  " + rotulo)))
    doc.addPageTemplates(modelos)
    largura = doc.width

    historia = [
        Paragraph("Roteiro da apresentação", e["titulo"]),
        Paragraph("Code Burguer's · Qualidade e Teste de Software · Entrega 1 · 20 slides, cerca de 22 minutos", e["sub"]),
        Spacer(1, 14),
    ]

    linhas = [["Slides", "Quem fala", "Parte", "Tempo"]]
    for slides, quem, parte, tempo, _ in PARTES:
        rotulo = str(slides[0]) if len(slides) == 1 else "%d a %d" % (slides[0], slides[-1])
        linhas.append([rotulo, quem, parte, tempo])
    tabela = Table(linhas, colWidths=[2.2 * cm, 6.0 * cm, 11.5 * cm, 2.2 * cm])
    tabela.setStyle(TableStyle([
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"), ("FONTSIZE", (0, 0), (-1, -1), 11),
        ("BACKGROUND", (0, 0), (-1, 0), MARROM), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, CREME]), ("TEXTCOLOR", (0, 1), (-1, -1), MARROM),
        ("TOPPADDING", (0, 0), (-1, -1), 7), ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
    ]))
    historia += [tabela, Spacer(1, 16), caixa(
        "<b>Como usar:</b> cada pessoa tem a sua página. O texto está quebrado em uma ideia por linha, para ler olhando "
        "pra turma e voltando o olho rápido. Onde está escrito PAUSA, respira e troca o slide — é o momento em que a turma "
        "processa o que você acabou de falar. Não precisa decorar nem ler palavra por palavra: o que não pode é você não "
        "saber explicar o teste que está citando.", e, largura)]

    for i, (slides, quem, parte, tempo, demonstracao) in enumerate(PARTES):
        rotulo = "Slide %d" % slides[0] if len(slides) == 1 else "Slides %d a %d" % (slides[0], slides[-1])
        historia += [NextPageTemplate("parte%d" % i), PageBreak(),
                     Paragraph(quem, e["quem"]), Paragraph("%s · %s · %s" % (parte, rotulo, tempo), e["sub"])]
        for n in slides:
            historia.append(Paragraph("SLIDE %d" % n, e["slide"]))
            historia += falar(notas[n], e)
        if demonstracao:
            historia += [Spacer(1, 12), caixa("<b>Para abrir na hora:</b> " + demonstracao, e, largura)]

    historia += [NextPageTemplate("capa"), PageBreak()]
    perguntas = [Paragraph("Perguntas prováveis", e["titulo"]),
                 Paragraph("Qualquer um do grupo pode responder", e["sub"]), Spacer(1, 6)]
    for pergunta, resposta in PERGUNTAS:
        perguntas += [Paragraph(pergunta, e["pergunta"]), Paragraph(resposta, e["nota"])]
    historia.append(KeepTogether(perguntas))

    doc.build(historia)
    print("gerado: roteiro-apresentacao.pdf")


if __name__ == "__main__":
    main()
