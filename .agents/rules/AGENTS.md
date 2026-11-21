# Regras do Projeto — Code Burguer's (Qualidade e Teste de Software)

## Stack e Restrições Técnicas

- Runtime obrigatório: Java JDK 8 (não usar sintaxe nem plugins que exijam Java 11+).
- Build e testes via Docker (`maven:3.8.4-openjdk-8`): JUnit 5, Mockito, JaCoCo e PITest.
- Layout legado NetBeans: código de produção fica em `src/java/` e testes em `src/test/java/`.
- Nunca alterar arquivos em `src/java/` sem autorização explícita (preservar baseline do SonarQube).
- Nunca executar `git commit` ou `git push` automaticamente; apenas editar arquivos e rodar comandos de validação.

## Registro no AI-LOG

- Ao concluir uma tarefa relevante (configuração, geração de testes ou refatoração), incluir no final da resposta no chat uma linha em Markdown pronta para a tabela de `docs/ai/vinicius-pinheiro/AI-LOG.md`.
- Preencher: Responsável (`Vinícius Pinheiro`), Atividade, Ferramenta (incluindo o modelo usado), Prompt/instrução e Resultado (com métricas reais de execução).
- Nas colunas **Decisão** e **Validação**, escrever sempre `**[PENDENTE: revisão manual]**`.
- NÃO editar `AI-LOG.md` automaticamente durante tarefas de código; só editar o arquivo quando solicitado explicitamente.
