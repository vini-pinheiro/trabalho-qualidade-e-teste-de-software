# Trabalho da matéria de Qualidade e Teste de Software baseado no projeto APS-04-Lanchonete-Online-em-Java

## Documentos

- [Plano de Testes](https://docs.google.com/document/d/1MT2I238OlATRWofvbea8ut1F0oSDA92Siztl87TmjpI/edit?usp=drive_link)
- Casos de Testes:
  - [Caso de Teste 1 - Yuri](docs/entrega-1/casos-de-testes/CT-01-Autenticacao-Yuri-Mascarenhas.docx)
  - [Caso de Teste 2 - Vinicius Pinheiro](docs/entrega-1/casos-de-testes/CT-02-Carrinho-e-Compra-Vinicius-Rocha-Pinheiro.docx)
  - [Caso de Teste 3 - Thiago](docs/entrega-1/casos-de-testes/CT-03-Cadastro-de-Cliente-Thiago-Ferreira.docx)
  - [Caso de Teste 4 - Vinicius Fonseca](docs/entrega-1/casos-de-testes/CT-04-Montagem-de-Lanche-Vinicius-Fonseca.docx)
  - [Caso de Teste 5 - Felipe](docs/entrega-1/casos-de-testes/CT-05-Insumos-e-Estoque-Felipe-Paixao.docx)

## Sobre

Com o objetivo de desenvolver a capacidade dos alunos e obter nota na disciplina APS (Atividades Práticas Supervisionadas),
foi proposto um projeto de desenvolvimento de um sistema para uma lanchonete online, onde o administrador consiga controlar
os pedidos da lanchonete e emitir relatórios. A lanchonete devera permitir o cadastro dos usuários, para que eles possam realizar seus pedidos,
e o cadastro de produtos, que ficariam por parte do administrador. Após o cadastro, cliente poderá utilizar os ingredientes cadastrados para
criar seu lanche personalizado. O sistema deverá fazer o controle dos pedidos de forma que agrade os clientes, e controlar tambem o estoque de produtos.

## Tecnologias Utilizadas

O Sistema funciona com base em um Frontend Utilizando HTML 5, CSS3 e JavaScript, e um Backend baseado em Java Web utilizando-se do Servidor Glassfish 4
e muito baseado no uso de Servlets para a Comunicação atraves de requisições. Além disso o Sistema utiliza das Bibliotecas gson-2.8.6 e json-20200518
Para a manipulação de Arquivos JSON dentro do Código Java, e de um Banco de Dados PostgreSQL, do qual o Código base também se encontra no repositório.

## Alguns Screenshots

![alt text](https://i.ibb.co/BPn99jW/248f5162-df3a-4754-8ade-82b9784f94d8.jpg)
![alt text](https://i.ibb.co/GM3r7Dd/daf6e1f9-676e-4a27-9669-80036dc52cce.jpg)
![alt text](https://i.ibb.co/kXdFFq5/e378bda9-bcc8-4483-bb2f-f2143a79817e.jpg)
![alt text](https://i.ibb.co/z7kqx4x/a5a0e3f3-3605-4d3f-b2ba-f54c2ef76f18.jpg)
![alt text](https://i.ibb.co/C6kMZLW/c1bad7f9-c79a-4516-9d08-bc2548ee9880.jpg)
![alt text](https://i.ibb.co/2321674/8a74fb26-1db0-49df-b2d7-2479d0567a4e.jpg)
![alt text](https://i.ibb.co/2YSbvGZ/8d3386e3-d13b-4a42-b389-151fbadb1d77.jpg)

## Como rodar o projeto

### Pré-requisitos

- [Docker](https://www.docker.com/get-started) e [Docker Compose](https://docs.docker.com/compose/install/)
- [Java 8+](https://adoptopenjdk.net/) e [Maven](https://maven.apache.org/)

### Passos para executar

1. **Clone o repositório:**

   ```bash
   git clone <url-do-repositorio>
   cd APS-04-Lanchonete-Online-em-Java
   ```

2. **Suba os containers com Docker Compose:**

   ```bash
   docker-compose up --build -d
   ```

   Isso irá criar e iniciar os containers do banco de dados PostgreSQL e do servidor Tomcat com a aplicação.

3. **Acesse a aplicação:**
   - API: [http://localhost:8080](http://localhost:8080)

4. **Para parar e remover tudo:**

   ```bash
   docker-compose down -v
   ```

### Observações

- O banco de dados será inicializado automaticamente com as tabelas necessárias e o usuário de admin para login.
- Caso precise alterar configurações, edite os arquivos `docker-compose.yml` ou `banco.sql`.
- Se houver problemas de cache no navegador, utilize Ctrl+F5 ou limpe o cache manualmente.
