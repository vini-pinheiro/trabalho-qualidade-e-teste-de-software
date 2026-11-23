# Publicação do Code Burguer's na VPS

Endereço: **https://codeburguer.maia-ai.com.br**, protegido por login no Cloudflare Access.

## Como fica

```
navegador ──> Cloudflare (DNS proxied + Access: login obrigatório)
                 │  só passa quem fez login e está autorizado
                 ▼
          nginx do host (srv2063117), vhost codeburguer.conf
                 │  aceita só IPs do Cloudflare e exige o cabeçalho do Access
                 ▼
          127.0.0.1:8895 ──> codeburguer-app (Tomcat 8.5, WAR deste repositório)
                                  │  rede interna, sem saída para a internet
                                  ▼
                            codeburguer-db (PostgreSQL 13, volume próprio)
```

- **Quem entra:** qualquer e-mail `@id.uff.br` e os e-mails pessoais listados na política do Access
  (app `Code Burguer's (UFF) - privado`, política `UFF e equipe do Code Burguer's`).
- **Como entra:** código de 6 dígitos enviado por e-mail (One-time PIN). O "Entrar com Google" entra
  quando o provedor Google for cadastrado no Zero Trust (passo 3 abaixo).
- **Isolamento:** projeto Compose próprio (`codeburguer`), sem nada compartilhado com a stack da MAIA.
  O banco não tem porta publicada nem rota de saída, então as falhas conhecidas da aplicação
  (#22, #23) ficam restritas a este banco de demonstração.
- **Senha do admin do painel:** o `banco.sql` cria `admin/admin`. Na instalação ela é trocada por
  uma senha aleatória, guardada em `~/codeburguer-admin.txt` (0600) no usuário `niutech` da VPS.

## Instalação (feita uma vez)

Na VPS, como `niutech`:

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

No Cloudflare (conta da NIUTech, zona `maia-ai.com.br`):

1. **DNS:** registro `A` `codeburguer` → `187.127.31.206`, com proxy ligado (nuvem laranja).
2. **Zero Trust → Access → Applications → Add → Self-hosted:**
   - nome `Code Burguer's (UFF)`, domínio `codeburguer.maia-ai.com.br`, sessão de 24h;
   - métodos de login: **Google** e **One-time PIN**;
   - política `Allow` com duas regras **Include**: `Emails ending in` `@id.uff.br` e `Emails` com a lista
     de exceções.
   Esse app é mais específico que o `*.maia-ai.com.br` da MAIA, então a política dele é a que vale.
3. **Login com Google (feito pelo dono da conta Google Cloud):**
   - no Google Cloud Console, criar um *OAuth client ID* do tipo *Web application* com a origem
     `https://wild-truth-50df.cloudflareaccess.com` e o redirect
     `https://wild-truth-50df.cloudflareaccess.com/cdn-cgi/access/callback`;
   - em Zero Trust → Settings → Authentication → Add new → Google, colar o Client ID e o Client secret;
   - marcar o Google como método de login no app acima.

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

## Conferir

```bash
docker compose -f deploy/compose.vps.yml ps
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8895/view/home/home.html   # 200 local
curl -s -o /dev/null -w '%{http_code} %{redirect_url}\n' https://codeburguer.maia-ai.com.br  # 302 para o login do Access
```

## Desfazer

```bash
sudo rm /etc/nginx/sites-enabled/codeburguer.conf && sudo nginx -t && sudo systemctl reload nginx
cd ~/apps/codeburguer && docker compose -f deploy/compose.vps.yml down        # -v apaga também o banco
```

Depois, apagar o app do Access e o registro DNS no Cloudflare.
