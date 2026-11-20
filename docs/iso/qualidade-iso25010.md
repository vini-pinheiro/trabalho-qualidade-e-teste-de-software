# Avaliação de Características de Qualidade do Produto (ISO/IEC 25010:2023)

## 1. Escala de Avaliação

Cada característica foi classificada na seguinte escala de criticidade:

- **1:** Não afeta o sucesso do sistema.
- **2:** Desejável, mas pode ser negligenciada sem prejuízo significativo.
- **3:** Deve ser atendida em nível aceitável.
- **4:** Falhas nessa característica comprometem o uso do sistema.
- **5:** Falhas podem causar danos graves (financeiros, à saúde, legais, à reputação).

## 2. Avaliação das 9 Características da ISO/IEC 25010:2023

### 2.1. Adequação Funcional (_Functional Suitability_)

- **Nível de Importância:** **5 (Falhas causam danos graves)**
- **Elementos Concretos do Sistema:**
  - **Requisito:** Fechamento de pedidos e cálculo do valor total da compra no Servlet [`Controllers.comprar`](/src/java/Controllers/comprar.java).
  - **Dado tratado:** Quantidade de itens e valor unitário de venda (`valor_venda`).
  - **Defeitos reais identificados no projeto:**
    - **DEF-02 (Issue #21):** O método `processRequest` ignora a quantidade no cálculo do total acumulado (`valor_total += lanche.getValor_venda()`), cobrando apenas 1 unidade de cada item independentemente do número de lanches escolhidos.
    - **Defeito Manual CT-02:** Carrinho vazio grava pedido de R$ 0,00 no banco de dados e retorna mensagem de sucesso.
- **Justificativa:** A função primordial de uma lanchonete é vender itens e cobrar o valor correto. Um erro funcional no cálculo de checkout causa **prejuízo financeiro direto e imediato** ao caixa do estabelecimento ou cobrança indevida ao cliente, inviabilizando o modelo de negócio da aplicação.

### 2.2. Segurança da Informação (_Security_)

- **Nível de Importância:** **5 (Falhas causam danos graves)**
- **Elementos Concretos do Sistema:**
  - **Dados tratados:** Dados cadastrais do cliente ([`Model.Cliente`](/src/java/Model/Cliente.java)), incluindo `usuario`, `senha`, `telefone` e `endereco`.
  - **Mecanismos de autenticação:** Validação de cookies via [`Helpers.ValidadorCookie`](/src/java/Helpers/ValidadorCookie.java).
  - **Defeitos reais identificados no projeto:**
    - **DEF-03 (Issue #22):** O servlet `comprar` associa o pedido ao campo `"id"` extraído diretamente do payload JSON da requisição, e não ao ID do cliente autenticado no cookie de sessão, permitindo que um usuário logado grave pedidos cobrados em nome de qualquer outro cliente (personificação de usuário / falha de autorização).
    - **DEF-04 (Issue #23):** Consultas em `DaoCliente` realizam concatenação direta de strings em SQL (`"WHERE usuario = '" + usuario + "'"`), expondo o banco a ataques de **SQL Injection**.
- **Justificativa:** Falhas de segurança que expõem senhas, endereços residenciais ou permitem adulteração de pedidos violam diretamente a legislação de proteção de dados (**LGPD**), expondo os responsáveis a penalidades legais gravíssimas, perda de confiança e dano severo à reputação.

### 2.3. Confiabilidade (_Reliability_)

- **Nível de Importância:** **4 (Falhas comprometem o uso do sistema)**
- **Elementos Concretos do Sistema:**
  - **Restrição técnica:** Pool de conexões JDBC com o PostgreSQL 13 sob carga concorrente no Tomcat 8.5.
  - **Defeitos reais identificados no projeto:**
    - **Defeito Manual CT-02:** A tentativa de finalizar compra com bebidas no carrinho dispara erro HTTP 500 (`NullPointerException` em `comprar.java`), interrompendo abruptamente o checkout.
    - **Tratamento de exceções:** Blocos `catch` vazios (como em `comprar.java#L85-L86`) que silenciam falhas e mascaram o estado do sistema.
- **Justificativa:** O faturamento de estabelecimentos gastronômicos concentra-se em janelas críticas de pico (horários de almoço e jantar). Instabilidades no container, vazamentos de conexões JDBC ou falhas de runtime (HTTP 500) impedem a realização de pedidos e interrompem o fluxo da cozinha. O cliente com fome não aguarda a recuperação do sistema e migra para plataformas concorrentes.

### 2.4. Capacidade de Interação / Usabilidade (_Interaction Capability / Usability_)

- **Nível de Importância:** **4 (Falhas comprometem o uso do sistema)**
- **Elementos Concretos do Sistema:**
  - **Tipo de usuário:** `Cliente` final operando o sistema via navegador web ou dispositivo móvel.
  - **Mensagens de feedback da API:** Respostas textuais cruas geradas pelo Servlet (`"Pedido Salvo com Sucesso!"` vs. `"erro"`).
  - **Defeito real do projeto (DEF-05 / Issue #24):** No formulário de cadastro, a expressão lógica `!field.name == 'complemento'` em `cadastro.js` é avaliada sempre como falsa, desabilitando a validação de campos obrigatórios no frontend.
- **Justificativa:** A usabilidade dita a taxa de conversão do comércio eletrônico. Mensagens de erro opacas (como o texto `"erro"` retornado sem código de status nem explicação) e falhas em formulários frustram o usuário, que abandona o carrinho de compras por insegurança ou envia pedidos duplicados por não ter clareza sobre o processamento da compra.

### 2.5. Eficiência de Desempenho (_Performance Efficiency_)

- **Nível de Importância:** **3 (Deve ser atendida em nível aceitável)**
- **Elementos Concretos do Sistema:**
  - **Requisito:** Tempo de resposta do endpoint `/comprar` e das buscas em catálogo.
  - **Contexto de uso:** Lanchonete de abrangência local (dezenas de pedidos simultâneos, sem escala distribuída de milhões de acessos).
- **Justificativa:** Para o perfil de uso de uma lanchonete local, latências de 1 a 2 segundos para validar o cookie, somar itens e registrar o pedido no banco relacional são plenamente toleráveis pelo usuário. Desempenho sub-segundo ou processamento assíncrono complexo não são indispensáveis para a viabilidade do produto.

### 2.6. Manutenibilidade (_Maintainability_)

- **Nível de Importância:** **3 (Deve ser atendida em nível aceitável)**
- **Elementos Concretos do Sistema:**
  - **Tipo de usuário:** `Desenvolvedor / Mantenedor`.
  - **Restrição técnica:** Código legado em Java 8 com Servlets procedurais, alta complexidade ciclomática ($CC = 10$) e acoplamento rígido original via `new Dao...()`.
- **Justificativa:** Embora a arquitetura interna seja transparente para o cliente da lanchonete, um nível aceitável de manutenibilidade e testabilidade é mandatório para permitir que os mantenedores corrijam os defeitos descobertos (como os 7 defeitos catalogados na Entrega 1) sem introduzir efeitos colaterais nos demais módulos do sistema.

### 2.7. Compatibilidade (_Compatibility_)

- **Nível de Importância:** **3 (Deve ser atendida em nível aceitável)**
- **Elementos Concretos do Sistema:**
  - **Restrição técnica:** Interoperabilidade do frontend web com os Servlets Java via requisições assíncronas JSON.
  - **Defeito manual CT-03:** Conflito de codificação de caracteres (`ISO-8859-1` vs. `UTF-8`) corrompendo acentuações no banco de dados (ex: "José Conceição" gravado como "Jos? Concei??o").
- **Justificativa:** A aplicação precisa interoperar adequadamente com os diferentes navegadores modernos de clientes (Chrome, Firefox, Safari mobile) sem que discrepâncias de charset ou interpretação de cabeçalhos HTTP quebrem a desserialização do JSON dinâmico recebido via `request.getInputStream()`.

### 2.8. Flexibilidade / Portabilidade (_Flexibility / Portability_)

- **Nível de Importância:** **2 (Desejável, mas de baixo impacto)**
- **Elementos Concretos do Sistema:**
  - **Restrição técnica:** Ambiente de execução completamente padronizado e isolado via **Docker** (`Dockerfile` multi-stage com Maven 3.8 / Tomcat 8.5 e container PostgreSQL 13).
- **Justificativa:** Como a aplicação é empacotada em containers Docker, ela já possui garantia de execução idêntica em qualquer host compatível com contêineres. Não há requisito operacional ou de negócio para suportar migração para outros bancos (ex: Oracle ou SQL Server) ou múltiplos servidores de aplicação distintos do Tomcat.

### 2.9. Segurança Operacional / Proteção contra Danos (_Safety_)

> (Novidade da ISO/IEC 25010:2023 — prevenção de danos físicos à saúde, pessoas ou ambiente)

- **Nível de Importância:** **2 (Desejável, mas pode ser negligenciada no software)**
- **Elementos Concretos do Sistema:**
  - **Dados tratados:** Campos descritivos em [`Model.Lanche`](/src/java/Model/Lanche.java) e [`Model.Ingrediente`](/src/java/Model/Ingrediente.java) (`nome`, `descricao`, `valor_venda`).
- **Justificativa:** O sistema é estritamente transacional e administrativo; ele não controla maquinário físico, fornos, dosadores automatizados de ingredientes ou sistemas ciber-físicos. O preparo físico dos lanches é 100% manual pela equipe da cozinha. Embora a rotulagem de alérgenos graves (glúten, lactose, frutos do mar) seja recomendada na descrição do item, a responsabilidade operacional e de vigilância sanitária recai sobre o preparo físico, não gerando riscos diretos de falha catastrófica originados pelo software.

## 3. Matriz Síntese de Priorização

|   #   | Característica (ISO/IEC 25010:2023)   | Nível | Elemento Concreto / Evidência no Sistema                                               |
| :---: | :------------------------------------ | :---: | :------------------------------------------------------------------------------------- |
| **1** | Adequação Funcional                   | **5** | Requisito de cálculo do total em `comprar` e defeitos DEF-02 e CT-02 (carrinho vazio). |
| **2** | Segurança da Informação               | **5** | Credenciais em `Cliente`, personificação no pedido (DEF-03) e SQL Injection (DEF-04).  |
| **3** | Confiabilidade                        | **4** | Pool de conexões JDBC no pico e erro HTTP 500 no checkout de bebidas (CT-02).          |
| **4** | Capacidade de Interação / Usabilidade | **4** | Feedback ao usuário `Cliente` e falha de validação no frontend (DEF-05).               |
| **5** | Eficiência de Desempenho              | **3** | Latência tolerável (1-2s) para a escala de demanda de uma lanchonete local.            |
| **6** | Manutenibilidade                      | **3** | Código acoplado com $CC = 10$, exigindo refatoração para injeção de dependência.       |
| **7** | Compatibilidade                       | **3** | Interoperabilidade JSON e corrupção de acentuação de nomes de clientes (CT-03).        |
| **8** | Flexibilidade / Portabilidade         | **2** | Ambiente já imutável e padronizado em contêineres Docker (Tomcat + Postgres).          |
| **9** | Segurança Operacional (_Safety_)      | **2** | Software não aciona atuadores físicos; preparo e higiene são manuais da cozinha.       |
