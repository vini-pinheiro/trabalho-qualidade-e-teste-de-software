# Publicação do Code Burguer's na VPS

Endereço: **https://codeburguer.maia-ai.com.br**, aberto ao público (sem login) desde 25/11/2026.

## Como fica

```
navegador ──> Cloudflare (DNS proxied + Access com política Bypass para todos)
                 │  passa qualquer pessoa, sem login
                 ▼
          nginx do host, vhost codeburguer.conf
                 │  aceita só IPs do Cloudflare e recusa /tabela
                 ▼
          127.0.0.1:8895 ──> codeburguer-app (Tomcat 8.5, WAR deste repositório)
                                  │  rede interna, sem saída para a internet
                                  ▼
                            codeburguer-db (PostgreSQL 13, volume próprio)
```

- **Quem entra:** qualquer pessoa. O app do Access `Code Burguer's (UFF)` continua existindo, mas com
  uma política **Bypass** com Include `Everyone`. Ele precisa ficar: sem ele, vale o app
  `*.maia-ai.com.br` da MAIA, que pede login. Para voltar a exigir login, trocar a política dele pela
  `UFF e equipe do Code Burguer's` (Allow, `@id.uff.br` + e-mails da equipe, só Google) e repor no
  vhost a checagem do cabeçalho `Cf-Access-Jwt-Assertion`.
- **O que fica exposto:** as falhas conhecidas da aplicação, agora para qualquer um: SQL injection no
  login (#23), pedido gravado em nome de outro cliente (#22) e `alterarStatusLanchonete` sem login
  (qualquer um abre ou fecha a lanchonete). O `/tabela`, que lista os clientes com o hash da senha sem
  login, é recusado no nginx.
- **Isolamento:** projeto Compose próprio (`codeburguer`), sem nada compartilhado com a stack da MAIA.
  O banco não tem porta publicada nem rota de saída, então essas falhas ficam restritas a este banco
  de demonstração.
- **Dados de demonstração:** o `banco.sql` cria a lanchonete `FECHADO` e sem cardápio. Na VPS foram
  carregados os dados de `docs/util/insertions.sql` e a lanchonete foi aberta (veja "Abrir a
  lanchonete" abaixo).
- **Senha do admin do painel:** o `banco.sql` cria `admin/admin`. Na instalação ela é trocada por
  uma senha aleatória, guardada em `~/codeburguer-admin.txt` (0600) no usuário de deploy da VPS.

## Instalação (feita uma vez)

Na VPS, como o usuário de deploy:

```bash
git clone https://github.com/vini-pinheiro/trabalho-qualidade-e-teste-de-software.git ~/apps/codeburguer
cd ~/apps/codeburguer
git switch -c vps origin/master
docker compose -f deploy/compose.vps.yml up -d --build
bash deploy/trocar-senha-admin.sh

# TLS de origem autoassinado (a zona usa o modo Full do Cloudflare)
sudo install -d -m 0700 /etc/nginx/codeburguer-tls
sudo openssl req -x509 -newkey rsa:3072 -nodes -days 3650 \
  -subj /CN=codeburguer.maia-ai.com.br -addext subjectAltName=DNS:codeburguer.maia-ai.com.br \
  -keyout /etc/nginx/codeburguer-tls/privkey.pem -out /etc/nginx/codeburguer-tls/fullchain.pem

sudo bash deploy/gerar-cloudflare-only.sh
sudo install -m 0644 deploy/nginx/codeburguer.conf /etc/nginx/sites-available/codeburguer.conf
sudo ln -s /etc/nginx/sites-available/codeburguer.conf /etc/nginx/sites-enabled/codeburguer.conf
sudo nginx -t && sudo systemctl reload nginx
```

No Cloudflare (zona `maia-ai.com.br`):

1. **DNS:** registro `A` `codeburguer` → o IP da VPS, com proxy ligado (nuvem laranja).
2. **Zero Trust → Access → Applications → Add → Self-hosted:**
   - nome `Code Burguer's (UFF)`, domínio `codeburguer.maia-ai.com.br`, sessão de 24h;
   - método de login: só **Google**, com "Aplicar autenticação instantânea" ligado;
   - política `Allow` com duas regras **Include**: `Emails ending in` `@id.uff.br` e `Emails` com a lista
     de exceções (era assim até 25/11/2026; hoje o app usa uma política `Bypass` com Include
     `Everyone`, e o site é público).
   Esse app é mais específico que o `*.maia-ai.com.br` da MAIA, então a política dele é a que vale.
3. **Login com Google** (já feito):
   - Google Cloud, projeto **MAIA** → Google Auth Platform: app `MAIA`, público
     **Externo**, **em produção**, só os escopos `openid email profile` (não exige verificação);
     domínios autorizados `maia-ai.com.br` e `cloudflareaccess.com`; página inicial
     `https://maia-ai.com.br` e privacidade `https://maia-ai.com.br/privacy`;
   - OAuth client `Cloudflare Access - Code Burguer's` (*Web application*), origem
     `https://<time>.cloudflareaccess.com` e redirect
     `https://<time>.cloudflareaccess.com/cdn-cgi/access/callback`;
   - Zero Trust → Integrações → Provedores de identidade → **Google**, com o Client ID e o Client
     secret desse client. O segredo não fica em nenhum arquivo do repositório nem da VPS.
   - Para trocar o segredo: gerar um novo no client do Google Cloud e atualizar o provedor Google no
     Zero Trust.

## Atualizar para a versão mais nova do `master`

```bash
cd ~/apps/codeburguer
git fetch origin
git switch -C vps origin/master
docker compose -f deploy/compose.vps.yml up -d --build app
```

O checkout da VPS fica na branch local `vps`, que só acompanha o `origin/master` (nada é commitado
nem enviado de lá).

O banco e os dados ficam no volume `codeburguer_db-data`; `--build app` recria só a aplicação.

O vhost não é atualizado pelo `git switch`. Depois de mudar `deploy/nginx/codeburguer.conf`:

```bash
sudo install -m 0644 deploy/nginx/codeburguer.conf /etc/nginx/sites-available/codeburguer.conf
sudo nginx -t && sudo systemctl reload nginx
```

## Abrir a lanchonete e carregar o cardápio

O `banco.sql` deixa a lanchonete `FECHADO` e sem lanches nem bebidas, então o cardápio avisa "A
lanchonete está fechada no momento". Quem abre é o admin, no painel. Pela VPS, sem o painel:

```bash
# dados de demonstração (só num banco vazio: os ids 1, 2, 3 estão fixos no arquivo)
docker exec -i codeburguer-db psql -U postgres -d lanchonete -1 -v ON_ERROR_STOP=1 -q < docs/util/insertions.sql

# abrir a lanchonete, o mesmo que o botão do painel faz
curl -s -X POST --data '{"status":"ABERTO"}' http://127.0.0.1:8895/alterarStatusLanchonete
```

## Conferir

```bash
docker compose -f deploy/compose.vps.yml ps
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8895/view/home/home.html   # 200 local
curl -s -o /dev/null -w '%{http_code}\n' https://codeburguer.maia-ai.com.br/view/menu/menu.html  # 200, sem login
curl -s -o /dev/null -w '%{http_code}\n' https://codeburguer.maia-ai.com.br/tabela  # 403
curl -s https://codeburguer.maia-ai.com.br/getStatusLanchonete  # {"status":"ABERTO"}
```

## Desfazer

```bash
sudo rm /etc/nginx/sites-enabled/codeburguer.conf && sudo nginx -t && sudo systemctl reload nginx
cd ~/apps/codeburguer && docker compose -f deploy/compose.vps.yml down        # -v apaga também o banco
```

Depois, apagar o app do Access e o registro DNS no Cloudflare.
